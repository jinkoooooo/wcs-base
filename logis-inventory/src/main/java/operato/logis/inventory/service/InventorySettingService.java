package operato.logis.inventory.service;

import operato.logis.inventory.entity.TbInventorySetting;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class InventorySettingService extends AbstractQueryService {

    /**
     * optionName에 해당하는 optionValue 조회
     *
     * @param optionName 설정 이름
     * @return optionName에 해당하는 optionValue
     */
    public String getOptionValue(String optionName) {
        String sql = "select option_value from tb_inventory_setting where option_name = :optionName";
        Map<String, Object> param = ValueUtil.newMap("optionName", optionName);
        return this.queryManager.selectBySql(sql, param, String.class);
    }

    public List<TbInventorySetting> getTotalSetting() {
        String sql = "select * from tb_inventory_setting order by option_name asc";
        return this.queryManager.selectListBySql(sql, null, TbInventorySetting.class, 0, 0);
    }

    public void setOptionValue(List<TbInventorySetting> optionList) {
        for (TbInventorySetting option : optionList) {
            String sql = "update tb_inventory_setting set option_value = :optionValue where option_name = :optionName";
            Map<String, Object> param = ValueUtil.newMap("optionName,optionValue", option.getOptionName(), option.getOptionValue());
            this.queryManager.executeBySql(sql, param);
        }
    }
}