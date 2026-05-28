package operato.logis.kmat_2026.biz.ecs.tspg4way.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.InternalEcsCallbackService;
import org.springframework.stereotype.Service;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEcsRouteOrder;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbWcsShuttleOrder;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TspgOrderCompleteService {
    private final IQueryManager iQueryManager;
    private final InternalEcsCallbackService internalEcsCallbackService;


    public void completeMoveRouteOrder(){
        List<TbWcsShuttleOrder> wcsShuttleOrders = selectPendingWcsShuttleOrdersForRoute();
        if(wcsShuttleOrders!= null && !wcsShuttleOrders.isEmpty()){
            wcsShuttleOrders.stream().forEach(wcsShuttleOrder -> {
                log.info("TspgOrderCompleteService completeMoveRouteOrder :{}", wcsShuttleOrder.getOrderKey());
                internalEcsCallbackService.conveyorArrived(wcsShuttleOrder.getOrderKey());
                wcsShuttleOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.RACK_IN_MOVE_COMPLETE.getValue());
                iQueryManager.update(wcsShuttleOrder);
            });
        }
    }


    public void completeOrder(){
        List<TbWcsShuttleOrder> wcsShuttleInboundOrders = selectPendingWcsShuttleOrdersForRackInbound();
        if(wcsShuttleInboundOrders!= null && !wcsShuttleInboundOrders.isEmpty()){
            wcsShuttleInboundOrders.stream().forEach(wcsShuttleOrder -> {
                log.info("TspgOrderCompleteService inbound order :{}", wcsShuttleOrder.getOrderKey());
                internalEcsCallbackService.complete(wcsShuttleOrder.getOrderKey());
                wcsShuttleOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.COMPLETE.getValue());
                iQueryManager.update(wcsShuttleOrder);
            });
        }

        List<TbWcsShuttleOrder> wcsShuttleOutboundOrders = selectPendingWcsShuttleOrdersForAgfOutbound();
        if(wcsShuttleOutboundOrders!= null && !wcsShuttleOutboundOrders.isEmpty()){
            wcsShuttleOutboundOrders.stream().forEach(wcsShuttleOrder -> {
                log.info("TspgOrderCompleteService outbound(agf cell) order :{}", wcsShuttleOrder.getOrderKey());
                internalEcsCallbackService.complete(wcsShuttleOrder.getOrderKey());
                wcsShuttleOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.COMPLETE.getValue());
                iQueryManager.update(wcsShuttleOrder);
            });
        }
    }

    // 랙간 이송지시 완료한 지시 조회
    private List<TbWcsShuttleOrder> selectPendingWcsShuttleOrdersForRoute(){
        int orderType = EcsDBConsts.OrderType.MOVE.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.RACK_IN_MOVE_COMPLETE.getValue();
        String sql = """
                select * 
                from tb_wcs_shuttle_order
                where ecs_if_status < :ecsIfStatus
                and order_key in (
                				 select order_key
                                from tb_ecs_route_order
                                where order_type = :orderType
                                and order_status = :completeStatus
                				)
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,orderType,completeStatus", ecsIfStatus, orderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);

    }

    // 입고 완료된 지시 조회 (wcs에서 재고이동 지시인경우도, ecs rack 입장에선 입고완료시 완료임)
    private List<TbWcsShuttleOrder> selectPendingWcsShuttleOrdersForRackInbound(){
        int inboundOrderType = EcsDBConsts.OrderType.INBOUND.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.COMPLETE.getValue();
        String sql = """
                select * from tb_wcs_shuttle_order
                where ecs_if_status < :ecsIfStatus
                and order_key in (
                				 select order_key
                                from tb_ecs_rack_order
                                where order_type = :inboundOrderType
                                and order_status = :completeStatus
                				)
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,inboundOrderType,completeStatus", ecsIfStatus, inboundOrderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);

    }

    // 출고 완료된 지시 조회 (입고로 이송하는 route order 지시 완료시 출고 완료로 봄)
    private List<TbWcsShuttleOrder> selectPendingWcsShuttleOrdersForRouteOutbound(){
        int outboundOrderType = EcsDBConsts.OrderType.OUTBOUND.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.COMPLETE.getValue();
        String sql = """
                select * from tb_wcs_shuttle_order
                where ecs_if_status < :ecsIfStatus
                and order_key in (
                				 select order_key
                                from tb_ecs_route_order
                                where order_type = :outboundOrderType
                                and order_status = :completeStatus
                				)
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,outboundOrderType,completeStatus", ecsIfStatus, outboundOrderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);
    }


    // 전시회 기준
    // 1층 출고단으로 완료된 지시 조회
    private List<TbWcsShuttleOrder> selectPendingWcsShuttleOrdersForAgfOutbound(){
        int outboundOrderType = EcsDBConsts.OrderType.OUTBOUND.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.COMPLETE.getValue();
        String sql = """
                select * from tb_wcs_shuttle_order
                where ecs_if_status < :ecsIfStatus
                and order_key in (
                				 select order_key
                                from tb_ecs_rack_order
                                where order_type = :outboundOrderType
                                and order_status = :completeStatus
                                and to_loc_code  in ('10601', '10602')
                				)
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,outboundOrderType,completeStatus", ecsIfStatus, outboundOrderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);
    }

}
