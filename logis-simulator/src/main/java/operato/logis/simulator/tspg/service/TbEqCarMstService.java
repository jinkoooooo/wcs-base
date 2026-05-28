package operato.logis.simulator.tspg.service;

import operato.logis.simulator.tspg.entity.TbEqCarErrorLog;
import operato.logis.simulator.tspg.entity.TbEqCarMst;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class TbEqCarMstService extends AbstractQueryService {

    public List<TbEqCarMst> getCarMstList() {
        String sql = "SELECT * FROM tb_eq_car_mst ORDER BY eq_id ASC";
        return this.queryManager.selectListBySql(sql, null, TbEqCarMst.class, 0, 0);
    }

    public TbEqCarMst getCarMstByEqId(String eqId) {
        String sql = "SELECT * FROM tb_eq_car_mst WHERE eq_id = :eqId";
        Map<String, Object> param = ValueUtil.newMap("eqId", eqId);
        return this.queryManager.selectBySql(sql, param, TbEqCarMst.class);
    }

    public void updateCarStatus(TbEqCarMst car) {
        this.queryManager.update(car, "row", "bay", "level", "rackId", "rackEqId",
                "plcCmdId", "plcCompCmdId", "completeYn", "status", "cargoYn", "errorId", "errorDesc");
    }

    public void createCarErrorLog(TbEqCarErrorLog log) {
        log.setId(null);
        this.queryManager.insert(log);
    }
}