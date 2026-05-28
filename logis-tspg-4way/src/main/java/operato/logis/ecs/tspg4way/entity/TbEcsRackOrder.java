package operato.logis.ecs.tspg4way.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Table(name = "tb_ecs_rack_order", idStrategy = GenerationRule.UUID)
public class TbEcsRackOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_key", length = 100)
    private String orderKey;

    @Column(name = "order_type")
    private int orderType;

    @Column(name = "order_status")
    private int orderStatus;

    @Column(name = "priority")
    private int priority;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "eq_id", length = 50)
    private String eqId;

    @Column(name = "eq_car_id", length = 50)
    private String eqCarId;

    @Column(name = "plc_cmd_id")
    private int plcCmdId;

    @Column(name = "cmd_status")
    private int cmdStatus;
    @Column(name = "level")
    private int level;

    @Column(name = "from_loc_code", length = 50)
    private String fromLocId;

    @Column(name = "from_row")
    private int fromRow;

    @Column(name = "from_bay")
    private int fromBay;

    @Column(name = "to_loc_code", length = 50)
    private String toLocId;

    @Column(name = "to_row")
    private int toRow;

    @Column(name = "to_bay")
    private int toBay;

    @Column(name = "wait_yn")
    private boolean waitYn;

    @Column(name = "error_id", length = 50)
    private String errorId;

    @Column(name = "error_desc", length = 500)
    private String errorDesc;

    // 날짜/시간 문자열(ISO/OffsetDateTime 문자열)을 저장한다고 가정
    @Column(name = "started_at", length = 30)
    private String startedAt;

    @Column(name = "finished_at", length = 30)
    private String finishedAt;

    private static String generateId(){
        return UUID.randomUUID().toString();
    }

    /**
     * 입출고 포트가 한개인 경우의 설비 지시 생성
     */
    public static List<TbEcsRackOrder> onlyOnePortOf(TbWcsShuttleOrder order, String rackEqId, List<TbEqRackMst> eqRackMstList){
        List<TbEcsRackOrder> rackOrderList = new ArrayList<>();
        TbEcsRackOrder rackOrder;
        EcsDBConsts.OrderType orderType = EcsDBConsts.OrderType.valueOf(order.getOrderType());
        switch (orderType){
            case INBOUND,OUTBOUND -> {
                rackOrder = inOutboundRackOrder(order, rackEqId, eqRackMstList);
                if(rackOrder != null)
                    rackOrderList.add(rackOrder);
            }
            case MOVE -> rackOrderList = moveRackOrder(order, rackEqId, eqRackMstList);
        }
        return rackOrderList;
    }



    private static List<TbEcsRackOrder> moveRackOrder(TbWcsShuttleOrder order, String rackEqId, List<TbEqRackMst> eqRackMstList) {
        List<TbEqRackMst> eqRackInutPortMstList = eqRackMstList.stream().filter(eqRackMst -> eqRackMst.getType() == EcsDBConsts.RackType.IN_OUTBOUND_PORT.getValue()).toList();
        List<TbEcsRackOrder> rackOrderList = new ArrayList<>();

        if(eqRackInutPortMstList.isEmpty()){
            return null;
        }

        int outboundLevel = Character.getNumericValue(order.getFromLocId().charAt(0));
        int outboundFromRow = Integer.parseInt(order.getFromLocId().substring(1, 3));
        int outboundFromBay =  Integer.parseInt(order.getFromLocId().substring(3, 5));
        int inboundLevel = Character.getNumericValue(order.getToLocId().charAt(0));
        int inboundToRow = Integer.parseInt(order.getToLocId().substring(1, 3));
        int inboundToBay = Integer.parseInt(order.getToLocId().substring(3, 5));
        // 같은 층에서의 이동인 경우
        if(outboundLevel == inboundLevel){
            TbEcsRackOrder outboundOrder = new TbEcsRackOrder();
            outboundOrder.setId(generateId());
            outboundOrder.setOrderKey(order.getOrderKey());
            outboundOrder.setOrderType(EcsDBConsts.OrderType.OUTBOUND.getValue());
            outboundOrder.setPriority(order.getPriority());
            outboundOrder.setBarcode(order.getBarcode());
            outboundOrder.setEqId(rackEqId);
            outboundOrder.setPlcCmdId(0);
            outboundOrder.setCmdStatus(0);
            outboundOrder.setFromLocId(order.getFromLocId());
            outboundOrder.setFromRow(outboundFromRow);
            outboundOrder.setFromBay(outboundFromBay);
            outboundOrder.setToLocId(order.getToLocId());
            outboundOrder.setToRow(inboundToRow);
            outboundOrder.setToBay(inboundToBay);
            outboundOrder.setLevel(outboundLevel);
            rackOrderList.add(outboundOrder);

            return rackOrderList;
        }
        TbEqRackMst outboundInOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getLevel() == outboundLevel).findFirst().orElse(null);
        if(outboundInOutPortMst == null){
            return null;
        }
        int outboundToRow = Integer.parseInt(outboundInOutPortMst.getRackId().substring(1, 3));
        int outboundToBay = Integer.parseInt(outboundInOutPortMst.getRackId().substring(3, 5));

        TbEqRackMst inboundInOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getLevel() == inboundLevel).findFirst().orElse(null);
        if(inboundInOutPortMst == null){
            return null;
        }
        int inboundFromRow = Integer.parseInt(inboundInOutPortMst.getRackId().substring(1, 3));
        int inboundFromBay = Integer.parseInt(inboundInOutPortMst.getRackId().substring(3, 5));

        TbEcsRackOrder outboundOrder = new TbEcsRackOrder();
        outboundOrder.setId(generateId());
        outboundOrder.setOrderKey(order.getOrderKey());
        outboundOrder.setOrderType(EcsDBConsts.OrderType.OUTBOUND.getValue());
        outboundOrder.setPriority(order.getPriority());
        outboundOrder.setBarcode(order.getBarcode());
        outboundOrder.setEqId(rackEqId);
        outboundOrder.setPlcCmdId(0);
        outboundOrder.setCmdStatus(0);
        outboundOrder.setFromLocId(order.getFromLocId());
        outboundOrder.setFromRow(outboundFromRow);
        outboundOrder.setFromBay(outboundFromBay);
        outboundOrder.setToLocId(outboundInOutPortMst.getRackId());
        outboundOrder.setToRow(outboundToRow);
        outboundOrder.setToBay(outboundToBay);
        outboundOrder.setLevel(outboundLevel);

        TbEcsRackOrder inboundOrder = new TbEcsRackOrder();
        inboundOrder.setId(generateId());
        inboundOrder.setOrderKey(order.getOrderKey());
        inboundOrder.setOrderType(EcsDBConsts.OrderType.INBOUND.getValue());
        inboundOrder.setPriority(order.getPriority());
        inboundOrder.setBarcode(order.getBarcode());
        inboundOrder.setEqId(rackEqId);
        inboundOrder.setPlcCmdId(0);
        inboundOrder.setCmdStatus(0);
        inboundOrder.setFromLocId(outboundInOutPortMst.getRackId());
        inboundOrder.setFromRow(inboundFromRow);
        inboundOrder.setFromBay(inboundFromBay);
        inboundOrder.setToLocId(order.getToLocId());
        inboundOrder.setToRow(inboundToRow);
        inboundOrder.setToBay(inboundToBay);
        inboundOrder.setLevel(inboundLevel);

        rackOrderList.add(inboundOrder);
        rackOrderList.add(outboundOrder);

        return rackOrderList;
    }

    private static TbEcsRackOrder inOutboundRackOrder(TbWcsShuttleOrder order, String rackEqId, List<TbEqRackMst> eqRackMstList) {

        List<TbEqRackMst> eqRackInutPortMstList = eqRackMstList.stream().filter(eqRackMst -> eqRackMst.getType() == EcsDBConsts.RackType.IN_OUTBOUND_PORT.getValue()).toList();
        if(eqRackInutPortMstList.isEmpty()){
            return null;
        }
        int fromLevel = Character.getNumericValue(order.getFromLocId().charAt(0));
        int fromRow = Integer.parseInt(order.getFromLocId().substring(1, 3));
        int fromBay =  Integer.parseInt(order.getFromLocId().substring(3, 5));
        int toLevel = Character.getNumericValue(order.getToLocId().charAt(0));
        int toRow = Integer.parseInt(order.getToLocId().substring(1, 3));
        int toBay =  Integer.parseInt(order.getToLocId().substring(3, 5));

        EcsDBConsts.OrderType ecsOrderType = EcsDBConsts.OrderType.find(order.getOrderType());

        TbEcsRackOrder ecsOrder = new TbEcsRackOrder();
        ecsOrder.setId(generateId());
        ecsOrder.setOrderKey(order.getOrderKey());
        ecsOrder.setOrderType(ecsOrderType.getValue());
        ecsOrder.setPriority(order.getPriority());
        ecsOrder.setBarcode(order.getBarcode());
        ecsOrder.setEqId(rackEqId);
        ecsOrder.setPlcCmdId(0);
        ecsOrder.setCmdStatus(0);
        switch (ecsOrderType){
            case INBOUND -> {
                // 입고 목적지 층의 입출고포트 정보를 가져옴. 해당 포트가 FROM 이 됨.
                TbEqRackMst inOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getLevel() == toLevel).findFirst().orElse(null);
                if(inOutPortMst == null){
                    return null;
                }
                int inboundFromRow = Integer.parseInt(inOutPortMst.getRackId().substring(1, 3));
                int inboundFromBay =  Integer.parseInt(inOutPortMst.getRackId().substring(3, 5));

                ecsOrder.setFromLocId(inOutPortMst.getRackId());
                ecsOrder.setFromRow(inboundFromRow);
                ecsOrder.setFromBay(inboundFromBay);
                ecsOrder.setToLocId(order.getToLocId());
                ecsOrder.setToRow(toRow);
                ecsOrder.setToBay(toBay);
                ecsOrder.setLevel(toLevel);
            }
            case OUTBOUND -> {
                // 출고 출발지 층의 입출고포트 정보를 가져옴. 해당 포트가 TO가 됨
                TbEqRackMst inOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getLevel() == fromLevel).findFirst().orElse(null);
                if(inOutPortMst == null){
                    return null;
                }
                int outboundToRow = Integer.parseInt(inOutPortMst.getRackId().substring(1, 3));
                int outboundToBay =  Integer.parseInt(inOutPortMst.getRackId().substring(3, 5));
                ecsOrder.setFromLocId(order.getFromLocId());
                ecsOrder.setFromRow(fromRow);
                ecsOrder.setFromBay(fromBay);
                ecsOrder.setToLocId(inOutPortMst.getRackId());
                ecsOrder.setToRow(outboundToRow);
                ecsOrder.setToBay(outboundToBay);
                ecsOrder.setLevel(fromLevel);
            }
        }
        return ecsOrder;
    }


    public static TbEcsRackOrder chargeOrder(String chargePortCell, String chargeEntPort, String rackEqId, String carEqId){
        int fromLevel = Character.getNumericValue(chargeEntPort.charAt(0));
        int fromRow = Integer.parseInt(chargeEntPort.substring(1, 3));
        int fromBay =  Integer.parseInt(chargeEntPort.substring(3, 5));
        int toRow = Integer.parseInt(chargePortCell.substring(1, 3));
        int toBay =  Integer.parseInt(chargePortCell.substring(3, 5));

        TbEcsRackOrder ecsOrder = new TbEcsRackOrder();
        ecsOrder.setId(generateId());
        ecsOrder.setOrderType(EcsDBConsts.OrderType.CHARGE.getValue());
        ecsOrder.setOrderStatus(EcsDBConsts.OrderStatus.READY.getValue());
        ecsOrder.setPriority(1);
        ecsOrder.setEqId(rackEqId);
        ecsOrder.setEqCarId(carEqId);
        ecsOrder.setCmdStatus(EcsDBConsts.EcsRackOrderCmdStatus.READY.getValue());
        ecsOrder.setFromLocId(chargeEntPort);
        ecsOrder.setFromRow(fromRow);
        ecsOrder.setFromBay(fromBay);
        ecsOrder.setToLocId(chargePortCell);
        ecsOrder.setToRow(toRow);
        ecsOrder.setToBay(toBay);
        ecsOrder.setLevel(fromLevel);
        return ecsOrder;
    }


    public static TbEcsRackOrder carHomeMoveOrder(String toLocId, int Level, String rackEqId, String carEqId){
        int toRow = Integer.parseInt(toLocId.substring(1, 3));
        int toBay =  Integer.parseInt(toLocId.substring(3, 5));

        TbEcsRackOrder ecsOrder = new TbEcsRackOrder();
        ecsOrder.setId(generateId());
        ecsOrder.setOrderType(EcsDBConsts.OrderType.MOVE_HOME.getValue());
        ecsOrder.setOrderStatus(EcsDBConsts.OrderStatus.READY.getValue());
        ecsOrder.setPriority(EcsDBConsts.OrderPriority.MOVE_HOME.getValue());
        ecsOrder.setEqId(rackEqId);
        ecsOrder.setEqCarId(carEqId);
        ecsOrder.setCmdStatus(EcsDBConsts.EcsRackOrderCmdStatus.READY.getValue());
        ecsOrder.setFromLocId("");
        ecsOrder.setFromRow(0);
        ecsOrder.setFromBay(0);
        ecsOrder.setToLocId(toLocId);
        ecsOrder.setToRow(toRow);
        ecsOrder.setToBay(toBay);
        ecsOrder.setLevel(Level);
        return ecsOrder;
    }

    public static TbEcsRackOrder otherCarMoveOrder(int toRow, int toBay, int Level, String rackEqId, String carEqId){
        TbEcsRackOrder ecsOrder = new TbEcsRackOrder();
        ecsOrder.setId(generateId());
        ecsOrder.setOrderType(EcsDBConsts.OrderType.MOVE_HOME.getValue());
        ecsOrder.setOrderStatus(EcsDBConsts.OrderStatus.READY.getValue());
        ecsOrder.setPriority(EcsDBConsts.OrderPriority.MOVE_HOME.getValue());
        ecsOrder.setEqId(rackEqId);
        ecsOrder.setEqCarId(carEqId);
        ecsOrder.setCmdStatus(EcsDBConsts.EcsRackOrderCmdStatus.READY.getValue());
        ecsOrder.setFromLocId("");
        ecsOrder.setFromRow(0);
        ecsOrder.setFromBay(0);
        if(String.valueOf(toRow).length() > 1){
            if(String.valueOf(toBay).length() > 1){
                ecsOrder.setToLocId(""+Level+toRow+toBay);
            }else{
                ecsOrder.setToLocId(""+Level+toRow+"0"+toBay);
            }
        }else{
            if(String.valueOf(toBay).length() > 1){
                ecsOrder.setToLocId(""+Level+"0"+toRow+toBay);
            }else{
                ecsOrder.setToLocId(""+Level+"0"+toRow+"0"+toBay);
            }
        }
        ecsOrder.setToRow(toRow);
        ecsOrder.setToBay(toBay);
        ecsOrder.setLevel(Level);
        return ecsOrder;
    }

}
