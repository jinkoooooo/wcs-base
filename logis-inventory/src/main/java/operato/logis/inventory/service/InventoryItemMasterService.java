package operato.logis.inventory.service;

import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.util.InventoryUtils;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryItemMasterService extends AbstractQueryService {

    /**
     * 품목 리스트 중 가장 높은 등급(숫자가 작을수록 높은 등급)을 조회합니다.
     *
     * @param itemList 품목 식별자 리스트
     * @return 가장 높은 등급을 반환합니다.
     */
    public int getHighestItemGrade(List<ItemIdentifierDto> itemList) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> param = new HashMap<>();

        sql.append("SELECT MIN(item_grade) ");
        sql.append("FROM tb_inventory_item_mst ");
        InventoryUtils.appendItemInClause(sql, param, itemList);

        return this.queryManager.selectBySql(sql.toString(), param, Integer.class);
    }
}