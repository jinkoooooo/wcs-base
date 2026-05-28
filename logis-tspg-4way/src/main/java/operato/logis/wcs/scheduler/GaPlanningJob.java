package operato.logis.wcs.scheduler;

import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.service.impl.scheduling.GaPlanCache;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.LiftDto;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.Request;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.Response;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.ShuttleDto;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.TaskDto;
import operato.logis.wcs.service.impl.scheduling.GaScheduleOptimizer;
import operato.logis.wcs.service.impl.scheduling.LocationResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GA(Genetic Algorithm) 기반 release 우선순위 산출 잡.
 *
 * 활성 eq_group_id 별로 OUTBOUND 후보 + 독립 MOVE 후보를 모아 GaScheduleOptimizer 에 위임,
 * 산출된 점수/breakdown 을 GaPlanCache 에 적재한다.
 * 적재된 점수는 ReleaseScheduler 의 번들/MOVE 정렬에 사용된다.
 */
@Component
@RequiredArgsConstructor
public class GaPlanningJob extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(GaPlanningJob.class);

    private static final Integer ECS_READY = EcsIfStatus.READY.code();
    private static final List<Integer> SO_PENDING = List.of(
            ShuttleOrderStatus.CREATED.code(),
            ShuttleOrderStatus.WAITING.code());
    private static final String OT_OUTBOUND = OrderType.OUTBOUND.code();
    private static final String OT_MOVE     = OrderType.MOVE.code();

    private static final long CACHE_TTL_MS              = 60_000L;
    private static final int  MAX_CANDIDATES_PER_GROUP  = 100;
    private static final long SEED_BUCKET_MS            = 30_000L;

    private final GaScheduleOptimizer optimizer;
    private final GaPlanCache cache;
    private final LocationResolver locationResolver;

    /**
     * 1 사이클 — 활성 그룹 조회 후 그룹별 planForGroup 실행.
     */
    public void runPlanningCycle() {
        long start = System.currentTimeMillis();
        List<String> groups = fetchActiveGroups();
        if (ValueUtil.isEmpty(groups)) return;

        int planned = 0, skipped = 0;
        for (String eqGroupId : groups) {
            try {
                if (planForGroup(eqGroupId)) planned++;
                else skipped++;
            } catch (Exception e) {
                logger.error("[ Scheduler ][ Planning ] group failed - eqGroupId={}", eqGroupId, e);
            }
        }
        logger.debug("[ Scheduler ][ Planning ] cycle done - groups={}, planned={}, skipped={}, tookMs={}",
                groups.size(), planned, skipped, System.currentTimeMillis() - start);
    }

    /**
     * 그룹 단위 산출 — 후보 없으면 캐시 무효화 후 skip.
     */
    private boolean planForGroup(String eqGroupId) {
        List<TaskDto> candidates = gatherCandidates(eqGroupId);
        if (ValueUtil.isEmpty(candidates)) {
            cache.invalidate(eqGroupId);
            return false;
        }

        // 셔틀/리프트 실시간 상태 수집
        List<ShuttleDto> shuttles = fetchShuttles(eqGroupId);
        List<LiftDto>    lifts    = fetchLifts(eqGroupId);

        // GA 입력 + 결정성 시드 빌드
        Request req = Request.builder()
                .eqGroupId(eqGroupId)
                .candidates(candidates)
                .shuttles(shuttles)
                .lifts(lifts)
                .seed(deterministicSeed(eqGroupId))
                .build();

        // 최적화 → 캐시 적재
        Response resp = optimizer.optimize(req);
        cache.put(eqGroupId, resp.getScoreByOrderKey(), resp.getBreakdownByOrderKey(), CACHE_TTL_MS);

        logger.info("[ Scheduler ][ Planning ] planned - eqGroupId={}, candidates={}, shuttles={}, makespanSec={}, gen={}",
                eqGroupId, candidates.size(), shuttles.size(),
                String.format("%.1f", resp.getTotalEstimatedTime()),
                resp.getConvergedGeneration());
        return true;
    }

    /**
     * release 대기 중 활성 eq_group_id 목록.
     */
    @SuppressWarnings("rawtypes")
    private List<String> fetchActiveGroups() {
        String sql = """
            SELECT DISTINCT eq_group_id
              FROM tb_wcs_shuttle_order
             WHERE ecs_if_status = :readyStatus
               AND order_status  IN (:pendingStatuses)
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "readyStatus,pendingStatuses", ECS_READY, SO_PENDING);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 100);
        return rows.stream()
                .map(r -> r.get("eq_group_id").toString())
                .collect(Collectors.toList());
    }

    /**
     * GA 후보 수집:
     *   1) OUTBOUND 부모 오더 (자식 MOVE 카운트 JOIN 포함)
     *   2) 여유 슬롯이 남으면 부모 없는 독립 MOVE 추가
     *   3) 등장한 모든 loc_code 를 LocationResolver 로 일괄 좌표 변환
     */
    @SuppressWarnings("rawtypes")
    private List<TaskDto> gatherCandidates(String eqGroupId) {
        Set<String> locCodes = new HashSet<>();

        // OUTBOUND 후보 + locCode 수집
        List<Map> outboundRows = fetchOutboundRows(eqGroupId);
        collectLocCodes(outboundRows, locCodes);

        // 슬롯 여유 시 MOVE 후보 추가
        List<Map> moveRows = Collections.emptyList();
        if (outboundRows.size() < MAX_CANDIDATES_PER_GROUP) {
            moveRows = fetchMoveRows(eqGroupId, MAX_CANDIDATES_PER_GROUP - outboundRows.size());
            collectLocCodes(moveRows, locCodes);
        }

        // 좌표 일괄 변환
        Map<String, int[]> coords = locationResolver.resolveBatch(eqGroupId, locCodes);

        // TaskDto 빌드
        List<TaskDto> result = new ArrayList<>(outboundRows.size() + moveRows.size());
        for (Map row : outboundRows) result.add(toTaskDto(row, OT_OUTBOUND, coords));
        for (Map row : moveRows)     result.add(toTaskDto(row, OT_MOVE,     coords));
        return result;
    }

    /**
     * OUTBOUND 부모 오더 + 자식 MOVE 카운트.
     */
    @SuppressWarnings("rawtypes")
    private List<Map> fetchOutboundRows(String eqGroupId) {
        String sql = """
            SELECT
                o.order_key,
                COALESCE(o.priority, 5) AS priority,
                COALESCE(o.level, 0) AS level,
                COALESCE(o.aging_count, 0) AS aging_count,
                o.from_loc_code, o.to_loc_code,
                COUNT(m.id) AS child_move_count
              FROM tb_wcs_shuttle_order o
              LEFT JOIN tb_wcs_shuttle_order m
                     ON m.parent_order_key = o.order_key
                    AND m.order_type       = :moveType
                    AND m.order_status     IN (:pendingStatuses)
                    AND m.ecs_if_status    = :readyStatus
             WHERE o.eq_group_id    = :eqGroupId
               AND o.order_type     = :outboundType
               AND o.order_status   IN (:pendingStatuses)
               AND o.ecs_if_status  = :readyStatus
             GROUP BY o.order_key, o.priority, o.level, o.aging_count,
                      o.from_loc_code, o.to_loc_code, o.created_at
             ORDER BY o.priority ASC, o.created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "moveType,pendingStatuses,readyStatus,eqGroupId,outboundType",
                OT_MOVE, SO_PENDING, ECS_READY, eqGroupId, OT_OUTBOUND);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, MAX_CANDIDATES_PER_GROUP);
    }

    /**
     * 부모 없는 독립 MOVE 오더.
     */
    @SuppressWarnings("rawtypes")
    private List<Map> fetchMoveRows(String eqGroupId, int limit) {
        String sql = """
            SELECT order_key,
                   COALESCE(priority, 5) AS priority,
                   COALESCE(level, 0) AS level,
                   COALESCE(aging_count, 0) AS aging_count,
                   from_loc_code, to_loc_code
              FROM tb_wcs_shuttle_order
             WHERE eq_group_id = :eqGroupId
               AND order_type = :moveType
               AND order_status IN (:pendingStatuses)
               AND ecs_if_status = :readyStatus
               AND (parent_order_key IS NULL OR parent_order_key = '')
             ORDER BY priority ASC, created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,moveType,pendingStatuses,readyStatus",
                eqGroupId, OT_MOVE, SO_PENDING, ECS_READY);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, limit);
    }

    /**
     * 결과 row 의 from/to loc_code 를 set 에 누적.
     */
    @SuppressWarnings("rawtypes")
    private void collectLocCodes(List<Map> rows, Set<String> bag) {
        for (Map row : rows) {
            addIfPresent(bag, (String) row.get("from_loc_code"));
            addIfPresent(bag, (String) row.get("to_loc_code"));
        }
    }

    private void addIfPresent(Set<String> bag, String code) {
        if (ValueUtil.isNotEmpty(code)) bag.add(code);
    }

    /**
     * SQL row + 좌표 캐시 → TaskDto.
     */
    @SuppressWarnings("rawtypes")
    private TaskDto toTaskDto(Map row, String orderType, Map<String, int[]> coords) {
        int[] from = resolveCoords(coords, (String) row.get("from_loc_code"));
        int[] to   = resolveCoords(coords, (String) row.get("to_loc_code"));
        boolean isOutbound = OT_OUTBOUND.equals(orderType);
        return TaskDto.builder()
                .orderKey((String) row.get("order_key"))
                .orderType(orderType)
                .priority(toInt(row.get("priority"), 5))
                .level(toInt(row.get("level"), 0))
                .agingCount(toInt(row.get("aging_count"), 0))
                .childMoveCount(isOutbound ? toInt(row.get("child_move_count"), 0) : 0)
                .fromRow(from[0]).fromBay(from[1]).fromLevel(from[2])
                .toRow(to[0]).toBay(to[1]).toLevel(to[2])
                .build();
    }

    /**
     * 좌표 캐시 hit, 미스 시 LocationResolver.parseFallback.
     */
    private int[] resolveCoords(Map<String, int[]> coords, String locCode) {
        if (ValueUtil.isEmpty(locCode)) return new int[]{0, 0, 0};
        int[] hit = coords.get(locCode);
        return ValueUtil.isNotEmpty(hit) ? hit : locationResolver.parseFallback(locCode);
    }

    /**
     * 셔틀 실시간 상태 — use_yn=true, plc_cmd_id 미보유만.
     */
    @SuppressWarnings("rawtypes")
    private List<ShuttleDto> fetchShuttles(String eqGroupId) {
        String sql = """
            SELECT car.id,
                   car.row     AS car_row,
                   car.bay     AS car_bay,
                   car.level   AS car_level,
                   car.cargo_yn AS cargo_yn
              FROM tb_eq_car_mst car
              JOIN tb_eq_mst eq ON eq.id = car.rack_eq_id
             WHERE eq.eq_group_id = :eqGroupId
               AND eq.type = :rackType
               AND car.use_yn = true
               AND (car.plc_cmd_id IS NULL OR car.plc_cmd_id = 0)
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,rackType", eqGroupId, EcsDBConsts.EqType.RACK.getValue());
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        return rows.stream()
                .map(r -> ShuttleDto.builder()
                        .id(String.valueOf(r.get("id")))
                        .currentRow(toInt(r.get("car_row"), 0))
                        .currentBay(toInt(r.get("car_bay"), 0))
                        .currentLevel(toInt(r.get("car_level"), 1))
                        .cargoYn(Boolean.TRUE.equals(r.get("cargo_yn")))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 리프트 실시간 상태.
     */
    @SuppressWarnings("rawtypes")
    private List<LiftDto> fetchLifts(String eqGroupId) {
        String sql = """
            SELECT lift.id AS id,
                   lift.level AS current_level,
                   lift.rack_row     AS aisle_row
              FROM tb_eq_cv_mst lift
              JOIN tb_eq_mst eq ON eq.id = lift.eq_id
             WHERE eq.eq_group_id = :eqGroupId
               AND eq.type = :cvType
               AND lift.type = :liftType
               AND lift.use_yn = true
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,cvType,liftType",
                eqGroupId, EcsDBConsts.EqType.CONVEYOR.getValue(), EcsDBConsts.ConveyorType.LIFT.getValue());

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        return rows.stream()
                .map(r -> LiftDto.builder()
                        .id(String.valueOf(r.get("id")))
                        .currentLevel(toInt(r.get("current_level"), 1))
                        .aisleRow(toInt(r.get("aisle_row"), 1))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 30초 버킷 단위 결정성 시드 — 같은 윈도우 내 동일 결과 보장.
     */
    private long deterministicSeed(String eqGroupId) {
        long bucket = System.currentTimeMillis() / SEED_BUCKET_MS;
        return ((long) eqGroupId.hashCode() << 32) ^ bucket;
    }

    private static int toInt(Object v, int def) {
        return (v instanceof Number n) ? n.intValue() : def;
    }
}
