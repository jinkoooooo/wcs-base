package operato.logis.ecs.base.wcs.service.impl;

import operato.logis.ecs.base.ecs.entity.TbWcsCraneOrder;
import operato.logis.ecs.base.wcs.consts.WcsDomainEnums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** TbWcsCraneOrder Entity Service (DB 전용) */
@Service
public class TbWcsCraneOrderService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbWcsCraneOrderService.class);

    /** ID로 조회 */
    public TbWcsCraneOrder findById(String id) {
        return this.queryManager.select(TbWcsCraneOrder.class, id);
    }

    /** orderKey로 조회 */
    public TbWcsCraneOrder findByOrderKey(String orderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", orderKey);
        return this.queryManager.selectByCondition(TbWcsCraneOrder.class, condition);
    }

    /** 상태별 조회 */
    public List<TbWcsCraneOrder> findByStatus(int orderStatus) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", orderStatus);
        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }

    /** eqGroupId별 조회 */
    public List<TbWcsCraneOrder> findByEqGroupId(String eqGroupId) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }

    /** 신규 저장 */
    public TbWcsCraneOrder insert(TbWcsCraneOrder entity) {
        logger.debug("Inserting TbWcsCraneOrder: orderKey={}, orderType={}",
                entity.getOrderKey(), entity.getOrderType());
        this.queryManager.insert(entity);
        return entity;
    }

    /** 수정 */
    @Transactional
    public TbWcsCraneOrder update(TbWcsCraneOrder entity, String... columns) {
        logger.info("Updating TbWcsCraneOrder: orderKey={}, orderStatus={}",
                entity.getOrderKey(), entity.getOrderStatus());
        this.queryManager.update(entity, columns);
        return entity;
    }

    /** 상태 업데이트 */
    public void updateStatus(String orderKey, int orderStatus) {
        TbWcsCraneOrder entity = this.findByOrderKey(orderKey);
        if (entity != null) {
            entity.setOrderStatus(orderStatus);
            this.queryManager.update(entity);
        }
    }

    /** ECS 인터페이스 상태 업데이트 */
    public void updateEcsIfStatus(String orderKey, int ecsIfStatus) {
        TbWcsCraneOrder entity = this.findByOrderKey(orderKey);
        if (entity != null) {
            entity.setEcsIfStatus(ecsIfStatus);
            this.queryManager.update(entity);
        }
    }

    /** 상태 + ECS 인터페이스 상태 동시 업데이트 */
    public void updateStatusAndEcsIfStatus(String orderKey, int orderStatus, int ecsIfStatus) {
        TbWcsCraneOrder entity = this.findByOrderKey(orderKey);
        if (entity != null) {
            entity.setOrderStatus(orderStatus);
            entity.setEcsIfStatus(ecsIfStatus);
            this.queryManager.update(entity);
        }
    }

    /** 상태 + ECS 인터페이스 상태 동시 업데이트 */
    public TbWcsCraneOrder findLatestTodayOrderByTypeAndEcsIfStatus(WcsDomainEnums.OrderType orderType, WcsDomainEnums.EcsIfStatus ecsIfStatus) {

        // 오늘 날짜 yyyyMMdd 생성
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", OrmConstants.LIKE, today);
        condition.addFilter("order_type", orderType.code());
        condition.addFilter("ecs_if_status", ecsIfStatus.code());
        condition.addOrder("order_key", false);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbWcsCraneOrder.class, condition);
    }

    public List<TbWcsCraneOrder> findByEqGroupIdAndOrderType(String kMatTspg, String move) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", kMatTspg);
        condition.addFilter("order_type", WcsDomainEnums.OrderType.MOVE.code());
        condition.addOrder("order_key", false);
        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }

    /**
     * 오더와 아이템의 상태를 한 번에 업데이트 (트랜잭션 보장)
     *
     * @param orderKey    오더 키
     * @param orderStatus 변경할 오더/아이템 비즈니스 상태
     * @param ecsIfStatus 변경할 인터페이스 상태
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFullStatus(String orderKey, int orderStatus, int ecsIfStatus) {
        // 1. 헤더(TbWcsCraneOrder) 상태 업데이트
        TbWcsCraneOrder header = this.findByOrderKey(orderKey);
        if (header != null) {
            header.setOrderStatus(orderStatus);
            header.setEcsIfStatus(ecsIfStatus);
            // 상태 관련 컬럼만 명시적으로 업데이트 (성능 및 사이드 이펙트 방지)
            this.queryManager.update(header, "orderStatus", "ecsIfStatus");

            logger.info("[STATUS_UPDATE] Header Updated: orderKey={}, status={}, ecsStatus={}",
                    orderKey, orderStatus, ecsIfStatus);
        }

        // 2. 아이템(TbWcsCraneOrderItem) 전체 상태 업데이트
        // 직접 SQL을 실행하여 해당 orderKey를 가진 모든 아이템의 상태를 한 번에 변경합니다.
        String itemUpdateSql = "UPDATE tb_wcs_shuttle_order_item " +
                "SET line_status = :lineStatus " +
                "WHERE order_key = :orderKey";

        Map<String, Object> params = ValueUtil.newMap("lineStatus,orderKey", orderStatus, orderKey);
        int updatedItems = this.queryManager.executeBySql(itemUpdateSql, params);

        logger.info("[STATUS_UPDATE] Items Updated: orderKey={}, updatedCount={}", orderKey, updatedItems);
    }

    public List<TbWcsCraneOrder> findByStatusGreaterThanEqual(int status) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", OrmConstants.GREATER_THAN_EQUAL, status); // GE는 Greater than or Equal (>=)
        condition.addOrder("createdAt", true); // 오래된 에러부터 순차 처리
        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }

    /**
     * [고도화] 특정 구역 내 포트별 현재 활성 작업(Active Orders) 수를 집계한다.
     *
     * @param eqGroupId 설비 그룹 ID
     * @return Map<포트코드, 작업수>
     */
    public Map<String, Long> getActiveOrderCountByPort(String eqGroupId) {
        // 완료(90)되지 않은 모든 오더를 목적지(to_loc_code)별로 그룹핑하여 카운트
        String sql = "SELECT to_loc_code as port_code, count(*) as cnt " +
                "  FROM tb_wcs_shuttle_order " +
                " WHERE eq_group_id = :eqGroupId " +
                "   AND order_status < :completeStatus " +
                " GROUP BY to_loc_code";

        Map<String, Object> params = ValueUtil.newMap("eqGroupId,completeStatus",
                eqGroupId, WcsDomainEnums.ShuttleOrderStatus.COMPLETED.code());

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        // 결과 가공: List<Map> -> Map<String, Long>
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
     * [동시성 제어] 대기 출고 오더를 1건 선점하여 반환한다.
     *
     * 다중 WCS 인스턴스 환경에서 동일한 WAITING 오더를 두 서버가 동시에 가져가는
     * "이중 릴리즈" 버그를 방지하기 위해 Native Query 의 FOR UPDATE SKIP LOCKED 를 사용한다.
     *
     * [동작 원리]
     * - FOR UPDATE     : 선택된 행에 배타 락(Exclusive Row Lock) 을 획득
     * - SKIP LOCKED    : 이미 다른 트랜잭션이 락을 보유 중인 행은 대기하지 않고 건너뜀
     * → 두 서버가 동시에 호출해도 각자 다른 행(또는 null)을 가져감
     *
     * [주의]
     * - 반드시 활성 트랜잭션 내에서 호출해야 한다 (Propagation.REQUIRED).
     * 락은 트랜잭션 커밋/롤백 시 해제된다.
     * - 가져온 오더의 상태 변경(CREATED, ERROR 등)을 같은 트랜잭션에서 완료해야
     * 다른 서버가 재처리하지 않는다.
     *
     * @param eqGroupId 설비 그룹 ID
     * @return 선점된 WAITING 출고 오더 1건, 없으면 null
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public TbWcsCraneOrder findOldestWaitingOrderForUpdate(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return null;

        // priority ASC (높은 우선순위 먼저), created_at ASC (오래 기다린 것 먼저)
        String sql = "SELECT *" +
                "  FROM tb_wcs_shuttle_order" +
                " WHERE eq_group_id  = :eqGroupId" +
                "   AND order_type   = :orderType" +
                "   AND order_status = :waitingStatus" +
                " ORDER BY priority ASC, created_at ASC" +
                " LIMIT 1" +
                " FOR UPDATE SKIP LOCKED";

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,orderType,waitingStatus",
                eqGroupId,
                WcsDomainEnums.OrderType.OUTBOUND.code(),
                WcsDomainEnums.ShuttleOrderStatus.WAITING.codeAsIntOrNull()
        );

        List<TbWcsCraneOrder> list = this.queryManager.selectListBySql(
                sql, params, TbWcsCraneOrder.class, 0, 1);

        if (ValueUtil.isEmpty(list)) {
            logger.debug("[WaitQueue][SKIP LOCKED] 가용 WAITING 오더 없음 - eqGroupId={}", eqGroupId);
            return null;
        }

        logger.debug("[WaitQueue][SKIP LOCKED] 선점 성공 - orderKey={}", list.get(0).getOrderKey());
        return list.get(0);
    }

    /**
     * [고도화] 현재 입고 작업이 진행 중인 포트(출발지) 목록을 조회한다.
     * 입/출고 겸용 포트에서 출고를 피하기 위한 용도.
     */
    public Set<String> getActiveInboundPorts(String eqGroupId) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("order_type", WcsDomainEnums.OrderType.INBOUND.code());
        condition.addFilter("order_status", OrmConstants.GREATER_THAN_EQUAL, WcsDomainEnums.ShuttleOrderStatus.RUNNING.code());
        condition.addFilter("order_status", OrmConstants.LESS_THAN, WcsDomainEnums.ShuttleOrderStatus.COMPLETED.code());

        List<TbWcsCraneOrder> activeInbounds = this.queryManager.selectList(TbWcsCraneOrder.class, condition);

        // 입고의 경우 출발지(from_loc_code)가 포트임
        return activeInbounds.stream()
                .map(TbWcsCraneOrder::getFromLocId)
                .filter(ValueUtil::isNotEmpty)
                .collect(Collectors.toSet());
    }

    /**
     * [번들 원자성 보장] 다수의 오더와 아이템 상태를 일괄 업데이트 (트랜잭션 보장)
     * IN 절을 사용하여 한 번의 DB I/O로 번들 전체를 안전하게 Commit/Rollback 합니다.
     *
     * @param orderKeys   업데이트할 오더 키 목록
     * @param orderStatus 변경할 오더/아이템 비즈니스 상태
     * @param ecsIfStatus 변경할 인터페이스 상태
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFullStatusList(List<String> orderKeys, Integer orderStatus, Integer ecsIfStatus) {
        if (ValueUtil.isEmpty(orderKeys)) {
            logger.warn("[STATUS_UPDATE_LIST] 업데이트할 오더 키 목록이 비어 있습니다.");
            return;
        }

        try {
            // 1. 헤더(tb_wcs_shuttle_order) 일괄 업데이트
            String headerUpdateSql = "UPDATE tb_wcs_shuttle_order " +
                    "SET order_status = :orderStatus, " +
                    "    ecs_if_status = :ecsIfStatus " +
                    "WHERE order_key IN (:orderKeys)";

            Map<String, Object> headerParams = ValueUtil.newMap(
                    "orderStatus,ecsIfStatus,orderKeys",
                    orderStatus, ecsIfStatus, orderKeys
            );
            int updatedHeaders = this.queryManager.executeBySql(headerUpdateSql, headerParams);

            // 2. 아이템(tb_wcs_shuttle_order_item) 일괄 업데이트
            String itemUpdateSql = "UPDATE tb_wcs_shuttle_order_item " +
                    "SET line_status = :orderStatus " +
                    "WHERE order_key IN (:orderKeys)";

            Map<String, Object> itemParams = ValueUtil.newMap(
                    "orderStatus,orderKeys",
                    orderStatus, orderKeys
            );
            int updatedItems = this.queryManager.executeBySql(itemUpdateSql, itemParams);

            logger.info("[STATUS_UPDATE_LIST] Bundle Updated: keysCount={}, headers={}, items={}",
                    orderKeys.size(), updatedHeaders, updatedItems);

        } catch (Exception e) {
            logger.error("ERR_TRACE TbWcsCraneOrderService.updateFullStatusList orderKeys={} cause={}", orderKeys, e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외를 던짐
        }
    }

    /** groupOrderKey로 그룹 오더 전체 조회 (다중 파렛트 출고) */
    public List<TbWcsCraneOrder> findByGroupOrderKey(String groupOrderKey) {
        if (ValueUtil.isEmpty(groupOrderKey)) return java.util.Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("group_order_key", groupOrderKey);
        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }

    public List<TbWcsCraneOrder> findByHostOrderKey(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) return java.util.Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }

    public List<TbWcsCraneOrder> findByHostOrderKeyAndOrderType(String hostOrderKey, WcsDomainEnums.OrderType orderType) {
        if (ValueUtil.isEmpty(hostOrderKey)) return java.util.Collections.emptyList();
        if (ValueUtil.isEmpty(orderType)) return java.util.Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        condition.addFilter("order_type", orderType.code());
        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }

    // 시뮬레이터용 — operato.logis.wcs.tspg_4way_shuttle.simulator 에서 사용
    // 진행 중(종료/에러 미포함) 셔틀 오더 수를 집계한다. HOST 시뮬의 쓰로틀(MAX_PENDING) 판단에 쓴다.
    public long countPending(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return 0L;

        String sql = "SELECT COUNT(*) AS cnt FROM tb_wcs_shuttle_order " +
                " WHERE eq_group_id = :eqGroupId " +
                "   AND order_status < :completeStatus";

        Map<String, Object> params = ValueUtil.newMap("eqGroupId,completeStatus",
                eqGroupId, WcsDomainEnums.ShuttleOrderStatus.COMPLETED.code());

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return 0L;
        Object cnt = rows.get(0).get("cnt");
        return cnt instanceof Number ? ((Number) cnt).longValue() : 0L;
    }

    // 시뮬레이터용 — operato.logis.wcs.tspg_4way_shuttle.simulator 에서 사용
    // ECS 로 전송 완료(ecs_if_status = SENT) 상태인 오더를 batch 만큼 가져와 PLC 시뮬레이터가 콜백 시퀀스를 실행한다.
    public List<TbWcsCraneOrder> findSentOrders(String eqGroupId, int batchSize) {
        if (ValueUtil.isEmpty(eqGroupId)) return java.util.Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("ecs_if_status", WcsDomainEnums.EcsIfStatus.SENT.code());
        condition.addFilter("order_status", OrmConstants.LESS_THAN,
                WcsDomainEnums.ShuttleOrderStatus.COMPLETED.code());
        condition.addOrder("priority", true);
        condition.addOrder("createdAt", true);
        condition.setMaxResultSize(Math.max(1, batchSize));

        return this.queryManager.selectList(TbWcsCraneOrder.class, condition);
    }
}
