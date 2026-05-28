package operato.logis.posco.service;

import operato.logis.posco.entity.TbMcsEquip;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Service
public class TbMcsEquipService extends AbstractQueryService {

    public List<TbMcsEquip> getEquipList() {
        String sql = "SELECT * FROM tb_mcs_equip ORDER BY equip_type asc, equip_code asc";
        return this.queryManager.selectListBySql(sql, null, TbMcsEquip.class, 0, 0);
    }
}