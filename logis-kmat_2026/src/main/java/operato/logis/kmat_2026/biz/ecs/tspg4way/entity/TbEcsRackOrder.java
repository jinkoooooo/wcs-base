package operato.logis.kmat_2026.biz.ecs.tspg4way.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
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
    private String fromLocCode;

    @Column(name = "from_row")
    private int fromRow;

    @Column(name = "from_bay")
    private int fromBay;

    @Column(name = "to_loc_code", length = 50)
    private String toLocCode;

    @Column(name = "to_row")
    private int toRow;

    @Column(name = "to_bay")
    private int toBay;

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

        int outboundLevel = Character.getNumericValue(order.getFromLocCode().charAt(0));
        int outboundFromRow = Character.getNumericValue(order.getFromLocCode().charAt(2));
        int outboundFromBay = Character.getNumericValue(order.getFromLocCode().charAt(4));
        int inboundLevel = Character.getNumericValue(order.getToLocCode().charAt(0));
        int inboundToRow = Character.getNumericValue(order.getToLocCode().charAt(2));
        int inboundToBay = Character.getNumericValue(order.getToLocCode().charAt(4));
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
            outboundOrder.setFromLocCode(order.getFromLocCode());
            outboundOrder.setFromRow(outboundFromRow);
            outboundOrder.setFromBay(outboundFromBay);
            outboundOrder.setToLocCode(order.getToLocCode());
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
        int outboundToRow = Character.getNumericValue(outboundInOutPortMst.getId().charAt(2));
        int outboundToBay = Character.getNumericValue(outboundInOutPortMst.getId().charAt(4));

        TbEqRackMst inboundInOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getLevel() == inboundLevel).findFirst().orElse(null);
        if(inboundInOutPortMst == null){
            return null;
        }
        int inboundFromRow = Character.getNumericValue(inboundInOutPortMst.getId().charAt(2));
        int inboundFromBay = Character.getNumericValue(inboundInOutPortMst.getId().charAt(4));

        TbEcsRackOrder outboundOrder = new TbEcsRackOrder();
        outboundOrder.setId(generateId());
        outboundOrder.setOrderKey(order.getOrderKey());
        outboundOrder.setOrderType(EcsDBConsts.OrderType.OUTBOUND.getValue());
        outboundOrder.setPriority(order.getPriority());
        outboundOrder.setBarcode(order.getBarcode());
        outboundOrder.setEqId(rackEqId);
        outboundOrder.setPlcCmdId(0);
        outboundOrder.setCmdStatus(0);
        outboundOrder.setFromLocCode(order.getFromLocCode());
        outboundOrder.setFromRow(outboundFromRow);
        outboundOrder.setFromBay(outboundFromBay);
        outboundOrder.setToLocCode(outboundInOutPortMst.getId());
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
        inboundOrder.setFromLocCode(outboundInOutPortMst.getId());
        inboundOrder.setFromRow(inboundFromRow);
        inboundOrder.setFromBay(inboundFromBay);
        inboundOrder.setToLocCode(order.getToLocCode());
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
        int fromLevel = Character.getNumericValue(order.getFromLocCode().charAt(0));
        int fromRow = Character.getNumericValue(order.getFromLocCode().charAt(2));
        int fromBay = Character.getNumericValue(order.getFromLocCode().charAt(4));
        int toLevel = Character.getNumericValue(order.getToLocCode().charAt(0));
        int toRow = Character.getNumericValue(order.getToLocCode().charAt(2));
        int toBay = Character.getNumericValue(order.getToLocCode().charAt(4));

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
                int inboundFromRow = Character.getNumericValue(inOutPortMst.getId().charAt(2));
                int inboundFromBay = Character.getNumericValue(inOutPortMst.getId().charAt(4));
                ecsOrder.setFromLocCode(inOutPortMst.getId());
                ecsOrder.setFromRow(inboundFromRow);
                ecsOrder.setFromBay(inboundFromBay);
                ecsOrder.setToLocCode(order.getToLocCode());
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
                int outboundToRow = Character.getNumericValue(inOutPortMst.getId().charAt(2));
                int outboundToBay = Character.getNumericValue(inOutPortMst.getId().charAt(4));
                ecsOrder.setFromLocCode(order.getFromLocCode());
                ecsOrder.setFromRow(fromRow);
                ecsOrder.setFromBay(fromBay);
                ecsOrder.setToLocCode(inOutPortMst.getId());
                ecsOrder.setToRow(outboundToRow);
                ecsOrder.setToBay(outboundToBay);
                ecsOrder.setLevel(fromLevel);
            }
        }
        return ecsOrder;
    }


    public static TbEcsRackOrder chargeOrder(String chargePortCell, String chargeEntPort, String rackEqId, String carEqId){
        int fromRow = Character.getNumericValue(chargeEntPort.charAt(2));
        int fromBay = Character.getNumericValue(chargeEntPort.charAt(4));
        int fromLevel = Character.getNumericValue(chargeEntPort.charAt(0));
        int toRow = Character.getNumericValue(chargePortCell.charAt(2));
        int toBay = Character.getNumericValue(chargePortCell.charAt(4));

        TbEcsRackOrder ecsOrder = new TbEcsRackOrder();
        ecsOrder.setId(generateId());
        ecsOrder.setOrderType(EcsDBConsts.OrderType.CHARGE.getValue());
        ecsOrder.setOrderStatus(EcsDBConsts.OrderStatus.READY.getValue());
        ecsOrder.setPriority(1);
        ecsOrder.setEqId(rackEqId);
        ecsOrder.setEqCarId(carEqId);
        ecsOrder.setCmdStatus(EcsDBConsts.EcsRackOrderCmdStatus.READY.getValue());
        ecsOrder.setFromLocCode(chargeEntPort);
        ecsOrder.setFromRow(fromRow);
        ecsOrder.setFromBay(fromBay);
        ecsOrder.setToLocCode(chargePortCell);
        ecsOrder.setToRow(toRow);
        ecsOrder.setToBay(toBay);
        ecsOrder.setLevel(fromLevel);
        return ecsOrder;
    }


    public static TbEcsRackOrder test(EcsDBConsts.OrderType orderType , String fromCellId, String toCellId){
        int fromLevel = Character.getNumericValue(fromCellId.charAt(0));
        int fromRow = Character.getNumericValue(fromCellId.charAt(2));
        int fromBay = Character.getNumericValue(fromCellId.charAt(4));
        int toLevel = Character.getNumericValue(toCellId.charAt(0));
        int toRow = Character.getNumericValue(toCellId.charAt(2));
        int toBay = Character.getNumericValue(toCellId.charAt(4));


       TbEcsRackOrder order = new TbEcsRackOrder();
        order.setId(generateId());
        order.setOrderKey("testOrderKey");
        order.setOrderType(orderType.getValue());
        order.setPriority(0);
        order.setBarcode("testBarcode");
        order.setEqId("testEqId");
        order.setPlcCmdId(0);
        order.setCmdStatus(0);
        order.setFromLocCode(fromCellId);
        order.setFromRow(fromRow);
        order.setFromBay(fromBay);
        order.setToLocCode(toCellId);
        order.setToRow(toRow);
        order.setToBay(toBay);
        order.setLevel(fromLevel);
        return order;
    }

}
