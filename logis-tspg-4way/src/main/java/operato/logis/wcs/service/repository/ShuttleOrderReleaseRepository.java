package operato.logis.wcs.service.repository;

import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import org.springframework.stereotype.Repository;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TbWcsShuttleOrder 릴리즈 스케줄러 전용 raw SQL 모음 DAO.
 *
 * CAS 락/롤백, 번들 집계, 유휴 셔틀 카운트, 포트 가용성 조회 등 스케줄러 송신 결정에 쓰는 쿼리를 모은다.
 * 호출자 트랜잭션에 참여한다(별도 @Transactional 부착 없음).
 */
@Repository
public class ShuttleOrderReleaseRepository extends AbstractQueryService {

    private static final Integer ECS_READY    = EcsIfStatus.READY.code();
    private static final Integer ECS_SENDING  = EcsIfStatus.SENDING.code();

    private static final Integer SO_CREATED   = ShuttleOrderStatus.CREATED.code();
    private static final Integer SO_WAITING   = ShuttleOrderStatus.WAITING.code();
    private static final Integer SO_ARRIVED   = ShuttleOrderStatus.ARRIVED.code();
    private static final Integer SO_COMPLETED = ShuttleOrderStatus.COMPLETED.code();
    private static final List<Integer> SO_PENDING_STATUSES = List.of(SO_CREATED, SO_WAITING);

    private static final String OT_INBOUND  = OrderType.INBOUND.code();
    private static final String OT_OUTBOUND = OrderType.OUTBOUND.code();
    private static final String OT_MOVE     = OrderType.MOVE.code();

    private static final int DEFAULT_PRIORITY = 5;

    /** 단일 주문 CAS 락 — ecs_if_status READY -> SENDING 전이 성공 여부 반환. */
    public boolean lockOrderAtomically(String orderKey, String eqGroupId) {
        String sql = """
            UPDATE tb_wcs_shuttle_order
               SET ecs_if_status = :sendingStatus
             WHERE order_key     = :orderKey
               AND eq_group_id   = :eqGroupId
               AND ecs_if_status = :readyStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "sendingStatus,orderKey,eqGroupId,readyStatus",
                ECS_SENDING, orderKey, eqGroupId, ECS_READY);
        return this.queryManager.executeBySql(sql, params) > 0;
    }

    /** 단일 주문 락 롤백 — ecs_if_status SENDING -> READY 되돌림. */
    public void rollbackOrder(String orderKey, String eqGroupId) {
        String sql = """
            UPDATE tb_wcs_shuttle_order
               SET ecs_if_status = :readyStatus
             WHERE order_key     = :orderKey
               AND eq_group_id   = :eqGroupId
               AND ecs_if_status = :sendingStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "readyStatus,orderKey,eqGroupId,sendingStatus",
                ECS_READY, orderKey, eqGroupId, ECS_SENDING);
        this.queryManager.executeBySql(sql, params);
    }

    /** 번들(parent OUTBOUND + child MOVE) CAS 락 — 묶음 단위 READY -> SENDING 전이 성공 여부. */
    public boolean lockBundleAtomically(String parentOrderKey, String eqGroupId) {
        String sql = """
            UPDATE tb_wcs_shuttle_order
               SET ecs_if_status = :sendingStatus
             WHERE eq_group_id   = :eqGroupId
               AND ecs_if_status = :readyStatus
               AND (
                   (order_type = :outboundType AND order_key        = :parentOrderKey)
                OR (order_type = :moveType     AND parent_order_key = :parentOrderKey)
               )
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "sendingStatus,eqGroupId,readyStatus,outboundType,moveType,parentOrderKey",
                ECS_SENDING, eqGroupId, ECS_READY, OT_OUTBOUND, OT_MOVE, parentOrderKey);
        return this.queryManager.executeBySql(sql, params) > 0;
    }

    /** READY 상태 OUTBOUND parent 의 aging_count +1 (송신 대기 노화 추적). */
    public void incrementAgingCount(String parentOrderKey, String eqGroupId) {
        String sql = """
            UPDATE tb_wcs_shuttle_order
               SET aging_count = COALESCE(aging_count, 0) + 1
             WHERE eq_group_id   = :eqGroupId
               AND order_type    = :outboundType
               AND order_key     = :parentOrderKey
               AND ecs_if_status = :readyStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,outboundType,parentOrderKey,readyStatus",
                eqGroupId, OT_OUTBOUND, parentOrderKey, ECS_READY);
        this.queryManager.executeBySql(sql, params);
    }

    /** OUTBOUND parent 의 aging_count 0 으로 리셋. */
    public void resetAgingCount(String parentOrderKey, String eqGroupId) {
        String sql = """
            UPDATE tb_wcs_shuttle_order
               SET aging_count   = 0
             WHERE eq_group_id   = :eqGroupId
               AND order_type    = :outboundType
               AND order_key     = :parentOrderKey
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,outboundType,parentOrderKey",
                eqGroupId, OT_OUTBOUND, parentOrderKey);
        this.queryManager.executeBySql(sql, params);
    }

    /** SENDING 으로 락 잡은 단일 주문 1건 조회. */
    public TbWcsShuttleOrder fetchLockedOrder(String orderKey, String eqGroupId) {
        String sql = """
            SELECT *
              FROM tb_wcs_shuttle_order
             WHERE order_key     = :orderKey
               AND eq_group_id   = :eqGroupId
               AND ecs_if_status = :sendingStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "orderKey,eqGroupId,sendingStatus",
                orderKey, eqGroupId, ECS_SENDING);
        List<TbWcsShuttleOrder> list = this.queryManager.selectListBySql(
                sql, params, TbWcsShuttleOrder.class, 0, 1);
        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }

    /** SENDING 락 잡힌 번들 구성원을 타입별로 조회. OUTBOUND 는 order_key, 그 외는 parent_order_key 로 매칭. */
    public List<TbWcsShuttleOrder> fetchBundleOrdersByType(
            String parentOrderKey, String eqGroupId, String orderType) {
        // OUTBOUND parent 는 자기 키, child(MOVE) 는 parent 키로 매칭
        String parentCondition = OT_OUTBOUND.equals(orderType)
                ? "AND o.order_key = :parentOrderKey"
                : "AND o.parent_order_key = :parentOrderKey";
        String sql = """
            SELECT * FROM tb_wcs_shuttle_order o
             WHERE o.eq_group_id = :eqGroupId
               AND o.order_type = :orderType
               AND o.ecs_if_status = :sendingStatus
               %s
             ORDER BY o.priority ASC, o.created_at ASC
            """.formatted(parentCondition);

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,orderType,sendingStatus,parentOrderKey",
                eqGroupId, orderType, ECS_SENDING, parentOrderKey);

        return this.queryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);
    }

    /** OUTBOUND 번들 송신 후보 조회 — 포트 가용·선행조건 충족·미완료 자식 없음 필터 후 우선순위순. */
    public List<Map> fetchOutboundBundleCandidates(String eqGroupId) {
        String sql = """
        SELECT
            o.order_key                              AS parent_order_key,
            COALESCE(o.priority, :defaultPriority)   AS priority,
            COALESCE(o.level, 0)                     AS level,
            COALESCE(o.aging_count,  0)              AS aging_count,
            COUNT(m.id)                              AS child_move_count
          FROM tb_wcs_shuttle_order o
          JOIN tb_inventory_location toLoc
            ON toLoc.loc_group = o.eq_group_id
           AND toLoc.loc_id    = o.to_loc_code
          LEFT JOIN tb_wcs_shuttle_order m
                 ON m.parent_order_key = o.order_key
                AND m.order_type       = :moveType
                AND m.order_status     IN (:pendingStatuses)
                AND m.ecs_if_status    = :readyStatus
         WHERE o.eq_group_id    = :eqGroupId
           AND o.order_type     = :outboundType
           AND o.order_status   IN (:pendingStatuses)
           AND o.ecs_if_status  = :readyStatus
           AND toLoc.is_enabled = true
           AND toLoc.loc_type IN ('OUTBOUND_PORT', 'IN_OUTBOUND_PORT')
           AND (
                toLoc.loc_type = 'OUTBOUND_PORT'
                OR (toLoc.loc_type = 'IN_OUTBOUND_PORT'
                    AND toLoc.port_mode IN ('OUTBOUND', 'OUTBOUND_PRIORITY', 'IDLE'))
           )
           AND (
                o.prerequisite_order_key IS NULL
                OR EXISTS (
                    SELECT 1 FROM tb_wcs_shuttle_order pre
                     WHERE pre.order_key    = o.prerequisite_order_key
                       AND pre.order_status >= :arrivedStatus
                )
           )
           AND NOT EXISTS (
                SELECT 1 FROM tb_wcs_shuttle_order c
                 WHERE c.parent_order_key = o.order_key
                   AND c.order_type       = :moveType
                   AND c.order_status     < :completedStatus
           )
         GROUP BY o.order_key, o.priority, o.level,
                  o.aging_count, o.created_at
         ORDER BY COALESCE(o.priority, :defaultPriority) ASC, COUNT(m.id) ASC, o.created_at ASC
        """;
        Map<String, Object> params = ValueUtil.newMap(
                "defaultPriority,moveType,pendingStatuses,readyStatus,eqGroupId,outboundType,arrivedStatus,completedStatus",
                DEFAULT_PRIORITY, OT_MOVE, SO_PENDING_STATUSES, ECS_READY, eqGroupId, OT_OUTBOUND, SO_ARRIVED, SO_COMPLETED);

        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /** 부모 없는 standalone MOVE 송신 후보 조회 — level 별·선행조건 충족 필터 후 우선순위순. */
    public List<Map> fetchStandaloneMoveCandidates(String eqGroupId, int level, int limit) {
        String sql = """
            SELECT o.order_key, COALESCE(o.priority, :defaultPriority) AS priority
              FROM tb_wcs_shuttle_order o
             WHERE o.eq_group_id    = :eqGroupId
               AND o.level          = :level
               AND o.order_type     = :orderType
               AND o.order_status   IN (:pendingStatuses)
               AND o.ecs_if_status  = :readyStatus
               AND (o.parent_order_key IS NULL OR o.parent_order_key = '')
               AND (
                    o.prerequisite_order_key IS NULL
                    OR EXISTS (
                        SELECT 1 FROM tb_wcs_shuttle_order pre
                         WHERE pre.order_key    = o.prerequisite_order_key
                           AND pre.order_status >= :arrivedStatus
                    )
               )
             ORDER BY o.priority ASC, o.created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,level,orderType,pendingStatuses,readyStatus,defaultPriority,arrivedStatus",
                eqGroupId, level, OT_MOVE, SO_PENDING_STATUSES, ECS_READY, DEFAULT_PRIORITY, SO_ARRIVED);

        return this.queryManager.selectListBySql(sql, params, Map.class, 0, limit);
    }

    /**
     * WAITING OUTBOUND parent 의 obstacle child MOVE 후보 조회.
     *
     * R1 메커니즘의 핵심: parent 가 prereq 차단되어 bundle 후보에서 빠진 상태에서
     * 자식들이 stuck 되지 않도록 별도 추출한다. 자식은 부모와 무관하게 standalone 방식으로 송신.
     *
     * 조건:
     *   - MOVE / pending / ECS READY
     *   - parent_order_key 가 WAITING 상태 OUTBOUND 를 가리킴
     *   - level 필터로 quota 정합
     *
     * 자식 본인은 prereq=NULL(방해물 자식에는 prereq 미설정) 이라 prereq 차단 평가 안 함.
     */
    public List<Map> fetchObstacleChildrenForWaitingParents(String eqGroupId, int level, int limit) {
        String sql = """
            SELECT c.order_key, COALESCE(c.priority, :defaultPriority) AS priority
              FROM tb_wcs_shuttle_order c
              JOIN tb_wcs_shuttle_order p
                ON p.order_key = c.parent_order_key
             WHERE c.eq_group_id   = :eqGroupId
               AND c.level         = :level
               AND c.order_type    = :moveType
               AND c.order_status  IN (:pendingStatuses)
               AND c.ecs_if_status = :readyStatus
               AND p.order_type    = :outboundType
               AND p.order_status  = :waitingStatus
             ORDER BY c.priority ASC, c.created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,level,moveType,pendingStatuses,readyStatus,outboundType,waitingStatus,defaultPriority",
                eqGroupId, level, OT_MOVE, SO_PENDING_STATUSES, ECS_READY,
                OT_OUTBOUND, SO_WAITING, DEFAULT_PRIORITY);

        return this.queryManager.selectListBySql(sql, params, Map.class, 0, limit);
    }

    /** origin_level 별 유휴 셔틀 수 집계 (quota 산정용). */
    public Map<Integer, Integer> countIdleShuttlesByOriginLevel(String eqGroupId) {
        String sql = """
        SELECT car.origin_level, COUNT(*) AS cnt
          FROM tb_eq_car_mst car
          JOIN tb_eq_mst eq ON eq.id = car.rack_eq_id
         WHERE eq.eq_group_id    = :eqGroupId
           AND eq.type        = :rackType
           AND car.use_yn        = true
           AND (car.plc_cmd_id IS NULL OR car.plc_cmd_id = car.plc_cmd_id)
           AND car.origin_level IS NOT NULL
         GROUP BY car.origin_level
        """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,rackType",
                eqGroupId, EcsDBConsts.EqType.RACK.getValue());

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        // level -> count 맵으로 변환 (level 이 숫자 아닌 행 제외)
        Map<Integer, Integer> result = new HashMap<>();
        for (Map r : rows) {
            Object levelObj = r.get("origin_level");
            if (!(levelObj instanceof Number levelNum)) continue;
            result.put(levelNum.intValue(), toInt(r.get("cnt"), 0));
        }
        return result;
    }

    /** level 별 미송신(READY + pending) 주문 수 집계, 지정 타입만. */
    public Map<Integer, Integer> countPendingOrdersByLevel(String eqGroupId, List<String> types) {
        if (ValueUtil.isEmpty(types)) return Collections.emptyMap();
        String sql = """
            SELECT level, COUNT(*) AS cnt
              FROM tb_wcs_shuttle_order
             WHERE eq_group_id   = :eqGroupId
               AND order_type    IN (:types)
               AND order_status  IN (:pendingStatuses)
               AND ecs_if_status = :readyStatus
               AND level IS NOT NULL
             GROUP BY level
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,types,pendingStatuses,readyStatus",
                eqGroupId, types, SO_PENDING_STATUSES, ECS_READY);

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        // level -> count 맵으로 변환 (level 비어있는 행 제외)
        Map<Integer, Integer> result = new HashMap<>();
        for (Map r : rows) {
            Number levelNum = (Number) r.get("level");
            if (ValueUtil.isEmpty(levelNum)) continue;
            result.put(levelNum.intValue(), toInt(r.get("cnt"), 0));
        }
        return result;
    }

    /** 미송신(READY + pending) 주문이 존재하는 eqGroup 목록 조회 (최대 100). */
    public List<String> fetchGroupsWithPendingOrders() {
        String sql = """
            SELECT DISTINCT eq_group_id
              FROM tb_wcs_shuttle_order
             WHERE ecs_if_status = :readyStatus
               AND order_status  IN (:pendingStatuses)
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "readyStatus,pendingStatuses",
                ECS_READY, SO_PENDING_STATUSES);

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 100);
        return rows.stream()
                .map(r -> r.get("eq_group_id").toString())
                .collect(Collectors.toList());
    }

    /** 같은 host_order_key 의 재입고 INBOUND shuttle (CREATED) 짝 존재 여부. */
    public int countPairedReinboundShuttles(String parentHostKey, String parentOrderKey) {
        String sql = """
            SELECT COUNT(*) AS cnt FROM tb_wcs_shuttle_order
             WHERE host_order_key   = :parentHostKey
               AND order_type       = :inboundType
               AND order_status     = :createdStatus
               AND order_key       <> :parentOrderKey
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "parentHostKey,inboundType,createdStatus,parentOrderKey",
                parentHostKey, OT_INBOUND, SO_CREATED, parentOrderKey);

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        return ValueUtil.isEmpty(rows) ? 0 : toInt(rows.get(0).get("cnt"), 0);
    }

    /** 그룹의 입고/출고 포트 가용성 단발 조회. */
    public boolean[] checkPortAbilities(String eqGroupId) {
        String sql = """
        SELECT
          BOOL_OR(
            loc_type = 'INBOUND_PORT'
            OR (loc_type = 'IN_OUTBOUND_PORT' AND port_mode IN ('INBOUND', 'IDLE'))
          ) AS can_inbound,
          BOOL_OR(
            loc_type = 'OUTBOUND_PORT'
            OR (loc_type = 'IN_OUTBOUND_PORT'
                AND port_mode IN ('OUTBOUND', 'OUTBOUND_PRIORITY', 'IDLE'))
          ) AS can_outbound
        FROM tb_inventory_location
       WHERE loc_group = :locGroup
         AND is_enabled = true
         AND loc_type IN ('INBOUND_PORT', 'OUTBOUND_PORT', 'IN_OUTBOUND_PORT')
        """;
        Map<String, Object> params = ValueUtil.newMap("locGroup", eqGroupId);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return new boolean[]{false, false};
        Map row = rows.get(0);
        boolean canInbound  = Boolean.TRUE.equals(row.get("can_inbound"));
        boolean canOutbound = Boolean.TRUE.equals(row.get("can_outbound"));
        return new boolean[]{canInbound, canOutbound};
    }

    /** 포트 로케이션의 task_id 단발 조회 (포트 점유 검사용). */
    public String findPortTaskId(String eqGroupId, String portCode) {
        String sql = """
            SELECT task_id FROM tb_inventory_location
             WHERE loc_group = :eqGroupId AND loc_id = :portCode
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,portCode", eqGroupId, portCode);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return null;
        Object taskIdObj = rows.get(0).get("task_id");
        if (ValueUtil.isEmpty(taskIdObj)) return null;
        return String.valueOf(taskIdObj).trim();
    }

    /** Number 면 int 로, 아니면 기본값 반환. */
    private static int toInt(Object value, int defaultValue) {
        if (value instanceof Number number) return number.intValue();
        return defaultValue;
    }
}
