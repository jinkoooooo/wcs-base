package operato.logis.kmat_2026.biz.ecs.tspg4way.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEcsRouteOrder;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
@Slf4j
public class InternalWcsCallbackService {

    private final IQueryManager iQueryManager;

    public InternalWcsCallbackService(IQueryManager iQueryManager) {
        this.iQueryManager = iQueryManager;
    }

    public void inboundConveyorBcrRead(String orderKey){
        log.info("inboundConveyorBcrRead "+ orderKey);
        try {
            Thread.sleep(1*1000);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.error("inboundConveyorBcrRead thread sleep  "+e.getMessage());
        }


        var routeOrder = selectTbEcsRouteOrder(orderKey);
        int cmdStatus = EcsDBConsts.EcsRouteOrderCmdStatus.INBOUND_READY.getValue();
        routeOrder.setCmdStatus(cmdStatus);
        iQueryManager.update(routeOrder);
    }

    private TbEcsRouteOrder selectTbEcsRouteOrder(String orderKey) {
        int orderStatus = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String sql = """
                SELECT *
                FROM tb_ecs_route_order
                WHERE order_status <> :orderStatus
                and  order_key = :orderKey
                """;
        Map<String, Object> params = ValueUtil.newMap("orderStatus,orderKey", orderStatus, orderKey);
        return iQueryManager.selectBySql(sql, params, TbEcsRouteOrder.class);
    }
}
