package operato.logis.ecs.base.ecs.service.common;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEcsRackOrder;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TbEcsRackOrderService extends AbstractQueryService {

    /** 검토완료) 완료되지 않은 현재 랙의 보관설비 지시를 조회 */
    public List<TbEcsRackOrder> selectTbEcsRackOrder(StackerCraneContext context) {
        String eqId = context.getRackEqId();
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String sql = """
                SELECT * 
                FROM tb_ecs_rack_order
                WHERE order_status <> :status
                    AND eq_id = :eqId
                    AND asiel IN (:asiel1, :asiel2);
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId,asiel1,asiel2", status, eqId, context.getAsiel1(), context.getAsiel2());
        return queryManager.selectListBySql(sql, params, TbEcsRackOrder.class, 0, 0);
    }

    public TbEcsRackOrder selectTbEcsRackWorkingOrder(StackerCraneContext context, String craneId) {
        int status = EcsDBConsts.OrderStatus.WORKING.getValue();
        String eqId = context.getRackEqId();
        String sql = """
                SELECT * 
                FROM tb_ecs_rack_order
                WHERE order_status = :status
                    AND eq_id = :eqId
                    AND asiel IN (:asiel1, :asiel2)
                    AND eq_car_id = :craneId;
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId,asiel1,asiel2,craneId", status, eqId, context.getAsiel1(), context.getAsiel2(), craneId);
        return queryManager.selectBySql(sql, params, TbEcsRackOrder.class);
    }

    public List<TbEcsRackOrder> selectTbEcsRackInboundOrder(StackerCraneContext context) {
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int orderType = EcsDBConsts.OrderType.INBOUND.getValue();
        String eqId = context.getRackEqId();
        String sql = """
                SELECT * 
                FROM tb_ecs_rack_order
                WHERE order_status <> :status
                    AND order_type = :orderType
                    AND eq_id = :eqId
                    AND asiel IN (:asiel1, :asiel2);
                """;
        Map<String, Object> params = ValueUtil.newMap("status,orderType,eqId,asiel1,asiel2", status, orderType, eqId, context.getAsiel1(), context.getAsiel2());
        return queryManager.selectListBySql(sql, params, TbEcsRackOrder.class, 0, 0);
    }

    public TbEcsRackOrder selectTbEcsOrderOtherCarWorking(StackerCraneContext context, String craneEqId) {
        int status = EcsDBConsts.OrderStatus.WORKING.getValue();
        String eqId = context.getRackEqId();
        String sql = """
                SELECT * 
                FROM tb_ecs_rack_order
                WHERE order_status = :status
                    AND eq_id = :eqId
                    AND eq_crane_id <> :craneEqId;
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId,craneEqId", status, eqId, craneEqId);
        return queryManager.selectBySql(sql, params, TbEcsRackOrder.class);
    }

    /** 검토완료) 작업상태 변경 */
    public void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.OrderStatus orderStatus) {
        rackOrder.setOrderStatus(orderStatus.getValue());
        queryManager.update(rackOrder);
        log.info("update rack Order Status: " + rackOrder.getOrderKey() + ", " + orderStatus.getDescription());
    }

    public void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.OrderStatus orderStatus, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setOrderStatus(orderStatus.getValue());
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        log.info("update rack Order Status" + "[" + plcCmdId + "] : " + rackOrder.getOrderKey() + ", " + orderStatus.getDescription() + ", " + cmdStatus.getDescription());

        queryManager.update(rackOrder);
    }

    /** 검토완료) 랙 작업 상태 및 설비 수정 */
    public void updateTbEcsRackOrder(String craneEqId, TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setEqCraneId(craneEqId);
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        queryManager.update(rackOrder);
        log.info("update rack Order Status" + "[" + plcCmdId + "] : " + rackOrder.getOrderKey() + ", " + cmdStatus.getDescription());
    }

    public void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        queryManager.update(rackOrder);
        log.info("update rack Order Status" + "[" + plcCmdId + "] : " + rackOrder.getOrderKey() + ", " + cmdStatus.getDescription());
    }

    public void updateTbEcsRackOrder(String craneEqId, TbEcsRackOrder rackOrder, EcsDBConsts.OrderStatus orderStatus, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setOrderStatus(orderStatus.getValue());
        rackOrder.setEqCraneId(craneEqId);
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        log.info("update rack Order Status" + "[" + craneEqId + "," + plcCmdId + "] : " + rackOrder.getOrderKey() + ", " + orderStatus.getDescription() + ", " + cmdStatus.getDescription());

        queryManager.update(rackOrder);
    }

    public void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, int plcCmdId) {
        rackOrder.setPlcCmdId(plcCmdId);

        queryManager.update(rackOrder);
    }

    public void avoidMoveComplete(TbEcsRackOrder originOrder) {
        originOrder.setWaitYn(false);
        queryManager.update(originOrder);
    }
}