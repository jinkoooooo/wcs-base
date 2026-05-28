package operato.logis.ecs.base.ecs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbWcsCraneOrder;
import org.springframework.stereotype.Service;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
// TODO : 상위 보고 내용, 상위 시스템에 따라서 수정
public class StackerCraneOrderCompleteService {

    private final IQueryManager iQueryManager;

    public void completeMoveRouteOrder() {
        List<TbWcsCraneOrder> wcsCraneOrders = selectPendingWcsCraneOrdersForRoute();
        if (wcsCraneOrders != null && !wcsCraneOrders.isEmpty()) {
            wcsCraneOrders.stream().forEach(wcsCraneOrder -> {
                log.info("MovexOrderCompleteService completeMoveRouteOrder :{}", wcsCraneOrder.getOrderKey());
                wcsCraneOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.RACK_IN_MOVE_COMPLETE.getValue());
                iQueryManager.update(wcsCraneOrder);
            });
        }
    }

    public void completeOrder() {
        List<TbWcsCraneOrder> wcsCraneInboundOrders = selectPendingWcsCraneOrdersForRackInbound();
        if (wcsCraneInboundOrders != null && !wcsCraneInboundOrders.isEmpty()) {
            wcsCraneInboundOrders.stream().forEach(wcsCraneOrder -> {
                log.info("MovexOrderCompleteService inbound order :{}", wcsCraneOrder.getOrderKey());
                // internalEcsCallbackService.complete(wcsCraneOrder.getOrderKey());
                wcsCraneOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.COMPLETE.getValue());
                iQueryManager.update(wcsCraneOrder);
            });
        }

        List<TbWcsCraneOrder> wcsCraneOutboundOrders = selectPendingWcsCraneOrdersForRouteOutbound();
        if (wcsCraneOutboundOrders != null && !wcsCraneOutboundOrders.isEmpty()) {
            wcsCraneOutboundOrders.stream().forEach(wcsCraneOrder -> {
                log.info("MovexOrderCompleteService outbound(agf cell) order :{}", wcsCraneOrder.getOrderKey());
                // internalEcsCallbackService.complete(wcsCraneOrder.getOrderKey());
                wcsCraneOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.COMPLETE.getValue());
                iQueryManager.update(wcsCraneOrder);
            });
        }

        List<TbWcsCraneOrder> wcsCraneMoveOrders = selectPendingWcsCraneOrdersForMove();
        if (wcsCraneMoveOrders != null && !wcsCraneMoveOrders.isEmpty()) {
            wcsCraneMoveOrders.stream().forEach(wcsCraneOrder -> {
                log.info("MovexOrderCompleteService outbound(agf cell) order :{}", wcsCraneOrder.getOrderKey());
                // internalEcsCallbackService.complete(wcsCraneOrder.getOrderKey());
                wcsCraneOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.COMPLETE.getValue());
                iQueryManager.update(wcsCraneOrder);
            });
        }
    }

    // 랙간 이송지시 완료한 지시 조회
    private List<TbWcsCraneOrder> selectPendingWcsCraneOrdersForRoute() {
        int orderType = EcsDBConsts.OrderType.MOVE.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.RACK_IN_MOVE_COMPLETE.getValue();
        String sql = """
                SELECT *  
                FROM tb_wcs_crane_order 
                WHERE ecs_if_status < :ecsIfStatus 
                    AND order_key in (
                        SELECT order_key 
                        FROM tb_ecs_route_order
                        WHERE order_type = :orderType 
                            AND order_status = :completeStatus
                        );
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,orderType,completeStatus", ecsIfStatus, orderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsCraneOrder.class, 0, 0);
    }

    // 입고 완료된 지시 조회 (wcs에서 재고이동 지시인경우도, ecs rack 입장에선 입고완료시 완료임)
    private List<TbWcsCraneOrder> selectPendingWcsCraneOrdersForRackInbound() {
        int inboundOrderType = EcsDBConsts.OrderType.INBOUND.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.COMPLETE.getValue();
        String sql = """
                SELECT * 
                FROM tb_wcs_crane_order 
                WHERE ecs_if_status < :ecsIfStatus
                    AND order_key in (
                        SELECT order_key 
                        FROM tb_ecs_rack_order 
                        WHERE order_type = :inboundOrderType
                            AND order_status = :completeStatus
                        );
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,inboundOrderType,completeStatus", ecsIfStatus, inboundOrderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsCraneOrder.class, 0, 0);
    }

    // 출고 완료된 지시 조회 (입고로 이송하는 route order 지시 완료시 출고 완료로 봄)
    private List<TbWcsCraneOrder> selectPendingWcsCraneOrdersForRouteOutbound() {
        int outboundOrderType = EcsDBConsts.OrderType.OUTBOUND.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.COMPLETE.getValue();
        String sql = """
                SELECT * 
                FROM tb_wcs_crane_order 
                WHERE ecs_if_status < :ecsIfStatus 
                    AND order_key in (
                        SELECT order_key 
                        FROM tb_ecs_route_order 
                        WHERE order_type = :outboundOrderType 
                            AND order_status = :completeStatus
                        );
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,outboundOrderType,completeStatus", ecsIfStatus, outboundOrderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsCraneOrder.class, 0, 0);
    }

    // 이동 완료된 지시 조회 (from loc code 의 층과, to loc code의 층이 같은 경우 이동으로 본다.)
    private List<TbWcsCraneOrder> selectPendingWcsCraneOrdersForMove() {
        int orderType = EcsDBConsts.OrderType.OUTBOUND.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.COMPLETE.getValue();
        String sql = """
                SELECT * 
                FROM tb_wcs_crane_order 
                WHERE ecs_if_status < :ecsIfStatus 
                    AND order_key in (
                        SELECT order_key
                        FROM tb_ecs_rack_order
                        WHERE LEFT(from_loc_code, 1) = LEFT(to_loc_code, 1) 
                            AND order_type = :orderType 
                            AND order_status = :completeStatus
                        );
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,orderType,completeStatus", ecsIfStatus, orderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsCraneOrder.class, 0, 0);
    }

    // 전시회 기준 / TODO: 검토 후 삭제
    // 1층 출고단으로 완료된 지시 조회
    private List<TbWcsCraneOrder> selectPendingWcsCraneOrdersForAgfOutbound() {
        int outboundOrderType = EcsDBConsts.OrderType.OUTBOUND.getValue();
        int completeStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int ecsIfStatus = EcsDBConsts.EcsIfStatus.COMPLETE.getValue();
        String sql = """
                SELECT * 
                FROM tb_wcs_crane_order 
                WHERE ecs_if_status < :ecsIfStatus 
                    AND order_key in (
                        SELECT order_key 
                        FROM tb_ecs_rack_order 
                        WHERE order_type = :outboundOrderType 
                            AND order_status = :completeStatus 
                            AND to_loc_code in ('10601', '10602')
                        );
                """;
        Map<String, Object> params = ValueUtil.newMap("ecsIfStatus,outboundOrderType,completeStatus", ecsIfStatus, outboundOrderType, completeStatus);
        return iQueryManager.selectListBySql(sql, params, TbWcsCraneOrder.class, 0, 0);
    }
}
