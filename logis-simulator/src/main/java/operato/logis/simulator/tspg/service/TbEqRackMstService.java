package operato.logis.simulator.tspg.service;

import operato.logis.simulator.tspg.entity.TbEqRackMst;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class TbEqRackMstService extends AbstractQueryService {

    public List<TbEqRackMst> getRackMstList() {
        String sql = "SELECT * FROM tb_eq_rack_mst ORDER BY eq_id ASC";
        return this.queryManager.selectListBySql(sql, null, TbEqRackMst.class, 0, 0);
    }

    public TbEqRackMst getRackMstByEqId(String eqId) {
        String sql = "SELECT * FROM tb_eq_rack_mst WHERE eq_id = :eqId";
        Map<String, Object> param = ValueUtil.newMap("eqId", eqId);
        return this.queryManager.selectBySql(sql, param, TbEqRackMst.class);
    }
}