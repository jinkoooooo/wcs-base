package operato.logis.simulator.asrs.service;

import operato.logis.simulator.asrs.entity.TbSimulatorAsrsOrder;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Service
public class TbSimulatorAsrsOrderService extends AbstractQueryService {

    public List<TbSimulatorAsrsOrder> getNewOrderList() {
        String sql = "SELECT * FROM tb_simulator_order WHERE order_status = 1 AND is_read = 0";
        List<TbSimulatorAsrsOrder> orderList = this.queryManager.selectListBySql(sql, null, TbSimulatorAsrsOrder.class, 0, 0);
        for (TbSimulatorAsrsOrder order : orderList) {
            order.setIsRead(1);
            this.queryManager.update(order, "isRead");
        }

        return orderList;
    }

    public void updateOrderStatus(TbSimulatorAsrsOrder order) {
        order.setIsRead(0);
        this.queryManager.update(order, "orderStatus", "isRead");
    }
}