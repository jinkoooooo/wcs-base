package operato.logis.ecs.base.ecs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 검토중) 사용 확인
@Getter
@Setter
@Table(name = "tb_ecs_rack_order", idStrategy = GenerationRule.UUID)
public class TbEcsRackOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_key", length = 100) // 사용 / TbEcsRouteOrder의 order_key
    private String orderKey;

    @Column(name = "order_type")
    private int orderType;

    @Column(name = "order_status") // 사용 / OrderStatus
    private int orderStatus;

    @Column(name = "priority") // 사용
    private int priority;

    @Column(name = "barcode", length = 100) // 사용 / TbEcsRouteOrder의 barcode
    private String barcode;

    @Column(name = "eq_id", length = 50) // 사용
    private String eqId;

    @Column(name = "eq_crane_id", length = 50) // todo: shuttle car용으로 확인. crane에서 미사용 확인
    private String eqCraneId;

    @Column(name = "plc_cmd_id") // 사용
    private int plcCmdId;

    @Column(name = "cmd_status") // 사용 / EcsRackOrderCmdStatus
    private int cmdStatus;

    @Column(name = "asiel") // todo: level 변경
    private int asiel;

    @Column(name = "from_loc_code", length = 50)
    // 사용 / TbEqRackMst의 rack_id / todo: Baylevel 구조 확인 / MovexCraneOrderService.isLoadTarget()
    private String fromLocId;

    @Column(name = "from_asiel") // todo: from_row 변경
    private int fromAsiel;

    @Column(name = "from_bay")
    private int fromBay;

    @Column(name = "from_level")
    private int fromlevel;

    @Column(name = "to_loc_code", length = 50) // 사용 / TbEqRackMst의 rack_id
    private String toLocId;

    @Column(name = "to_asiel")
    private int toAsiel;

    @Column(name = "to_bay")
    private int toBay;

    @Column(name = "to_level")
    private int tolevel;

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

    private static String generateId() { return UUID.randomUUID().toString(); }

    /** 입출고 포트가 한개인 경우의 설비 지시 생성 */
    public static List<TbEcsRackOrder> onlyOnePortOf(TbWcsCraneOrder order, String rackEqId, List<TbEqRackMst> eqRackMstList) {
        List<TbEcsRackOrder> rackOrderList = new ArrayList<>();
        TbEcsRackOrder rackOrder;
        EcsDBConsts.OrderType orderType = EcsDBConsts.OrderType.valueOf(order.getOrderType());
        switch (orderType) {
            case INBOUND, OUTBOUND -> {
                rackOrder = inOutboundRackOrder(order, rackEqId, eqRackMstList);
                if (rackOrder != null)
                    rackOrderList.add(rackOrder);
            }
            case MOVE -> rackOrderList = moveRackOrder(order, rackEqId, eqRackMstList);
        }
        return rackOrderList;
    }

    private static List<TbEcsRackOrder> moveRackOrder(TbWcsCraneOrder order, String rackEqId, List<TbEqRackMst> eqRackMstList) {
        List<TbEqRackMst> eqRackInutPortMstList = eqRackMstList.stream().filter(eqRackMst -> eqRackMst.getType() == EcsDBConsts.RackType.IN_OUTBOUND_PORT.getValue()).toList();
        List<TbEcsRackOrder> rackOrderList = new ArrayList<>();

        if (eqRackInutPortMstList.isEmpty()) {
            return null;
        }

        int outboundlevel = Character.getNumericValue(order.getFromLocId().charAt(0));
        int outboundFromRow = Integer.parseInt(order.getFromLocId().substring(1, 3));
        int outboundFromBay = Integer.parseInt(order.getFromLocId().substring(3, 5));
        int inboundlevel = Character.getNumericValue(order.getToLocId().charAt(0));
        int inboundToRow = Integer.parseInt(order.getToLocId().substring(1, 3));
        int inboundToBay = Integer.parseInt(order.getToLocId().substring(3, 5));
        // 같은 층에서의 이동인 경우
        if (outboundlevel == inboundlevel) {
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
            outboundOrder.setFromBay(outboundFromBay);
            outboundOrder.setFromlevel(outboundFromRow); // todo: setFromRow -> setFromlevel
            outboundOrder.setToLocId(order.getToLocId());
            outboundOrder.setAsiel(outboundlevel); // todo: sstlevel -> setAsiel
            outboundOrder.setToBay(inboundToBay);
            outboundOrder.setTolevel(inboundToRow); // todo : setToRow -> setTolevel
            rackOrderList.add(outboundOrder);

            return rackOrderList;
        }
        //todo: 기존 로직 검토. TbEqRackMst outboundInOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getlevel() == outboundlevel).findFirst().orElse(null);
        TbEqRackMst outboundInOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getAsiel() == outboundlevel).findFirst().orElse(null);
        if (outboundInOutPortMst == null) {
            return null;
        }
        int outboundToRow = Integer.parseInt(outboundInOutPortMst.getRackId().substring(1, 3));
        int outboundToBay = Integer.parseInt(outboundInOutPortMst.getRackId().substring(3, 5));

        // todo: 기존 로직 검토. TbEqRackMst inboundInOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getlevel() == inboundlevel).findFirst().orElse(null);
        TbEqRackMst inboundInOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getAsiel() == inboundlevel).findFirst().orElse(null);
        if (inboundInOutPortMst == null) {
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
        //outboundOrder.setFromRow(outboundFromRow); // todo: setFromlevel
        outboundOrder.setFromBay(outboundFromBay);
        outboundOrder.setToLocId(outboundInOutPortMst.getRackId());
        outboundOrder.setToBay(outboundToBay);
        //outboundOrder.setToRow(outboundToRow); // todo: setTolevel
        outboundOrder.setAsiel(outboundlevel); // todo: setlevel -> setAsiel

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
        //inboundOrder.setFromRow(inboundFromRow); // todo: setFromlevel
        inboundOrder.setFromBay(inboundFromBay);
        inboundOrder.setToLocId(order.getToLocId());
        //inboundOrder.setToRow(inboundToRow); // todo: setTolevel
        inboundOrder.setToBay(inboundToBay);
        //inboundOrder.setlevel(inboundlevel); // todo: setAsiel

        rackOrderList.add(inboundOrder);
        rackOrderList.add(outboundOrder);

        return rackOrderList;
    }

    private static TbEcsRackOrder inOutboundRackOrder(TbWcsCraneOrder order, String rackEqId, List<TbEqRackMst> eqRackMstList) {

        List<TbEqRackMst> eqRackInutPortMstList = eqRackMstList.stream().filter(eqRackMst -> eqRackMst.getType() == EcsDBConsts.RackType.IN_OUTBOUND_PORT.getValue()).toList();
        if (eqRackInutPortMstList.isEmpty()) {
            return null;
        }
        int fromlevel = Character.getNumericValue(order.getFromLocId().charAt(0));
        int fromAsiel = Integer.parseInt(order.getFromLocId().substring(1, 3));
        int fromBay = Integer.parseInt(order.getFromLocId().substring(3, 5));
        int tolevel = Character.getNumericValue(order.getToLocId().charAt(0));
        int toAsiel = Integer.parseInt(order.getToLocId().substring(1, 3));
        int toBay = Integer.parseInt(order.getToLocId().substring(3, 5));

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
        switch (ecsOrderType) {
            case INBOUND -> {
                // 입고 목적지 층의 입출고포트 정보를 가져옴. 해당 포트가 FROM 이 됨.
                // todo: 기존 로직 검토. TbEqRackMst inOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getlevel() == tolevel).findFirst().orElse(null);
                TbEqRackMst inOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getAsiel() == tolevel).findFirst().orElse(null);
                if (inOutPortMst == null) {
                    return null;
                }
                int inboundFromRow = Integer.parseInt(inOutPortMst.getRackId().substring(1, 3));
                int inboundFromBay = Integer.parseInt(inOutPortMst.getRackId().substring(3, 5));

                ecsOrder.setFromLocId(inOutPortMst.getRackId());
                //ecsOrder.setFromRow(inboundFromRow); // todo: setFromlevel
                ecsOrder.setFromBay(inboundFromBay);
                ecsOrder.setToLocId(order.getToLocId());
                //ecsOrder.setToRow(toAsiel); // todo: setTolevel
                ecsOrder.setToBay(toBay);
                //ecsOrder.setlevel(tolevel); // todo: setAsiel
            }
            case OUTBOUND -> {
                // 출고 출발지 층의 입출고포트 정보를 가져옴. 해당 포트가 TO가 됨
                // todo: 기존 로직 검토. TbEqRackMst inOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getlevel() == fromlevel).findFirst().orElse(null);
                TbEqRackMst inOutPortMst = eqRackInutPortMstList.stream().filter(eqRackMst -> eqRackMst.getAsiel() == fromlevel).findFirst().orElse(null);
                if (inOutPortMst == null) {
                    return null;
                }
                int outboundToRow = Integer.parseInt(inOutPortMst.getRackId().substring(1, 3));
                int outboundToBay = Integer.parseInt(inOutPortMst.getRackId().substring(3, 5));
                ecsOrder.setFromLocId(order.getFromLocId());
                //ecsOrder.setFromRow(fromAsiel);
                ecsOrder.setFromBay(fromBay);
                ecsOrder.setToLocId(inOutPortMst.getRackId());
                //ecsOrder.setToRow(outboundToRow);
                ecsOrder.setToBay(outboundToBay);
                //ecsOrder.setlevel(fromlevel);
            }
        }
        return ecsOrder;
    }

    public static TbEcsRackOrder craneHomeMoveOrder(String toLocId, int level, String rackEqId, String craneEqId) {
        int toAsiel = Integer.parseInt(toLocId.substring(1, 3));
        int toBay = Integer.parseInt(toLocId.substring(3, 5));

        TbEcsRackOrder ecsOrder = new TbEcsRackOrder();
        ecsOrder.setId(generateId());
        ecsOrder.setOrderType(EcsDBConsts.OrderType.MOVE_HOME.getValue());
        ecsOrder.setOrderStatus(EcsDBConsts.OrderStatus.READY.getValue());
        ecsOrder.setPriority(EcsDBConsts.OrderPriority.MOVE_HOME.getValue());
        ecsOrder.setEqId(rackEqId);
        //ecsOrder.setEqCarId(craneEqId);
        ecsOrder.setCmdStatus(EcsDBConsts.EcsRackOrderCmdStatus.READY.getValue());
        ecsOrder.setFromLocId("");
        //ecsOrder.setFromRow(0);
        ecsOrder.setFromBay(0);
        ecsOrder.setToLocId(toLocId);
        //ecsOrder.setToRow(toAsiel);
        ecsOrder.setToBay(toBay);
        //ecsOrder.setlevel(level);
        return ecsOrder;
    }

    public static TbEcsRackOrder otherCraneMoveOrder(int toAsiel, int toBay, int level, String rackEqId, String craneEqId) {
        TbEcsRackOrder ecsOrder = new TbEcsRackOrder();
        ecsOrder.setId(generateId());
        ecsOrder.setOrderType(EcsDBConsts.OrderType.MOVE_HOME.getValue());
        ecsOrder.setOrderStatus(EcsDBConsts.OrderStatus.READY.getValue());
        ecsOrder.setPriority(EcsDBConsts.OrderPriority.MOVE_HOME.getValue());
        ecsOrder.setEqId(rackEqId);
        //ecsOrder.setEqCarId(craneEqId);
        ecsOrder.setCmdStatus(EcsDBConsts.EcsRackOrderCmdStatus.READY.getValue());
        ecsOrder.setFromLocId("");
        //ecsOrder.setFromRow(0);
        ecsOrder.setFromBay(0);
        if (String.valueOf(toAsiel).length() > 1) {
            if (String.valueOf(toBay).length() > 1) {
                ecsOrder.setToLocId("" + level + toAsiel + toBay);
            } else {
                ecsOrder.setToLocId("" + level + toAsiel + "0" + toBay);
            }
        } else {
            if (String.valueOf(toBay).length() > 1) {
                ecsOrder.setToLocId("" + level + "0" + toAsiel + toBay);
            } else {
                ecsOrder.setToLocId("" + level + "0" + toAsiel + "0" + toBay);
            }
        }
        //ecsOrder.setToRow(toAsiel);
        ecsOrder.setToBay(toBay);
        //ecsOrder.setlevel(level);
        return ecsOrder;
    }
}
