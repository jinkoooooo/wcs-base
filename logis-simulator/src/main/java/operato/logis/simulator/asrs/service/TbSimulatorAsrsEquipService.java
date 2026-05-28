package operato.logis.simulator.asrs.service;

import operato.logis.simulator.asrs.entity.TbSimulatorAsrsEquip;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Service
public class TbSimulatorAsrsEquipService extends AbstractQueryService {

    public List<TbSimulatorAsrsEquip> getEquipList() {
        String sql = "SELECT * FROM tb_simulator_equip ORDER BY crane_id ASC";
        return this.queryManager.selectListBySql(sql, null, TbSimulatorAsrsEquip.class, 0, 0);
    }

    public void updateEquipStatus(TbSimulatorAsrsEquip equip) {
        this.queryManager.update(equip);
    }
}