package operato.logis.ecs.base.wcs.service.impl;

import operato.logis.ecs.base.wcs.entity.ExtTbInventoryItemMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [ExtTbInventoryItemMaster Entity Service]
 * 테이블: tb_inventory_item_mst
 */
@Service
public class ExtTbInventoryItemMasterService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ExtTbInventoryItemMasterService.class);

    public ExtTbInventoryItemMaster findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(ExtTbInventoryItemMaster.class, id);
    }

    public ExtTbInventoryItemMaster findByOwnerAndCode(String itemOwner, String itemCode) {
        if (ValueUtil.isEmpty(itemOwner) || ValueUtil.isEmpty(itemCode)) return null;

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("item_owner", itemOwner);
        condition.addFilter("item_code", itemCode);
        return this.queryManager.selectByCondition(ExtTbInventoryItemMaster.class, condition);
    }

    public List<ExtTbInventoryItemMaster> findByOwner(String itemOwner) {
        if (ValueUtil.isEmpty(itemOwner)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("item_owner", itemOwner);
        return this.queryManager.selectList(ExtTbInventoryItemMaster.class, condition);
    }

    public ExtTbInventoryItemMaster insert(ExtTbInventoryItemMaster entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    public ExtTbInventoryItemMaster update(ExtTbInventoryItemMaster entity) {
        this.queryManager.update(entity);
        return entity;
    }

    /** 화주 + 아이템코드 리스트로 일괄 조회 (N+1 방지) */
    public List<ExtTbInventoryItemMaster> findByOwnerAndCodes(String itemOwner, List<String> itemCodes) {
        if (ValueUtil.isEmpty(itemOwner) || ValueUtil.isEmpty(itemCodes)) {
            return Collections.emptyList();
        }
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("item_owner", itemOwner);
        condition.addFilter("item_code", "in", itemCodes);
        return this.queryManager.selectList(ExtTbInventoryItemMaster.class, condition);
    }

    /** 화주 + 코드 리스트 → Map<itemCode, Master> */
    public Map<String, ExtTbInventoryItemMaster> findAsMapByOwnerAndCodes(String itemOwner, List<String> itemCodes) {
        return findByOwnerAndCodes(itemOwner, itemCodes).stream()
                .collect(Collectors.toMap(
                        ExtTbInventoryItemMaster::getItemCode,
                        m -> m,
                        (a, b) -> a   // 중복 시 첫 번째 유지
                ));
    }
}
