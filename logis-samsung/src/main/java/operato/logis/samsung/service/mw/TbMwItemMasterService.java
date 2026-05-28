package operato.logis.samsung.service.mw;

import operato.logis.samsung.entity.mw.TbMwBcrItemDimensionAvg;
import operato.logis.samsung.entity.mw.TbMwItemMaster;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
public class TbMwItemMasterService extends AbstractQueryService {

    public TbMwItemMaster getItemMaster(String itemCode) {
        String sql = "select * from tb_mw_item_master where inner_item_code = :itemCode";
        Map<String, Object> param = ValueUtil.newMap("itemCode", itemCode);
        return this.queryManager.selectBySql(sql, param, TbMwItemMaster.class);
    }
    public TbMwItemMaster getItemMasterByMaterial(String itemCode) {
        String sql = "select * from tb_mw_item_master where item_code = :itemCode";
        Map<String, Object> param = ValueUtil.newMap("itemCode", itemCode);
        return this.queryManager.selectBySql(sql, param, TbMwItemMaster.class);
    }
    public TbMwBcrItemDimensionAvg getItemDimension(String itemCode) {
        String sql = "select * from tb_mw_bcr_item_dimension_avg where inner_item_code = :itemCode";
        Map<String, Object> param = ValueUtil.newMap("itemCode", itemCode);
        return this.queryManager.selectBySql(sql, param, TbMwBcrItemDimensionAvg.class);
    }
}