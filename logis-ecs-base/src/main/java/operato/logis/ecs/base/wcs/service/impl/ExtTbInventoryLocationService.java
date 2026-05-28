package operato.logis.ecs.base.wcs.service.impl;


import operato.logis.ecs.base.wcs.consts.WcsDomainEnums;
import operato.logis.ecs.base.wcs.dto.LocWithPosition;
import operato.logis.ecs.base.wcs.entity.ExtTbInventoryLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * [ExtTbInventoryLocation Entity Service]
 * - lockYn/lockBy → taskId
 * - status → stockId IS NULL/NOT NULL
 * - useYn → isEnabled
 * - eqGroupId → locGroup
 * - rackCellId → locId
 */
@Service
public class ExtTbInventoryLocationService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ExtTbInventoryLocationService.class);

    // ========================================================================
    // 1. Atomic Operations (원자적 상태 변경)
    // ========================================================================

    @Transactional
    public boolean lock(String eqGroupId, String locId, String lockBy) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return false;

        String sql = "UPDATE tb_inventory_location SET task_id = :taskId " +
                " WHERE loc_group = :locGroup AND loc_id = :locId AND task_id IS NULL";
        Map<String, Object> params = ValueUtil.newMap("taskId,locGroup,locId", lockBy, eqGroupId, locId);
        return this.queryManager.executeBySql(sql, params) > 0;
    }

    @Transactional
    public void unlock(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        String sql = "UPDATE tb_inventory_location SET task_id = NULL " +
                " WHERE loc_group = :locGroup AND loc_id = :locId";
        Map<String, Object> params = ValueUtil.newMap("locGroup,locId", eqGroupId, locId);
        this.queryManager.executeBySql(sql, params);
    }

    // ========================================================================
    // 2. Basic Retrieval (기본 조회)
    // ========================================================================

    public ExtTbInventoryLocation findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(ExtTbInventoryLocation.class, id);
    }

    public ExtTbInventoryLocation findByEqGroupIdAndLocId(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return null;

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("loc_id", locId);

        return this.queryManager.selectByCondition(ExtTbInventoryLocation.class, condition);
    }

    public ExtTbInventoryLocation findByLocCode(String locCode) {
        if (ValueUtil.isEmpty(locCode)) return null;

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_code", locCode);

        return this.queryManager.selectByCondition(ExtTbInventoryLocation.class, condition);
    }

    public List<ExtTbInventoryLocation> findByEqGroupIdAndLocId(String eqGroupId, List<String> LocIdList) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(LocIdList)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("loc_id", OrmConstants.IN, LocIdList);

        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    public List<ExtTbInventoryLocation> findAllByLocId(String locId) {
        if (ValueUtil.isEmpty(locId)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_id", locId);

        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    /** eqGroupId + stockId로 로케이션 조회 (해당 stockId가 적재된 로케이션) */
    public ExtTbInventoryLocation findByStockId(String eqGroupId, String stockId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) return null;

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("stock_id", stockId);

        return this.queryManager.selectByCondition(ExtTbInventoryLocation.class, condition);
    }

    public List<ExtTbInventoryLocation> findByEqGroupIdAndLocTypeList(String eqGroupId, List<String> locTypeList) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locTypeList)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("loc_type", OrmConstants.IN, locTypeList);
        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    /**
     * 후보군 중에서 실제로 사용 가능한 로케이션 하나를 '선점(Lock)'하며 가져옵니다.
     * 동시성 제어를 위해 DB Native SQL의 FOR UPDATE를 활용합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public ExtTbInventoryLocation findAndLockBestOne(String eqGroupId, List<String> candidateCodes) {
        if (ValueUtil.isEmpty(candidateCodes)) {
            logger.info("findAndLockBestOne candidateCodes null");
            return null;
        }

        String sql = "SELECT * FROM tb_inventory_location " +
                " WHERE loc_group = :locGroup " +
                "   AND loc_id IN (:LocIds) " +
                "   AND is_enabled = true AND (task_id IS NULL OR task_id = '') AND (stock_id IS NULL OR stock_id = '') " +
                " LIMIT 1 FOR UPDATE SKIP LOCKED";

        Map<String, Object> params = ValueUtil.newMap("locGroup,LocIds", eqGroupId, candidateCodes);
        List<ExtTbInventoryLocation> list = this.queryManager.selectListBySql(sql, params, ExtTbInventoryLocation.class, 0, 0);

        if (ValueUtil.isEmpty(list)) {
            logger.info("findAndLockBestOne 결과 없음");
        }

        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }

    /** 로케이션 코드를 기반으로 해당 위치의 물리적 층(Level) 정보를 조회합니다. */
    public Integer getLevelByLocId(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return null;

        ExtTbInventoryLocation location = findByEqGroupIdAndLocId(eqGroupId, locId);

        if (ValueUtil.isEmpty(location)) return null;

        return location.getLocLevel();
    }

    // ========================================================================
    // 3. Availability Checks (상태 체크)
    // ========================================================================

    public boolean isAvailable(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = findByEqGroupIdAndLocId(eqGroupId, locId);
        return loc != null && Boolean.TRUE.equals(loc.getIsEnabled()) && loc.getTaskId() == null;
    }

    public boolean isInboundAllowed(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = findByEqGroupIdAndLocId(eqGroupId, locId);
        return loc != null && Boolean.TRUE.equals(loc.getIsEnabled()) && loc.getTaskId() == null && loc.getStockId() == null;
    }

    public boolean isOutboundAllowed(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = findByEqGroupIdAndLocId(eqGroupId, locId);
        return loc != null && Boolean.TRUE.equals(loc.getIsEnabled()) && loc.getTaskId() == null && loc.getStockId() != null;
    }

    public boolean exists(String eqGroupId, String locId) {
        return findByEqGroupIdAndLocId(eqGroupId, locId) != null;
    }

    // ========================================================================
    // 4. Complex Search & Business Logic (조인 및 필터링)
    // ========================================================================

    public List<LocWithPosition> findMultipleWithPosition(String eqGroupId, List<String> LocIds, WcsDomainEnums.LocType locTypeEnumCode) {
        if (ValueUtil.isEmpty(eqGroupId)) return Collections.emptyList();

        StringBuilder sql = new StringBuilder(
                "SELECT l.id AS loc_id, l.loc_id, l.loc_type, l.loc_group, l.rack_eq_id, " +
                        "       l.task_id, l.stock_id, l.is_enabled, " +
                        "       r.row AS rack_row, r.bay AS rack_bay, r.level AS rack_level, r.drive_only_yn as rack_drive_only_yn " +
                        "  FROM tb_inventory_location l " +
                        "  LEFT OUTER JOIN tb_eq_rack_mst r ON l.loc_id = r.id " +
                        " WHERE l.loc_group = :locGroup "
        );

        Map<String, Object> params = ValueUtil.newMap("locGroup", eqGroupId);

        if (!ValueUtil.isEmpty(LocIds)) {
            sql.append("  AND l.loc_id IN (:LocIds) ");
            params.put("LocIds", LocIds);
        }

        if (locTypeEnumCode != null) {
            sql.append(" AND l.loc_type = :locType ");
            params.put("locType", locTypeEnumCode.code().toString());
        }

        List<Map> rows = this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 0);
        return toLocWithPositionList(rows);
    }

    public List<ExtTbInventoryLocation> findAvailableRacks(String eqGroupId, WcsDomainEnums.LocType type, WcsDomainEnums.LocStatus status) {
        if (ValueUtil.isEmpty(eqGroupId) || type == null) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("is_enabled", true);
        condition.addFilter("task_id", OrmConstants.IS_NULL, null);

        // status=EMPTY → stockId IS NULL, status=OCCUPIED → stockId IS NOT NULL
        if (status == WcsDomainEnums.LocStatus.EMPTY) {
            condition.addFilter("stock_id", OrmConstants.IS_NULL, null);
        } else if (status == WcsDomainEnums.LocStatus.OCCUPIED) {
            condition.addFilter("stock_id", OrmConstants.IS_NOT_NULL, null);
        }

        condition.addOrder("loc_id", true);

        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    public List<ExtTbInventoryLocation> findAvailableByEqGroupIdAndLocIds(String eqGroupId, List<String> LocIds, WcsDomainEnums.LocStatus locStatus) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(LocIds) || locStatus == null)
            return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("loc_id", OrmConstants.IN, LocIds);
        condition.addFilter("is_enabled", true);
        condition.addFilter("task_id", OrmConstants.IS_NULL, null);

        if (locStatus == WcsDomainEnums.LocStatus.EMPTY) {
            condition.addFilter("stock_id", OrmConstants.IS_NULL, null);
        } else if (locStatus == WcsDomainEnums.LocStatus.OCCUPIED) {
            condition.addFilter("stock_id", OrmConstants.IS_NOT_NULL, null);
        }

        condition.addOrder("loc_id", true);

        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    public List<ExtTbInventoryLocation> findEmptyByEqGroupIdAndLocIds(String eqGroupId, List<String> LocIds) {
        return findAvailableByEqGroupIdAndLocIds(eqGroupId, LocIds, WcsDomainEnums.LocStatus.EMPTY);
    }

    public List<ExtTbInventoryLocation> findOccupiedByEqGroupIdAndLocIds(String eqGroupId, List<String> LocIds) {
        return findAvailableByEqGroupIdAndLocIds(eqGroupId, LocIds, WcsDomainEnums.LocStatus.OCCUPIED);
    }

    // ========================================================================
    // 5. Mutations (생성/수정)
    // ========================================================================

    @Transactional
    public ExtTbInventoryLocation insert(ExtTbInventoryLocation entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    @Transactional
    public ExtTbInventoryLocation update(ExtTbInventoryLocation entity, String... params) {
        this.queryManager.update(entity, params);
        return entity;
    }

    /**
     * 로케이션에 stockId를 매핑한다 (입고 완료 시 호출).
     * stockId가 세팅되면 해당 로케이션은 OCCUPIED 상태로 간주된다.
     */
    /**
     * 로케이션에 stockId를 매핑한다 (입고 완료 시 호출).
     * queryManager.update()를 사용하여 updatedAt 자동 갱신 보장.
     */
    @Transactional
    public void updateStockId(String eqGroupId, String locId, String stockId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        ExtTbInventoryLocation loc = this.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (loc == null) {
            logger.warn("updateStockId: location not found. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        loc.setStockId(stockId);
        this.queryManager.update(loc, "stockId");
    }

    /**
     * 로케이션의 stockId를 제거한다 (출고/이동 FROM_LOADING 시 호출).
     * barcode도 함께 제거한다.
     */
    @Transactional
    public void clearStockId(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        ExtTbInventoryLocation loc = this.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (loc == null) {
            logger.warn("clearStockId: location not found. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        loc.setStockId(null);
        loc.setBarcode(null);
        this.queryManager.update(loc, "stockId", "barcode");
    }

    /** 로케이션에 stockId와 barcode를 함께 세팅한다 (입고 COMPLETE 시 호출). */
    @Transactional
    public void updateStockIdAndBarcode(String eqGroupId, String locId, String stockId, String barcode) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        ExtTbInventoryLocation loc = this.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (loc == null) {
            logger.warn("updateStockIdAndBarcode: location not found. eqGroupId={}, locId={}",
                    eqGroupId, locId);
            return;
        }

        loc.setStockId(stockId);
        loc.setBarcode(barcode);
        this.queryManager.update(loc, "stockId", "barcode");
    }

    /**
     * 포트의 active_task_count를 원자적으로 1 증가시킨다.
     * 다중 파렛트 출고 할당 시 같은 포트만 반복 선택되지 않도록 즉시 반영한다.
     */
    @Transactional
    public void incrementActiveTaskCount(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        String sql = "UPDATE tb_inventory_location " +
                "   SET active_task_count = COALESCE(active_task_count, 0) + 1 " +
                " WHERE loc_group = :locGroup AND loc_id = :locId";
        Map<String, Object> params = ValueUtil.newMap("locGroup,locId", eqGroupId, locId);
        this.queryManager.executeBySql(sql, params);
    }

    /**
     * 포트의 active_task_count를 원자적으로 1 감소시킨다.
     * 오더 완료/실패/취소 시 호출.
     */
    @Transactional
    public void decrementActiveTaskCount(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        String sql = "UPDATE tb_inventory_location " +
                "   SET active_task_count = GREATEST(COALESCE(active_task_count, 0) - 1, 0) " +
                " WHERE loc_group = :locGroup AND loc_id = :locId";
        Map<String, Object> params = ValueUtil.newMap("locGroup,locId", eqGroupId, locId);
        this.queryManager.executeBySql(sql, params);
    }

    // ========================================================================
    // 6. Helpers
    // ========================================================================

    @SuppressWarnings("rawtypes")
    private List<LocWithPosition> toLocWithPositionList(List<Map> rows) {
        if (ValueUtil.isEmpty(rows)) return Collections.emptyList();
        List<LocWithPosition> result = new ArrayList<>(rows.size());
        for (Map row : rows) {
            ExtTbInventoryLocation loc = new ExtTbInventoryLocation();
            loc.setId(str(row.get("loc_id")));
            loc.setLocId(str(row.get("loc_id")));
            loc.setLocType(str(row.get("loc_type")));
            loc.setLocGroup(str(row.get("loc_group")));
            loc.setRackEqId(str(row.get("rack_eq_id")));
            loc.setTaskId(str(row.get("task_id")));
            loc.setStockId(str(row.get("stock_id")));
            loc.setIsEnabled(safeBool(row.get("is_enabled")));

            result.add(new LocWithPosition(loc, safeInt(row.get("rack_row")), safeInt(row.get("rack_bay")), safeInt(row.get("rack_level")), safeBool(row.get("rack_drive_only_yn"))));
        }
        return result;
    }

    private int safeInt(Object v) { return v instanceof Number ? ((Number) v).intValue() : 0; }

    private boolean safeBool(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return "Y".equalsIgnoreCase((String) v) || "1".equals(v);
        if (v instanceof Number) return ((Number) v).intValue() == 1;
        return false;
    }

    private String str(Object v) { return v == null ? null : v.toString(); }
}
