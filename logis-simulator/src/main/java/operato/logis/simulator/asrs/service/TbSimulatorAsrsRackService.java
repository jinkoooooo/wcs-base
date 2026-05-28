package operato.logis.simulator.asrs.service;

import operato.logis.simulator.asrs.entity.TbSimulatorAsrsRack;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Service
public class TbSimulatorAsrsRackService extends AbstractQueryService {

    public List<TbSimulatorAsrsRack> getRackList() {
        String sql = "SELECT * FROM tb_simulator_rack ORDER BY loc_id ASC";
        return this.queryManager.selectListBySql(sql, null, TbSimulatorAsrsRack.class, 0, 0);
    }
}