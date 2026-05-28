package operato.logis.ecs.base.ecs.service.common;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEcsRouteOrder;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

// 검토완료
@Service
@Slf4j
public class TbEcsRouteOrderService extends AbstractQueryService {

    /** 입출고대 컨베이어 상태 업데이트 */
    public void updateTbEcsRouteOrder(StackerCraneContext context, String orderKey, EcsDBConsts.EcsRouteOrderCmdStatus ecsRouteOrderCmdStatus) {
        String eqId = context.getCvEqId();
        int status = ecsRouteOrderCmdStatus.getValue();
        String sql = """
                SELECT * 
                FROM tb_ecs_route_order
                WHERE order_key = :orderKey
                    AND eq_id = :eqId
                    AND cmd_status <> :status;
                """;
        Map<String, Object> params = ValueUtil.newMap("orderKey,eqId,status", orderKey, eqId, status);
        TbEcsRouteOrder routeOrder = queryManager.selectBySql(sql, params, TbEcsRouteOrder.class);
        if (routeOrder == null)
            return;

        routeOrder.setCmdStatus(status);
        queryManager.update(routeOrder);
        log.info("update route Order Status: " + orderKey + ", " + ecsRouteOrderCmdStatus.getDescription());
    }

    // 이송지시를 조회
    public TbEcsRouteOrder selectTbEcsRouteOrder(String cvEqId, String orderKey) {
        String sql = """
                SELECT * 
                FROM tb_ecs_route_order
                WHERE order_key = :orderKey
                    AND eq_id = :eqId;
                """;
        Map<String, Object> params = ValueUtil.newMap("orderKey,eqId", orderKey, cvEqId);
        return queryManager.selectBySql(sql, params, TbEcsRouteOrder.class);
    }

    /** 현재 랙에 연결된 컨베이어에서 입출고 대기 중인 작업 조회 */
    public List<TbEcsRouteOrder> selectTbEcsRouteOrder(StackerCraneContext context) {
        String eqId = context.getCvEqId();
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int cmdStatus = EcsDBConsts.EcsRouteOrderCmdStatus.STATION_READY.getValue();
        String sql = """
                SELECT * 
                FROM tb_ecs_route_order
                WHERE order_status = :status
                    AND cmd_status = :cmdStatus
                    AND eq_id = :eqId
                    AND to_cv_id in (
                        SELECT id
                        FROM tb_eq_cv_mst
                        WHERE asiel IN (:asiel1, :asiel2);
                    );
                """;
        Map<String, Object> params = ValueUtil.newMap("status,cmdStatus,eqId,asiel1,asiel2", status, cmdStatus, eqId, context.getAsiel1(), context.getAsiel2());
        return queryManager.selectListBySql(sql, params, TbEcsRouteOrder.class, 0, 0);
    }

    /** 현재랙에 연결된 입출고대 컨베이어 진행중인 지시 조회 */
    public List<TbEcsRouteOrder> selectTbEcsRouteReserveOrder(StackerCraneContext context, int cvId) {
        int orderType = EcsDBConsts.OrderType.INBOUND.getValue();
        int statusEqSend = EcsDBConsts.OrderStatus.EQ_SEND.getValue();
        int statusWorking = EcsDBConsts.OrderStatus.WORKING.getValue();
        String eqCvId = context.getCvEqId();
        String rackInCvId = String.valueOf(cvId);
        String sql = """
                SELECT * 
                FROM tb_ecs_route_order
                WHERE order_type = :orderType
                    AND order_status in (:statusEqSend, :statusWorking)
                    AND eq_id = :eqCvId
                    AND to_cv_id = :rackInCvId
                """;
        Map<String, Object> params = ValueUtil.newMap("orderType,statusEqSend,statusWorking,eqCvId,rackInCvId", orderType, statusEqSend, statusWorking, eqCvId, rackInCvId);
        return queryManager.selectListBySql(sql, params, TbEcsRouteOrder.class, 0, 0);
    }
}