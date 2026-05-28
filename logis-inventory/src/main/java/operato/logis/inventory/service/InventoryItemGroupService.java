package operato.logis.inventory.service;

import operato.logis.inventory.consts.InventoryConstants;
import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.util.InventoryUtils;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryItemGroupService extends AbstractQueryService {

    /**
     * 전체 품목에 대한 전용 그룹 조회
     *
     * @param itemList 입고하는 재고의 전체 품목 정보
     * @return 그룹 목록을 우선순위에 따라 오름차순 정렬 후 중복을 제거하여 반환합니다.
     */
    public List<String> getDedicatedGroupList(List<ItemIdentifierDto> itemList) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> param = new HashMap<>();

        sql.append("SELECT DISTINCT ON (item_group) item_group ");
        sql.append("FROM tb_inventory_item_group ");
        InventoryUtils.appendItemInClause(sql, param, itemList);
        sql.append("AND restriction_type = :restrictionType ");
        sql.append("ORDER BY item_group, dedicated_priority ASC");

        param.put("restrictionType", InventoryConstants.RESTRICTION_DEDICATED);

        return this.queryManager.selectListBySql(sql.toString(), param, String.class, 0, 0);
    }

    /**
     * 전체 품목에 대한 금지 그룹 조회
     *
     * @param itemList 입고하는 재고의 전체 품목 정보
     * @return 그룹 목록을 중복을 제거하여 반환합니다.
     */
    public List<String> getForbiddenGroupList(List<ItemIdentifierDto> itemList) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> param = new HashMap<>();

        sql.append("SELECT DISTINCT item_group ");
        sql.append("FROM tb_inventory_item_group ");
        InventoryUtils.appendItemInClause(sql, param, itemList);
        sql.append("AND restriction_type = :restrictionType ");

        param.put("restrictionType", InventoryConstants.RESTRICTION_FORBIDDEN);

        return this.queryManager.selectListBySql(sql.toString(), param, String.class, 0, 0);
    }
}