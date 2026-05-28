package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsIfStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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

/**
 * TbWcsShuttleOrder Entity Service (DB 전용)
 */
@Service
public class TbWcsShuttleOrderService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbWcsShuttleOrderService.class);

    /**
     * ID로 조회
     */
    public TbWcsShuttleOrder findById(String id) {
        return this.queryManager.select(TbWcsShuttleOrder.class, id);
    }

    /**
     * orderKey로 조회
     */
    public TbWcsShuttleOrder findByOrderKey(String orderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", orderKey);
        return this.queryManager.selectByCondition(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 상태별 조회
     */
    public List<TbWcsShuttleOrder> findByStatus(int orderStatus) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", orderStatus);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * eqGroupId별 조회
     */
    public List<TbWcsShuttleOrder> findByEqGroupId(String eqGroupId) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 신규 저장
     */
    public TbWcsShuttleOrder insert(TbWcsShuttleOrder entity) {
        logger.debug("Inserting TbWcsShuttleOrder: orderKey={}, orderType={}",
                entity.getOrderKey(), entity.getOrderType());
        this.queryManager.insert(entity);
        return entity;
    }

    /**
     * 수정
     */
    public TbWcsShuttleOrder update(TbWcsShuttleOrder entity) {
        logger.info("Updating TbWcsShuttleOrder: orderKey={}, orderStatus={}",
                entity.getOrderKey(), entity.getOrderStatus());
        this.queryManager.update(entity);
        return entity;
    }

    /**
     * 수정
     */
    @Transactional
    public TbWcsShuttleOrder update(TbWcsShuttleOrder entity, String... columns) {
        logger.info("Updating TbWcsShuttleOrder: orderKey={}, orderStatus={}",
                entity.getOrderKey(), entity.getOrderStatus());
        this.queryManager.update(entity, columns);
        return entity;
    }

    /**
     * 상태 업데이트
     */
    public void updateStatus(String orderKey, int orderStatus) {
        TbWcsShuttleOrder entity = this.findByOrderKey(orderKey);
        if (entity != null) {
            entity.setOrderStatus(orderStatus);
            this.queryManager.update(entity);
        }
    }

    /**
     * ECS 인터페이스 상태 업데이트
     */
    public void updateEcsIfStatus(String orderKey, int ecsIfStatus) {
        TbWcsShuttleOrder entity = this.findByOrderKey(orderKey);
        if (entity != null) {
            entity.setEcsIfStatus(ecsIfStatus);
            this.queryManager.update(entity);
        }
    }

    /**
     * 상태 + ECS 인터페이스 상태 동시 업데이트
     */
    public void updateStatusAndEcsIfStatus(String orderKey, int orderStatus, int ecsIfStatus) {
        TbWcsShuttleOrder entity = this.findByOrderKey(orderKey);
        if (entity != null) {
            entity.setOrderStatus(orderStatus);
            entity.setEcsIfStatus(ecsIfStatus);
            this.queryManager.update(entity);
        }
    }

    /**
     * 상태 + ECS 인터페이스 상태 동시 업데이트
     */
    public TbWcsShuttleOrder findLatestTodayOrderByTypeAndEcsIfStatus(OrderTypeEnumCode orderType, EcsIfStatusEnumCode ecsIfStatus) {

        // 오늘 날짜 yyyyMMdd 생성
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key",OrmConstants.LIKE,today);
        condition.addFilter("order_type", orderType.code());
        condition.addFilter("ecs_if_status", ecsIfStatus.code());
        condition.addOrder("order_key", false);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbWcsShuttleOrder.class, condition);
    }

    public List<TbWcsShuttleOrder> findByEqGroupIdAndOrderType(String kMatTspg, String move) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id",kMatTspg);
        condition.addFilter("order_type", OrderTypeEnumCode.MOVE.code());
        condition.addOrder("order_key", false);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 오더와 아이템의 상태를 한 번에 업데이트 (트랜잭션 보장)
     * @param orderKey 오더 키
     * @param orderStatus 변경할 오더/아이템 비즈니스 상태
     * @param ecsIfStatus 변경할 인터페이스 상태
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFullStatus(String orderKey, int orderStatus, int ecsIfStatus) {
        // 1. 헤더(TbWcsShuttleOrder) 상태 업데이트
        TbWcsShuttleOrder header = this.findByOrderKey(orderKey);
        if (header != null) {
            header.setOrderStatus(orderStatus);
            header.setEcsIfStatus(ecsIfStatus);
            // 상태 관련 컬럼만 명시적으로 업데이트 (성능 및 사이드 이펙트 방지)
            this.queryManager.update(header, "orderStatus", "ecsIfStatus");

            logger.info("[STATUS_UPDATE] Header Updated: orderKey={}, status={}, ecsStatus={}",
                    orderKey, orderStatus, ecsIfStatus);
        }

        // 2. 아이템(TbWcsShuttleOrderItem) 전체 상태 업데이트
        // 직접 SQL을 실행하여 해당 orderKey를 가진 모든 아이템의 상태를 한 번에 변경합니다.
        String itemUpdateSql = "UPDATE tb_wcs_shuttle_order_item " +
                "SET line_status = :lineStatus " +
                "WHERE order_key = :orderKey";

        Map<String, Object> params = ValueUtil.newMap("lineStatus,orderKey", orderStatus, orderKey);
        int updatedItems = this.queryManager.executeBySql(itemUpdateSql, params);

        logger.info("[STATUS_UPDATE] Items Updated: orderKey={}, updatedCount={}", orderKey, updatedItems);
    }

    public List<TbWcsShuttleOrder> findByStatusGreaterThanEqual(int status) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", OrmConstants.GREATER_THAN_EQUAL, status); // GE는 Greater than or Equal (>=)
        condition.addOrder("createdAt", true); // 오래된 에러부터 순차 처리
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * [고도화] 특정 구역 내 포트별 현재 활성 작업(Active Orders) 수를 집계한다.
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
                eqGroupId, ShuttleOrderStatusEnumCode.COMPLETED.code());

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
     * [고도화] 현재 입고 작업이 진행 중인 포트(출발지) 목록을 조회한다.
     * 입/출고 겸용 포트에서 출고를 피하기 위한 용도.
     */
    public Set<String> getActiveInboundPorts(String eqGroupId) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("order_type", OrderTypeEnumCode.INBOUND.code());
        condition.addFilter("order_status", OrmConstants.LESS_THAN, ShuttleOrderStatusEnumCode.COMPLETED.code());

        List<TbWcsShuttleOrder> activeInbounds = this.queryManager.selectList(TbWcsShuttleOrder.class, condition);

        // 입고의 경우 출발지(from_loc_code)가 포트임
        return activeInbounds.stream()
                .map(TbWcsShuttleOrder::getFromLocCode)
                .filter(ValueUtil::isNotEmpty)
                .collect(Collectors.toSet());
    }
}
