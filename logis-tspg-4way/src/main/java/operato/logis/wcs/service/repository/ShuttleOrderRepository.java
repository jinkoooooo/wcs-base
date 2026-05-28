package operato.logis.wcs.service.repository;

import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TbWcsShuttleOrder 영속성 전담 DAO.
 *
 * 셔틀 주문의 조회·생성·상태 전이·의존(prerequisite) 관계 기동을 한 aggregate 단위로 캡슐화한다.
 * order_status 전이는 transitionOrderStatus 가 유일한 정식 통로이며, DB 현재값을 조건으로 검사해 역행을 원자적으로 차단한다.
 */
@Repository
public class ShuttleOrderRepository extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ShuttleOrderRepository.class);

    /** PK 로 단건 조회. */
    public TbWcsShuttleOrder findById(String id) {
        return this.queryManager.select(TbWcsShuttleOrder.class, id);
    }

    /** order_key 로 단건 조회. */
    public TbWcsShuttleOrder findByOrderKey(String orderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", orderKey);
        return this.queryManager.selectByCondition(TbWcsShuttleOrder.class, condition);
    }

    /** order_status 로 목록 조회. */
    public List<TbWcsShuttleOrder> findByStatus(int orderStatus) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", orderStatus);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** eqGroup 으로 목록 조회. */
    public List<TbWcsShuttleOrder> findByEqGroupId(String eqGroupId) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** 신규 주문 insert. */
    public TbWcsShuttleOrder insert(TbWcsShuttleOrder entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 지정 컬럼만 update. */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsShuttleOrder update(TbWcsShuttleOrder entity, String... columns) {
        this.queryManager.update(entity, columns);
        return entity;
    }

    /**
     * order_status 전이의 유일한 정식 통로.
     *
     * 엔티티 메모리 값이 아니라 DB 현재값을 WHERE 조건으로 검사하므로, 동시 트랜잭션이
     * 먼저 완전 종료 상태로 올려둔 경우 0 row 갱신되어 역행이 원자적으로 차단된다.
     *
     * force=false (자동 경로 전부): 완전 종료(COMPLETED/CANCELLED/ABORTED) 상태면 0 row, 진행중/에러 상태에서는 전이 — 늦은 중복 콜백 역행 차단.
     * force=true (운영자 명시 복구 전용): 모든 가드 무시. 에러 상태 재개·비정상 종료 복구에만 사용하고 자동 콜백 경로에서는 금지.
     * 반환값은 갱신된 row 수(0 이면 force=false 기준 이미 완전 종료 → 호출자는 전이 무시로 판단).
     */
    @Transactional(rollbackFor = Exception.class)
    public int transitionOrderStatus(String orderKey, int newStatus, boolean force) {
        if (ValueUtil.isEmpty(orderKey)) return 0;

        // force 면 무조건 전이, 아니면 완전 종료 상태 제외 조건부 전이
        String sql;
        Map<String, Object> params;
        if (force) {
            sql = """
                UPDATE tb_wcs_shuttle_order
                   SET order_status = :newStatus
                 WHERE order_key = :orderKey
                """;
            params = ValueUtil.newMap("newStatus,orderKey", newStatus, orderKey);
        } else {
            sql = """
                UPDATE tb_wcs_shuttle_order
                   SET order_status = :newStatus
                 WHERE order_key = :orderKey
                   AND order_status NOT IN (:terminalStatuses)
                """;
            params = ValueUtil.newMap("newStatus,orderKey,terminalStatuses",
                    newStatus, orderKey, ShuttleOrderStatus.terminalCodes());
        }

        // 0 row + 비강제면 역행 차단된 것
        int updated = this.queryManager.executeBySql(sql, params);
        if (updated == 0 && !force) {
            logger.warn("[ Order ][ Shuttle ] transition blocked, already terminal - orderKey={}, newStatus={}",
                    orderKey, newStatus);
        }
        return updated;
    }

    /** 오늘자 + 타입 + ecsIfStatus 의 최신 주문 1건 조회. */
    public TbWcsShuttleOrder findLatestTodayOrderByTypeAndEcsIfStatus(OrderType orderType, EcsIfStatus ecsIfStatus) {
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", OrmConstants.LIKE, today);
        condition.addFilter("order_type", orderType.code());
        condition.addFilter("ecs_if_status", ecsIfStatus.code());
        condition.addOrder("order_key", false);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbWcsShuttleOrder.class, condition);
    }

    /** 특정 eqGroup 의 MOVE 주문 목록 조회 (order_key 내림차순). */
    public List<TbWcsShuttleOrder> findByEqGroupIdAndOrderType(String kMatTspg, String move) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", kMatTspg);
        condition.addFilter("order_type", OrderType.MOVE.code());
        condition.addOrder("order_key", false);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** order_status 가 기준값 이상인 주문 목록 조회 (생성순). */
    public List<TbWcsShuttleOrder> findByStatusGreaterThanEqual(int status) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", OrmConstants.GREATER_THAN_EQUAL, status);
        condition.addOrder("createdAt", true);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** 포트별(to_loc_code) 미완료 주문 수 집계. */
    public Map<String, Long> getActiveOrderCountByPort(String eqGroupId) {
        String sql = """
            SELECT to_loc_code as port_code, count(*) as cnt
              FROM tb_wcs_shuttle_order
             WHERE eq_group_id = :eqGroupId
               AND order_status < :completeStatus
             GROUP BY to_loc_code
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,completeStatus",
                eqGroupId, ShuttleOrderStatus.COMPLETED.code());

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        // 포트코드 -> 건수 맵으로 변환 (포트코드 비어있는 행 제외)
        Map<String, Long> resultMap = new HashMap<>();
        for (Map row : rows) {
            String portCode = (String) row.get("port_code");
            Long count = Long.valueOf(row.get("cnt").toString());
            if (ValueUtil.isNotEmpty(portCode)) {
                resultMap.put(portCode, count);
            }
        }
        return resultMap;
    }

    /**
     * 가장 오래된 WAITING OUTBOUND 주문 1건을 FOR UPDATE SKIP LOCKED 로 선점 조회.
     * 호출자 트랜잭션 필수(REQUIRED) — 락은 커밋/롤백 시 해제. 다중 인스턴스 이중 릴리즈 방지.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public TbWcsShuttleOrder findOldestWaitingOrderForUpdate(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return null;

        String sql = """
            SELECT *
              FROM tb_wcs_shuttle_order
             WHERE eq_group_id  = :eqGroupId
               AND order_type   = :orderType
               AND order_status = :waitingStatus
             ORDER BY priority ASC, created_at ASC
             LIMIT 1
             FOR UPDATE SKIP LOCKED
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,orderType,waitingStatus",
                eqGroupId,
                OrderType.OUTBOUND.code(),
                ShuttleOrderStatus.WAITING.codeAsIntOrNull()
        );

        List<TbWcsShuttleOrder> list = this.queryManager.selectListBySql(
                sql, params, TbWcsShuttleOrder.class, 0, 1);
        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }

    /** 완료된 주문에 의존하던 WAITING 주문을 선행조건 충족 시 CREATED 로 기동. */
    @Transactional(rollbackFor = Exception.class)
    public int wakeUpDependentsByCompletion(String completedKey) {
        if (ValueUtil.isEmpty(completedKey)) return 0;

        String sql = """
        UPDATE tb_wcs_shuttle_order O
           SET order_status = :createdStatus
         WHERE O.order_status  = :waitingStatus
           AND (
                O.prerequisite_order_key = :completedKey
                OR EXISTS (SELECT 1 FROM tb_wcs_shuttle_order trig
                            WHERE trig.order_key        = :completedKey
                              AND trig.parent_order_key = O.order_key
                              AND trig.order_type       = :moveType)
           )
           AND (
                O.prerequisite_order_key IS NULL
                OR EXISTS (SELECT 1 FROM tb_wcs_shuttle_order pre
                            WHERE pre.order_key    = O.prerequisite_order_key
                              AND pre.order_status >= :arrivedStatus)
           )
           AND NOT EXISTS (
                SELECT 1 FROM tb_wcs_shuttle_order c
                 WHERE c.parent_order_key = O.order_key
                   AND c.order_type       = :moveType
                   AND c.order_status     < :completedStatus
           )
        """;
        Map<String, Object> params = ValueUtil.newMap(
                "createdStatus,waitingStatus,completedKey,arrivedStatus,completedStatus,moveType",
                ShuttleOrderStatus.CREATED.codeAsIntOrNull(),
                ShuttleOrderStatus.WAITING.codeAsIntOrNull(),
                completedKey,
                ShuttleOrderStatus.ARRIVED.codeAsIntOrNull(),
                ShuttleOrderStatus.COMPLETED.codeAsIntOrNull(),
                OrderType.MOVE.code()
        );
        int updated = this.queryManager.executeBySql(sql, params);
        if (updated > 0) {
            logger.info("[ Order ][ Shuttle ] dependents awoken - completedKey={}, awoken={}", completedKey, updated);
        }
        return updated;
    }

    /** WAITING 주문의 선행조건(prerequisite)을 수동으로 끊는다. */
    @Transactional(rollbackFor = Exception.class)
    public int severPrerequisiteByOrderKey(String orderKey) {
        if (ValueUtil.isEmpty(orderKey)) return 0;

        String sql = """
            UPDATE tb_wcs_shuttle_order
               SET prerequisite_order_key = NULL
             WHERE order_key   = :orderKey
               AND order_status = :waitingStatus
               AND prerequisite_order_key IS NOT NULL
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "orderKey,waitingStatus",
                orderKey, ShuttleOrderStatus.WAITING.codeAsIntOrNull()
        );
        int updated = this.queryManager.executeBySql(sql, params);
        if (updated > 0) {
            logger.warn("[ Order ][ Shuttle ] prerequisite severed - orderKey={}", orderKey);
        }
        return updated;
    }

    /** 자기 자신이 WAITING 이고 선행조건 충족 시 CREATED 로 기동. */
    @Transactional(rollbackFor = Exception.class)
    public int wakeUpSelfIfReady(String orderKey) {
        if (ValueUtil.isEmpty(orderKey)) return 0;

        String sql = """
        UPDATE tb_wcs_shuttle_order O
           SET order_status = :createdStatus
         WHERE O.order_key      = :orderKey
           AND O.order_status   = :waitingStatus
           AND (
                O.prerequisite_order_key IS NULL
                OR EXISTS (SELECT 1 FROM tb_wcs_shuttle_order pre
                            WHERE pre.order_key    = O.prerequisite_order_key
                              AND pre.order_status >= :arrivedStatus)
           )
           AND NOT EXISTS (
                SELECT 1 FROM tb_wcs_shuttle_order c
                 WHERE c.parent_order_key = O.order_key
                   AND c.order_type       = :moveType
                   AND c.order_status     < :completedStatus
           )
        """;
        Map<String, Object> params = ValueUtil.newMap(
                "createdStatus,orderKey,waitingStatus,arrivedStatus,completedStatus,moveType",
                ShuttleOrderStatus.CREATED.codeAsIntOrNull(),
                orderKey,
                ShuttleOrderStatus.WAITING.codeAsIntOrNull(),
                ShuttleOrderStatus.ARRIVED.codeAsIntOrNull(),
                ShuttleOrderStatus.COMPLETED.codeAsIntOrNull(),
                OrderType.MOVE.code()
        );
        return this.queryManager.executeBySql(sql, params);
    }

    /** 선행조건이 취소/종료되어 영원히 못 깨어나는 WAITING 주문 조회. */
    public List<TbWcsShuttleOrder> findStuckWaitingWithDeadPrerequisite(String eqGroupId) {
        String sql = """
            SELECT o.* FROM tb_wcs_shuttle_order o
              JOIN tb_wcs_shuttle_order pre
                ON pre.order_key = o.prerequisite_order_key
             WHERE o.order_status     = :waitingStatus
               AND o.prerequisite_order_key IS NOT NULL
               AND pre.order_status  >= :cancelledStatus
               AND (:eqGroupId IS NULL OR o.eq_group_id = :eqGroupId)
             ORDER BY o.created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "waitingStatus,cancelledStatus,eqGroupId",
                ShuttleOrderStatus.WAITING.codeAsIntOrNull(),
                ShuttleOrderStatus.CANCELLED.codeAsIntOrNull(),
                eqGroupId
        );
        return this.queryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);
    }

    /** 진행중 INBOUND 주문의 출발 포트(from_loc_code) 집합 조회. */
    public Set<String> getActiveInboundPorts(String eqGroupId) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("order_type", OrderType.INBOUND.code());
        condition.addFilter("order_status", OrmConstants.GREATER_THAN_EQUAL, ShuttleOrderStatus.RUNNING.code());
        condition.addFilter("order_status", OrmConstants.LESS_THAN, ShuttleOrderStatus.COMPLETED.code());

        List<TbWcsShuttleOrder> activeInbounds = this.queryManager.selectList(TbWcsShuttleOrder.class, condition);

        return activeInbounds.stream()
                .map(TbWcsShuttleOrder::getFromLocCode)
                .filter(ValueUtil::isNotEmpty)
                .collect(Collectors.toSet());
    }

    /** group_order_key 로 묶인 주문 목록 조회. */
    public List<TbWcsShuttleOrder> findByGroupOrderKey(String groupOrderKey) {
        if (ValueUtil.isEmpty(groupOrderKey)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("group_order_key", groupOrderKey);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** host_order_key 로 파생된 주문 목록 조회. */
    public List<TbWcsShuttleOrder> findByHostOrderKey(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** parent_order_key 로 자식 주문 목록 조회. */
    public List<TbWcsShuttleOrder> findByParentOrderKey(String parentOrderKey) {
        if (ValueUtil.isEmpty(parentOrderKey)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("parent_order_key", parentOrderKey);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** barcode 로 주문 목록 조회 (생성 역순). */
    public List<TbWcsShuttleOrder> findByBarcode(String barcode) {
        if (ValueUtil.isEmpty(barcode)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("barcode", barcode);
        condition.addOrder("createdAt", false);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 재입고 대기 파렛트 집계 — follow_up_since 가 set 된(미재입고) 파렛트.
     * 파렛트별 가장 이른 follow_up_since 와 경과(분)를 반환. due 판정은 호출측.
     */
    public List<Map> findFollowUpPendingPallets() {
        // 파렛트(barcode+host+eq)별 가장 이른 follow_up_since 행 1건 — sub_order_type 도 그 행 값
        // follow_up_since 는 ISO 문자열로 반환 — elidom 직렬화가 Date 를 date-only 로 깎아 프론트 경과 계산이 틀어짐
        String sql = """
            SELECT pallet_barcode,
                   host_order_key,
                   eq_group_id,
                   sub_order_type,
                   to_char(follow_up_since, 'YYYY-MM-DD"T"HH24:MI:SS') AS follow_up_since,
                   elapsed_min
              FROM (
                SELECT DISTINCT ON (barcode, host_order_key, eq_group_id)
                       barcode                                            AS pallet_barcode,
                       host_order_key,
                       eq_group_id,
                       sub_order_type,
                       follow_up_since,
                       EXTRACT(EPOCH FROM (NOW() - follow_up_since)) / 60 AS elapsed_min
                  FROM tb_wcs_shuttle_order
                 WHERE follow_up_since IS NOT NULL
                   AND barcode IS NOT NULL
                 ORDER BY barcode, host_order_key, eq_group_id, follow_up_since ASC
            ) p
            ORDER BY follow_up_since ASC
            """;
        return this.queryManager.selectListBySql(sql, new HashMap<String, Object>(), Map.class, 0, 0);
    }

    /** host_order_key + 주문 타입으로 목록 조회. */
    public List<TbWcsShuttleOrder> findByHostOrderKeyAndOrderType(String hostOrderKey, OrderType orderType) {
        if (ValueUtil.isEmpty(hostOrderKey)) return Collections.emptyList();
        if (ValueUtil.isEmpty(orderType)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        condition.addFilter("order_type", orderType.code());
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** eqGroup 의 미완료 주문 수. */
    public long countPending(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return 0L;

        String sql = """
            SELECT COUNT(*) AS cnt FROM tb_wcs_shuttle_order
             WHERE eq_group_id = :eqGroupId
               AND order_status < :completeStatus
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,completeStatus",
                eqGroupId, ShuttleOrderStatus.COMPLETED.code());

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return 0L;
        Object cnt = rows.get(0).get("cnt");
        return cnt instanceof Number n ? n.longValue() : 0L;
    }

    /** 동일 barcode + 타입의 진행중 주문 존재 여부. */
    public boolean hasInProgressOrderByBarcode(String barcode, OrderType orderType) {
        if (ValueUtil.isEmpty(barcode) || ValueUtil.isEmpty(orderType)) return false;

        String sql = """
            SELECT COUNT(*) AS cnt FROM tb_wcs_shuttle_order
             WHERE barcode = :barcode
               AND order_type = :orderType
               AND order_status < :completeStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "barcode,orderType,completeStatus",
                barcode,
                orderType.codeAsString(),
                ShuttleOrderStatus.COMPLETED.code());

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return false;
        Object cnt = rows.get(0).get("cnt");
        return cnt instanceof Number n && n.longValue() > 0;
    }

    /** ECS 로 전송된(SENT) 미완료 주문을 우선순위/생성순으로 batchSize 만큼 조회. */
    public List<TbWcsShuttleOrder> findSentOrders(String eqGroupId, int batchSize) {
        if (ValueUtil.isEmpty(eqGroupId)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("ecs_if_status", EcsIfStatus.SENT.code());
        condition.addFilter("order_status", OrmConstants.LESS_THAN,
                ShuttleOrderStatus.COMPLETED.code());
        condition.addOrder("priority", true);
        condition.addOrder("createdAt", true);
        condition.setMaxResultSize(Math.max(1, batchSize));

        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /** barcode 로 입고 준비된(WAITING/CREATED + READY) INBOUND 주문 1건 조회. */
    public TbWcsShuttleOrder findReadyInboundByBarcode(String eqGroupId, String barcode) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(barcode)) return null;

        String sql = """
            SELECT *
              FROM tb_wcs_shuttle_order
             WHERE eq_group_id   = :eqGroupId
               AND order_type    = :inboundType
               AND barcode       = :barcode
               AND order_status  in (:inboundReadyStatus)
               AND ecs_if_status = :readyStatus
             ORDER BY priority ASC, created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,inboundType,barcode,inboundReadyStatus,readyStatus",
                eqGroupId,
                OrderType.INBOUND.codeAsString(),
                barcode,
                List.of(ShuttleOrderStatus.WAITING.code(),
                        ShuttleOrderStatus.CREATED.code()),
                EcsIfStatus.READY.code());

        List<TbWcsShuttleOrder> list = this.queryManager.selectListBySql(
                sql, params, TbWcsShuttleOrder.class, 0, 1);
        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }

    /** 같은 적치 위치/재고에 묶인, 자신을 제외한 CREATED INBOUND 주문 목록 조회. */
    public List<TbWcsShuttleOrder> findOtherCreatedInboundOrdersAt(String eqGroupId,
                                                                   String bestLocId,
                                                                   String bestStockId,
                                                                   String excludeOrderKey) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(bestLocId)
                || ValueUtil.isEmpty(bestStockId) || ValueUtil.isEmpty(excludeOrderKey)) {
            return Collections.emptyList();
        }
        String sql = """
            SELECT * FROM tb_wcs_shuttle_order
             WHERE eq_group_id       = :eqGroupId
               AND to_loc_code       = :bestLocId
               AND carrying_stock_id = :bestStockId
               AND order_type        = :inboundType
               AND order_key        != :currentOrderKey
               AND order_status      = :createdStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,bestLocId,bestStockId,inboundType,currentOrderKey,createdStatus",
                eqGroupId, bestLocId, bestStockId,
                OrderType.INBOUND.codeAsString(),
                excludeOrderKey,
                ShuttleOrderStatus.CREATED.codeAsIntOrNull());
        return this.queryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);
    }
}
