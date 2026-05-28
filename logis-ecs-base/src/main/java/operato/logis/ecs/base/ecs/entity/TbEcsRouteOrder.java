package operato.logis.ecs.base.ecs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.List;
import java.util.UUID;

// 검토중) 사용 확인
@Getter
@Setter
@Table(name = "tb_ecs_route_order", idStrategy = GenerationRule.UUID)
public class TbEcsRouteOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_key", length = 100) // 사용 / TbEcsRackOrder의 order_key
    private String orderKey;

    @Column(name = "order_type") // 사용
    private int orderType;

    @Column(name = "order_status") // 사용 - 컨베이어 작동 완료
    private int orderStatus;

    @Column(name = "priority")
    private int priority;

    @Column(name = "barcode", length = 100) // 사용 / TbEcsRackOrder의 barcode
    private String barcode;

    @Column(name = "eq_id", length = 50) // 사용 / 켄베이어ID 조회 시 사용
    private String eqId;

    @Column(name = "eq_type", length = 30)
    private String eqType;

    @Column(name = "cmd_status") // 사용 - 크레인 작업 상태 / EcsRouteOrderCmdStatus
    private int cmdStatus;

    @Column(name = "plc_cmd_id", length = 50)
    private String plcCmdId;

    @Column(name = "from_cv_id", length = 50)
    private String fromCvId;

    @Column(name = "to_cv_id", length = 50) // 사용
    private String toCvId;

    @Column(name = "wait_yn")
    private boolean waitYn;

    @Column(name = "error_id", length = 50)
    private String errorId;

    @Column(name = "error_desc", length = 500)
    private String errorDesc;

    /** 시간 컬럼은 문자열(ISO-8601 / OffsetDateTime 문자열) 기준 */
    @Column(name = "started_at", length = 30)
    private String startedAt;

    @Column(name = "finished_at", length = 30)
    private String finishedAt;

    private static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static TbEcsRouteOrder onlyOnePortOf(TbWcsCraneOrder order, String cvEqId, List<TbEqCvMst> eqCvMstList) {
        TbEcsRouteOrder routeOrder = new TbEcsRouteOrder();
        switch (EcsDBConsts.OrderType.find(order.getOrderType())) {
            case INBOUND -> {
                routeOrder = inboundRouteOrder(order, cvEqId, eqCvMstList);
            }
            case OUTBOUND -> {
                routeOrder = outboundRouteOrder(order, cvEqId, eqCvMstList);
            }
            case MOVE -> {
                routeOrder = moveRouteOrder(order, cvEqId, eqCvMstList);
            }
        }
        return routeOrder;
    }

    private static TbEcsRouteOrder moveRouteOrder(TbWcsCraneOrder order, String cvEqId, List<TbEqCvMst> eqCvMstList) {
        // 이동 출발지 층
        int fromLevel = Integer.parseInt(order.getFromLocId().substring(0, 1));
        // 이동 목적지 층
        int toLevel = Integer.parseInt(order.getToLocId().substring(0, 1));

        // 층이 같은 경우
        if (fromLevel == toLevel) {
            return null;
        }
        // 이동 출발지 랙단 컨베이어
        TbEqCvMst fromRackInCvMst = eqCvMstList.stream().filter(eqCvMst -> eqCvMst.getType() == EcsDBConsts.ConveyorType.INBOUND.getValue() // RACK_IN -> INBOUND
                && eqCvMst.getAsiel() == fromLevel).findFirst().orElse(null); // level -> asiel
        // 이동  목적지 랙단 컨베이어
        TbEqCvMst toRackInCvMst = eqCvMstList.stream().filter(eqCvMst -> eqCvMst.getType() == EcsDBConsts.ConveyorType.INBOUND.getValue() // RACK_IN -> INBOUND
                && eqCvMst.getAsiel() == toLevel).findFirst().orElse(null); // level -> asiel

        if (fromRackInCvMst == null && toRackInCvMst == null)
            return null;

        TbEcsRouteOrder moveOrder = new TbEcsRouteOrder();

        moveOrder.setId(generateId());
        moveOrder.setOrderKey(order.getOrderKey());
        moveOrder.setOrderType(EcsDBConsts.OrderType.MOVE.getValue());
        moveOrder.setPriority(order.getPriority());
        moveOrder.setBarcode(order.getBarcode());
        moveOrder.setEqId(cvEqId);
        moveOrder.setCmdStatus(0);
        moveOrder.setFromCvId(String.valueOf(fromRackInCvMst.getId()));
        moveOrder.setToCvId(String.valueOf(toRackInCvMst.getId()));
        return moveOrder;
    }

    private static TbEcsRouteOrder inboundRouteOrder(TbWcsCraneOrder order, String cvEqId, List<TbEqCvMst> eqCvMstList) {
        // 입고 목적지 층
        int toLevel = Integer.parseInt(order.getToLocId().substring(0, 1));
        TbEcsRouteOrder routeOrder = new TbEcsRouteOrder();

        // 입고 출발지 입출고대 컨베이어
        TbEqCvMst inboundCvMst = eqCvMstList.stream().filter(eqCvMst -> eqCvMst.getType() == EcsDBConsts.ConveyorType.IN_OUTBOUND.getValue()).findFirst().orElse(null);
        // 입고 목적지 랙단 컨베이어
        TbEqCvMst rackInCvMst = eqCvMstList.stream().filter(eqCvMst -> eqCvMst.getType() == EcsDBConsts.ConveyorType.INBOUND.getValue() // RACK_IN -> INBOUND
                && eqCvMst.getAsiel() == toLevel).findFirst().orElse(null); // level -> asiel

        if (inboundCvMst == null && rackInCvMst == null)
            return null;

        routeOrder.setId(generateId());
        routeOrder.setOrderKey(order.getOrderKey());
        routeOrder.setOrderType(EcsDBConsts.OrderType.INBOUND.getValue());
        routeOrder.setPriority(order.getPriority());
        routeOrder.setBarcode(order.getBarcode());
        routeOrder.setEqId(cvEqId);
        routeOrder.setCmdStatus(0);
        routeOrder.setFromCvId(String.valueOf(inboundCvMst.getId()));
        routeOrder.setToCvId(String.valueOf(rackInCvMst.getId()));
        return routeOrder;
    }

    private static TbEcsRouteOrder outboundRouteOrder(TbWcsCraneOrder order, String cvEqId, List<TbEqCvMst> eqCvMstList) {
        // 출고 출발지 층
        int fromLevel = Integer.parseInt(order.getFromLocId().substring(0, 1));
        TbEcsRouteOrder routeOrder = new TbEcsRouteOrder();
        // 출고 목적지 입출고대 컨베이어
        TbEqCvMst outboundCvMst = eqCvMstList.stream().filter(eqCvMst -> eqCvMst.getType() == EcsDBConsts.ConveyorType.IN_OUTBOUND.getValue()).findFirst().orElse(null);
        // 출고 출발지 랙단 컨베이어
        TbEqCvMst rackInCvMst = eqCvMstList.stream().filter(eqCvMst -> eqCvMst.getType() == EcsDBConsts.ConveyorType.INBOUND.getValue() // RACK_IN -> INBOUND
                && eqCvMst.getAsiel() == fromLevel).findFirst().orElse(null); // level -> asiel

        if (outboundCvMst == null && rackInCvMst == null)
            return null;

        routeOrder.setId(generateId());
        routeOrder.setOrderKey(order.getOrderKey());
        routeOrder.setOrderType(EcsDBConsts.OrderType.OUTBOUND.getValue());
        routeOrder.setPriority(order.getPriority());
        routeOrder.setBarcode(order.getBarcode());
        routeOrder.setEqId(cvEqId);
        routeOrder.setCmdStatus(0);
        routeOrder.setFromCvId(String.valueOf(rackInCvMst.getId()));
        routeOrder.setToCvId(String.valueOf(outboundCvMst.getId()));
        return routeOrder;
    }
}