package operato.logis.ecs.base.ecs.service.conveyor;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.ConveyorWriteConsts;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.entity.TbEcsRouteOrder;
import operato.logis.ecs.base.ecs.entity.TbEqCvMst;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ConveyorOrderService {

    private IQueryManager iQueryManager = BeanUtil.get(IQueryManager.class);
    private ConveyorPlcWriteService cvPlcWriteService;
    private ConveyorPlcManager cvPlcManager;
    private ConveyorPlc cvPlc;
    private String cvEqId;
    private int workId = 1001;

    private int nextWorkId() {
        workId++;
        if (workId > 9999) {
            workId = 1001;
        }
        return workId;
    }

    public ConveyorOrderService(ConveyorPlcWriteService cvPlcWriteService, ConveyorPlcManager cvPlcManager, String cvEqId) {
        this.cvPlcWriteService = cvPlcWriteService;
        this.cvPlcManager = cvPlcManager;
        this.cvEqId = cvEqId;
        this.cvPlc = this.cvPlcManager.getEquipment(this.cvEqId);
    }

    public void work() {
        ConveyorOrderManager();
    }

    private void ConveyorOrderManager() {
        // TODO : 셔틀카 단이동중인경우 대기하는 로직 추가
        List<TbEcsRouteOrder> orderList = selectTbEcsRouteOrder();
        if (orderList != null && orderList.isEmpty()) {
            // log.info("[ConveyorOrderManager] orderList.isEmpty()");
            return;
        }
        Map<Integer, List<TbEcsRouteOrder>> orderMap = orderList.stream().collect(Collectors.groupingBy(TbEcsRouteOrder::getOrderStatus));
        List<TbEcsRouteOrder> eqSendOrderList = orderMap.get(EcsDBConsts.OrderStatus.EQ_SEND.getValue());
        List<TbEcsRouteOrder> workingOrderList = orderMap.get(EcsDBConsts.OrderStatus.WORKING.getValue());
        List<TbEcsRouteOrder> readyOrderList = orderMap.get(EcsDBConsts.OrderStatus.READY.getValue());
        if (eqSendOrderList != null && !eqSendOrderList.isEmpty()) {
            // log.info("[ConveyorOrderManager] eqSendOrderManager : " + eqSendOrderList.size());
            eqSendOrderManager(eqSendOrderList);
            return;
        }
        if (workingOrderList != null && !workingOrderList.isEmpty()) {
            // log.info("[ConveyorOrderManager] workingOrderManager : " + workingOrderList.size());
            workingOrderManager(workingOrderList);
            return;
        }
        if (readyOrderList != null && !readyOrderList.isEmpty()) {
            // log.info("[ConveyorOrderManager] readyOrderManager : " + readyOrderList.size());
            Map<Integer, List<TbEcsRouteOrder>> readyOrderMap = readyOrderList.stream().collect(Collectors.groupingBy(TbEcsRouteOrder::getCmdStatus));
            List<TbEcsRouteOrder> outboundOrderList = readyOrderMap.get(EcsDBConsts.EcsRouteOrderCmdStatus.READY.getValue()); //TODO: 검토 / 기존 RACK_CV_READY.getValue());
            List<TbEcsRouteOrder> inboundOrderList = readyOrderMap.get(EcsDBConsts.EcsRouteOrderCmdStatus.INBOUND_READY.getValue());

            /**
             * 전시회 기준
             * 출고지시 랙단 대기인게 있으면, 무조건 재고이동일 것이니 출고지시 운선실행
             * 출고지시가 없으면 그다음에 입고 지시 실행
             */
            if (outboundOrderList != null && !outboundOrderList.isEmpty()) {
                // log.info("[ConveyorOrderManager] outboundOrderManager : " + outboundOrderList.size());
                outboundOrderManager(outboundOrderList.get(0));
                return;
            }

            if (inboundOrderList != null && !inboundOrderList.isEmpty()) {
                // log.info("[ConveyorOrderManager] inboundOrderManager : " + inboundOrderList.size());
                inboundOrderManager(inboundOrderList.get(0));
            }
        }
    }

    /**
     * 설비 전송된 지시 관리
     */
    private void eqSendOrderManager(List<TbEcsRouteOrder> eqSendOrderList) {
        Map<Integer, List<TbEcsRouteOrder>> eqSendOrderMap = eqSendOrderList.stream().collect(Collectors.groupingBy(TbEcsRouteOrder::getOrderType));
        List<TbEcsRouteOrder> liftMoveOrderList = eqSendOrderMap.get(EcsDBConsts.OrderType.INBOUND.getValue());
        List<TbEcsRouteOrder> levelMoveOrderList = eqSendOrderMap.get(EcsDBConsts.OrderType.MOVE.getValue());
        List<TbEcsRouteOrder> rackCvOrderList = eqSendOrderMap.get(EcsDBConsts.OrderType.OUTBOUND.getValue());
        // 입고 지시인 경우 리프트컨베이어에 화물이 감지되면 지시 작업중
        if (liftMoveOrderList != null && !liftMoveOrderList.isEmpty()) {
            liftMoveOrderList.forEach(order -> {
                TbEqCvMst liftCvMst = selectTbEqCv(EcsDBConsts.ConveyorType.LIFT);
                if (liftCvMst != null && liftCvMst.isCargoYn())
                    updateTbEcsRouteOrder(order, EcsDBConsts.OrderStatus.WORKING);
            });
        } else if (levelMoveOrderList != null && !levelMoveOrderList.isEmpty()) {
            levelMoveOrderList.forEach(order -> {
                TbEqCvMst liftCvMst = selectTbEqCv(EcsDBConsts.ConveyorType.LIFT);
                if (liftCvMst != null && liftCvMst.isCargoYn())
                    updateTbEcsRouteOrder(order, EcsDBConsts.OrderStatus.WORKING);
            });
        }
        // 출고 지시인 경우 지시 출발지 랙단컨베이어 단과 리프트 컨베이어 단이 일치하면 지시 작업중
        else if (rackCvOrderList != null && !rackCvOrderList.isEmpty()) {
            rackCvOrderList.forEach(order -> {
                TbEqCvMst liftCvMst = selectTbEqCv(EcsDBConsts.ConveyorType.LIFT);
                TbEqCvMst rackCvMst = selectTbEqCv(String.valueOf(order.getFromCvId()));
                if (liftCvMst != null && rackCvMst != null && liftCvMst.getAsiel() == rackCvMst.getAsiel())
                    //todo: 기존 로직 비교 검토 / if (liftCvMst != null && rackCvMst != null && liftCvMst.getLevel() == rackCvMst.getLevel())
                    updateTbEcsRouteOrder(order, EcsDBConsts.OrderStatus.WORKING);
            });
        }
    }

    /**
     * 진행중인 지시 관리
     */
    private void workingOrderManager(List<TbEcsRouteOrder> workingOrderList) {
        // 입출고 지시인 경우 목적지 컨베이어(입고-랙단컨베이어, 출고-출고대)에 화물이 감지되면 지시 완료
        if (!workingOrderList.isEmpty()) {
            workingOrderList.forEach(order -> {
                TbEqCvMst targetCvMst = selectTbEqCv(order.getToCvId());
                if (targetCvMst != null && targetCvMst.isCargoYn() && !targetCvMst.isRunYn())
                    if (order.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue()) {
                        updateTbEcsRouteOrder(order, EcsDBConsts.OrderStatus.COMPLETE, EcsDBConsts.EcsRouteOrderCmdStatus.READY); // todo: 기존 로직 검토 / 기존 .RACK_CV_READY);
                        // 랙단컨베이어 화물 예약 대기 업데이트
                        updateTbEqCvMst(targetCvMst, EcsDBConsts.EqConveyorStatus.READY);
                    } else if (order.getOrderType() == EcsDBConsts.OrderType.OUTBOUND.getValue())
                        updateTbEcsRouteOrder(order, EcsDBConsts.OrderStatus.COMPLETE, EcsDBConsts.EcsRouteOrderCmdStatus.COMPLETE);
                    else if (order.getOrderType() == EcsDBConsts.OrderType.MOVE.getValue())
                        updateTbEcsRouteOrder(order, EcsDBConsts.OrderStatus.COMPLETE, EcsDBConsts.EcsRouteOrderCmdStatus.READY); // todo: 기존 로직 검토 / 기존 .RACK_CV_READY);
            });
        }
    }

    /**
     * 출고 지시 관리
     */
    private void outboundOrderManager(TbEcsRouteOrder outboundOrder) {
        TbEqCvMst rackCv = selectTbEqCv(outboundOrder.getFromCvId());
        TbEqCvMst liftCv = selectTbEqCv(EcsDBConsts.ConveyorType.LIFT);

        // 출고 랙단컨베이어에 화물이 있고, 리프트 현재 화물이 없는 경우 지시
        if (rackCv != null && liftCv != null
                && rackCv.isCargoYn() && !liftCv.isCargoYn()) {
            int[] command = new int[]{};
            // 재고이동인 경우 설비 지시할 때 재고이동 송신 모드 비트 살림
            if (outboundOrder.getOrderType() == EcsDBConsts.OrderType.MOVE.getValue()) {
                sendPlcLiftMoveCommand();
                command = cvPlc.getWirteMap().getMoveCommand(this.nextWorkId(), Integer.valueOf(outboundOrder.getFromCvId()), Integer.parseInt(outboundOrder.getToCvId()));

            } else {
                sendPlcMoveCommand();
                command = cvPlc.getWirteMap().getOutboundCommand(this.nextWorkId(), Integer.valueOf(outboundOrder.getFromCvId()), Integer.parseInt(outboundOrder.getToCvId()));
            }
            sendPlcCommand(Integer.parseInt(rackCv.getId()), command);
            updateTbEcsRouteOrder(outboundOrder, this.workId, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRouteOrderCmdStatus.LIFT_MOVE);
        }
    }

    /**
     * 입고 지시 관리
     */
    private void inboundOrderManager(TbEcsRouteOrder inboundOrder) {
        TbEqCvMst rackCv = selectTbEqCv(inboundOrder.getToCvId());
        TbEqCvMst inboundCv = selectTbEqCv(EcsDBConsts.ConveyorType.IN_OUTBOUND);

        // 입고 도착지 랙단컨베이어화물이 없고, 리프트에 현재 화물이 있는 경우 지시
        if (rackCv != null && inboundCv != null
                && !rackCv.isCargoYn() && inboundCv.isCargoYn()) {
            log.info("inboundOrderManager sendPlcMoveCommand");
            sendPlcMoveCommand();
            int[] command = cvPlc.getWirteMap().getOutboundCommand(this.nextWorkId(), Integer.parseInt(inboundOrder.getFromCvId()), Integer.parseInt(inboundOrder.getToCvId()));
            sendPlcCommand(Integer.parseInt(inboundCv.getId()), command);
            updateTbEcsRouteOrder(inboundOrder, this.workId, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRouteOrderCmdStatus.LIFT_MOVE);
            // 랙단컨베이어 화물 예약 업데이트
            updateTbEqCvMst(rackCv, EcsDBConsts.EqConveyorStatus.MOVE_RESERVE);
        }
    }

    /**
     * 컨베이어 PLC 명령 송신
     */
    private void sendPlcCommand(int conveyorId, int[] command) {
        log.info("[PLC SEND] plc command for conveyorId: " + conveyorId);
        int firstDeviceCode = cvPlc.getWriteFirstDeviceCode(conveyorId);
        cvPlcWriteService.sendCommandConveyor(this.cvEqId, MelsecConsts.DeviceCode.R, firstDeviceCode, command);
    }

    /**
     * 컨베이어 이동 명령 송신모드 설정
     */
    private void sendPlcMoveCommand() {
        log.info("[PLC SEND] plc command for conveyorId: normal move");
        int[] command = { 0, 0 };
        int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
        cvPlcWriteService.sendCommandConveyor(this.cvEqId, MelsecConsts.DeviceCode.R, firstDeviceCode, command);
    }

    /**
     * 컨베이어 재고 단이동 명령 송신모드 설정
     */
    private void sendPlcLiftMoveCommand() {
        int modeWord = 1 << ConveyorWriteConsts.ConveyorLiftMoveMode.CARGO_MOVE.getBitIndex();
        log.info("[PLC SEND] plc command for conveyorId: level move" + modeWord);
        int[] command = { modeWord, 0 };
        int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
        cvPlcWriteService.sendCommandConveyor(this.cvEqId, MelsecConsts.DeviceCode.R, firstDeviceCode, command);
    }

    /** 컨베이어 셔틀카 단이동 명령 송신모드 설정 */
    private void sendPlcLiftCarMoveCommand(int toLevel) {
        int modeWord = 1 << ConveyorWriteConsts.ConveyorLiftMoveMode.SHUTTLE_CAR_MOVE.getBitIndex();
        int[] command = { modeWord, toLevel };
        int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
        cvPlcWriteService.sendCommandConveyor(this.cvEqId, MelsecConsts.DeviceCode.R, firstDeviceCode, command);
    }

    private List<TbEcsRouteOrder> selectTbEcsRouteOrder() {
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String eqId = this.cvEqId;
        String sql = """
                select * from tb_ecs_route_order
                where order_status <> :status
                and eq_id = :eqId
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId", status, eqId);
        return iQueryManager.selectListBySql(sql, params, TbEcsRouteOrder.class, 0, 0);
    }

    private TbEqCvMst selectTbEqCv(EcsDBConsts.ConveyorType conveyorType) {
        int cvType = conveyorType.getValue();
        String eqId = this.cvEqId;
        String sql = """
                select * from tb_eq_cv_mst
                where type = :cvType
                and eq_id = :eqId
                """;
        Map<String, Object> params = ValueUtil.newMap("cvType,eqId", cvType, eqId);
        return iQueryManager.selectBySql(sql, params, TbEqCvMst.class);
    }

    private TbEqCvMst selectTbEqCv(String conveyorId) {
        String eqId = this.cvEqId;
        String sql = """
                select * from tb_eq_cv_mst
                where eq_id = :eqId
                and id = :conveyorId
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId,conveyorId", eqId, conveyorId);
        return iQueryManager.selectBySql(sql, params, TbEqCvMst.class);
    }

    private void updateTbEcsRouteOrder(TbEcsRouteOrder order, EcsDBConsts.OrderStatus orderStatus) {
        order.setOrderStatus(orderStatus.getValue());
        log.info("update route Order Status " + order.getOrderKey() + ", " + orderStatus.getDescription());
        this.iQueryManager.update(order);
    }

    private void updateTbEcsRouteOrder(TbEcsRouteOrder order, EcsDBConsts.OrderStatus orderStatus, EcsDBConsts.EcsRouteOrderCmdStatus cmdStatus) {
        order.setOrderStatus(orderStatus.getValue());
        order.setCmdStatus(cmdStatus.getValue());
        log.info("update route Order Status " + order.getOrderKey() + ", " + orderStatus.getDescription() + ", " + cmdStatus.getDescription());
        this.iQueryManager.update(order);
    }

    private void updateTbEcsRouteOrder(TbEcsRouteOrder order, int plcCmdId, EcsDBConsts.OrderStatus orderStatus, EcsDBConsts.EcsRouteOrderCmdStatus cmdStatus) {
        order.setPlcCmdId(String.valueOf(plcCmdId));
        order.setOrderStatus(orderStatus.getValue());
        order.setCmdStatus(cmdStatus.getValue());
        log.info("update route Order Status" + "[" + plcCmdId + "] : " + order.getOrderKey() + ", " + orderStatus.getDescription() + ", " + cmdStatus.getDescription());
        this.iQueryManager.update(order);
    }

    private void updateTbEqCvMst(TbEqCvMst cvMst, EcsDBConsts.EqConveyorStatus eqConveyorStatus) {
        cvMst.setStatus(eqConveyorStatus.getValue());
        iQueryManager.update(cvMst);
    }
}
