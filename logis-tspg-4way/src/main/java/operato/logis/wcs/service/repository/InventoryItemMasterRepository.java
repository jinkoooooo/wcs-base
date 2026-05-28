package operato.logis.wcs.service.repository;

import operato.logis.wcs.common.service.audit.AuditReason;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import javax.transaction.Transactional;
import java.beans.Transient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ExtTbInventoryItemMaster 영속성 전담 DAO.
 *
 * 아이템 마스터의 조회·생성·수정과 상위 시스템 인수(fetched) 표시를 한 aggregate 단위로 캡슐화한다.
 */
@Repository
public class InventoryItemMasterRepository extends AbstractQueryService {

    /** PK 로 단건 조회. */
    public ExtTbInventoryItemMaster findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(ExtTbInventoryItemMaster.class, id);
    }

    /** item_code 로 단건 조회 (활성 마스터). */
    public ExtTbInventoryItemMaster findByCode(String itemCode) {
        if (ValueUtil.isEmpty(itemCode)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("item_code", itemCode);
        condition.addFilter("deleted_at", OrmConstants.IS_NULL, null);
        return this.queryManager.selectByCondition(ExtTbInventoryItemMaster.class, condition);
    }

    /** (item_owner, item_code) 로 단건 조회. */
    public ExtTbInventoryItemMaster findByOwnerAndCode(String itemOwner, String itemCode) {
        if (ValueUtil.isEmpty(itemOwner) || ValueUtil.isEmpty(itemCode)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("item_owner", itemOwner);
        condition.addFilter("item_code", itemCode);
        // soft delete 제외 (활성 마스터만)
        condition.addFilter("deleted_at", OrmConstants.IS_NULL, null);
        return this.queryManager.selectByCondition(ExtTbInventoryItemMaster.class, condition);
    }

    /** item_owner 의 마스터 목록 조회. */
    public List<ExtTbInventoryItemMaster> findByOwner(String itemOwner) {
        if (ValueUtil.isEmpty(itemOwner)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("item_owner", itemOwner);
        // soft delete 제외 (활성 마스터만)
        condition.addFilter("deleted_at", OrmConstants.IS_NULL, null);
        return this.queryManager.selectList(ExtTbInventoryItemMaster.class, condition);
    }

    /** 신규 마스터 insert. */
    @Transactional
    public ExtTbInventoryItemMaster insert(ExtTbInventoryItemMaster entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 전체 update. */
    @Transactional
    public ExtTbInventoryItemMaster update(ExtTbInventoryItemMaster entity) {
        this.queryManager.update(entity);
        return entity;
    }

    /** 전체 update. */
    @Transactional
    public ExtTbInventoryItemMaster update(ExtTbInventoryItemMaster entity, String... fieldNames) {
        this.queryManager.update(entity, fieldNames);
        return entity;
    }

    /** 조회 전으로 치환 */
    @Transactional
    public void setFetchedFalse(ExtTbInventoryItemMaster entity){
        entity.setFetched(false);
        this.queryManager.update(entity,"fetched");
    }

    /** (item_owner, item_code IN) 다건 조회. */
    public List<ExtTbInventoryItemMaster> findByOwnerAndCodes(String itemOwner, List<String> itemCodes) {
        if (ValueUtil.isEmpty(itemOwner) || ValueUtil.isEmpty(itemCodes)) {
            return Collections.emptyList();
        }
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("item_owner", itemOwner);
        condition.addFilter("item_code", "in", itemCodes);
        // soft delete 제외 (활성 마스터만)
        condition.addFilter("deleted_at", OrmConstants.IS_NULL, null);
        return this.queryManager.selectList(ExtTbInventoryItemMaster.class, condition);
    }

    /** (item_owner, item_code IN) 조회 결과를 item_code -> 마스터 맵으로 반환 (중복 시 선행 우선). */
    public Map<String, ExtTbInventoryItemMaster> findAsMapByOwnerAndCodes(String itemOwner, List<String> itemCodes) {
        return findByOwnerAndCodes(itemOwner, itemCodes).stream()
                .collect(Collectors.toMap(
                        ExtTbInventoryItemMaster::getItemCode,
                        m -> m,
                        (a, b) -> a
                ));
    }

    /** 상위 미인수 마스터 조회 — fetched=false. pull 대상 (updatedAt 순). */
    public List<ExtTbInventoryItemMaster> findUnfetched() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("fetched", Boolean.FALSE);
        condition.addOrder("updatedAt", true);
        return this.queryManager.selectList(ExtTbInventoryItemMaster.class, condition);
    }

    /**
     * 상위 인수 표시 — 표준 update 경로는 beforeUpdate 가 fetched 를 false 로 되돌리므로
     * raw SQL 로 우회한다. audit 은 명시적으로 남긴다.
     */
    public int markFetched(String id) {
        if (ValueUtil.isEmpty(id)) return 0;
        String sql = "UPDATE tb_inventory_item_mst SET fetched = TRUE, updated_at = now() WHERE id = :id";
        Map<String, Object> params = Map.of("id", id);
        return AuditReason.call("markFetched", () -> this.queryManager.executeBySql(sql, params));
    }

    /**
     * 다건 인수 일괄 표시 — LIMS pull 응답 직전 일괄 호출.
     * audit 보존을 위해 row 별 SELECT-DML-SELECT 패턴 사용.
     */
    public int markFetchedAll(List<String> ids) {
        if (ValueUtil.isEmpty(ids)) return 0;
        int affected = 0;
        // id 별로 markFetched 호출하여 audit 단위 보존
        for (String id : ids) affected += markFetched(id);
        return affected;
    }

    /**
     * soft delete — 물리삭제 대신 deleted_at set + 상위 재동기화 위해 fetched=false.
     * 이미 삭제된 행은 deleted_at IS NULL 가드로 무영향(멱등). audit 보존.
     */
    public int softDelete(String id, String reason) {
        if (ValueUtil.isEmpty(id)) return 0;
        // 물리삭제 대신 tombstone + 상위 pull 재동기화 트리거
        String sql = "UPDATE tb_inventory_item_mst SET deleted_at = now(), fetched = false WHERE id = :id AND deleted_at IS NULL";
        Map<String, Object> params = Map.of("id", id);
        String auditReason = ValueUtil.isEmpty(reason) ? "ITEM_MASTER_SOFT_DELETE" : "ITEM_MASTER_SOFT_DELETE: " + reason;
        return AuditReason.call(auditReason, () -> this.queryManager.executeBySql(sql, params));
    }
}
