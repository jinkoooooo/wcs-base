package operato.logis.simulator.tspg.service;

import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.simulator.tspg.dto.OrderUpdateRequestDto;
import operato.logis.simulator.tspg.entity.TbEqCarOrder;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class TbEqCarOrderService extends AbstractQueryService {

    public List<TbEqCarOrder> getUnreadOrderList() {
        String sql = "SELECT * FROM tb_eq_car_order WHERE is_read = 0 ORDER BY created_at ASC";
        return this.queryManager.selectListBySql(sql, null, TbEqCarOrder.class, 0, 0);
    }

    public CommonApiResponse updateOrderStatus(OrderUpdateRequestDto requestParam) {
        String sql = "UPDATE tb_eq_car_order ";
        if ("read".equals(requestParam.getStatus())) {
            sql += "SET is_read = 1 WHERE order_key = :orderKey";
        } else if ("end".equals(requestParam.getStatus())) {
            sql += "SET req_complete_reset = 1 WHERE order_key = :orderKey";
        } else {
            return CommonApiResponse.success();
        }

        Map<String, Object> param = ValueUtil.newMap("orderKey", requestParam.getOrderKey());
        this.queryManager.executeBySql(sql, param);

        return CommonApiResponse.success();
    }
}