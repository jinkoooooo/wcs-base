package operato.logis.inventory.service;

import operato.logis.inventory.entity.TbInventoryEquipment;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class InventoryEquipmentService extends AbstractQueryService {

    public List<TbInventoryEquipment> getEquipmentListByType(String equipType) {
        String sql = "SELECT * FROM tb_inventory_equipment WHERE equip_type = :equipType ORDER BY equip_code ASC";
        Map<String, Object> param = ValueUtil.newMap("equipType", equipType);
        return this.queryManager.selectListBySql(sql, param, TbInventoryEquipment.class, 0, 0);
    }
}