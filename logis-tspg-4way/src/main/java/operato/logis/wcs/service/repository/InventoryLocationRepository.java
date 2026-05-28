package operato.logis.wcs.service.repository;

import operato.logis.wcs.common.service.audit.AuditReason;
import operato.logis.wcs.consts.LocStatus;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.dto.LocWithPosition;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
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
 * ExtTbInventoryLocation 영속성 전담 DAO.
 *
 * 로케이션의 조회·생성·수정과 task_id 락(soft/hard CAS)·stock_id 적재/해제·active_task_count 증감을 한 aggregate 단위로 캡슐화한다.
 * 락 계열은 CAS(task_id 현재값 조건)로 동시성 race 를 차단하며, 트랜잭션 커밋/롤백 시 해제된다.
 */
@Repository
public class InventoryLocationRepository extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryLocationRepository.class);

    /** task_id IS NULL 일 때만 lockBy 로 점유 (CAS). 성공 여부 반환. */
    @Transactional(rollbackFor = Exception.class)
    public boolean lock(String eqGroupId, String locId, String lockBy) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return false;

        String sql = """
            UPDATE tb_inventory_location SET task_id = :taskId
             WHERE loc_group = :locGroup AND loc_id = :locId AND task_id IS NULL
            """;
        Map<String, Object> params = ValueUtil.newMap("taskId,locGroup,locId", lockBy, eqGroupId, locId);
        return AuditReason.call("lock", () -> this.queryManager.executeBySql(sql, params)) > 0;
    }

    /** task_id 해제 (무조건). */
    @Transactional(rollbackFor = Exception.class)
    public void unlock(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        String sql = """
            UPDATE tb_inventory_location SET task_id = NULL
             WHERE loc_group = :locGroup AND loc_id = :locId
            """;
        Map<String, Object> params = ValueUtil.newMap("locGroup,locId", eqGroupId, locId);
        AuditReason.run("unlock", () -> this.queryManager.executeBySql(sql, params));
    }

    /** PK 로 단건 조회. */
    public ExtTbInventoryLocation findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(ExtTbInventoryLocation.class, id);
    }

    /** (eqGroup, locId) 로 단건 조회. */
    public ExtTbInventoryLocation findByEqGroupIdAndLocId(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("loc_id", locId);
        return this.queryManager.selectByCondition(ExtTbInventoryLocation.class, condition);
    }

    /** loc_code 로 단건 조회. */
    public ExtTbInventoryLocation findByLocCode(String locCode) {
        if (ValueUtil.isEmpty(locCode)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_code", locCode);
        return this.queryManager.selectByCondition(ExtTbInventoryLocation.class, condition);
    }

    /** (eqGroup, locId IN) 다건 조회. */
    public List<ExtTbInventoryLocation> findByEqGroupIdAndLocId(String eqGroupId, List<String> LocIdList) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(LocIdList)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("loc_id", OrmConstants.IN, LocIdList);
        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    /** (eqGroup, stock_id) 로 적재 로케이션 단건 조회. */
    public ExtTbInventoryLocation findByStockId(String eqGroupId, String stockId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("stock_id", stockId);
        return this.queryManager.selectByCondition(ExtTbInventoryLocation.class, condition);
    }

    /** (eqGroup, loc_type IN) 로케이션 목록 조회. */
    public List<ExtTbInventoryLocation> findByEqGroupIdAndLocTypeList(String eqGroupId, List<String> locTypeList) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locTypeList)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("loc_type", OrmConstants.IN, locTypeList);
        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    /**
     * 후보 중 적치 가능한 로케이션 1건을 FOR UPDATE SKIP LOCKED 로 선점 조회.
     * 호출자 트랜잭션 강제(MANDATORY) — 락은 커밋/롤백 시 해제.
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public ExtTbInventoryLocation findAndLockBestOne(String eqGroupId, List<String> candidateCodes) {
        if (ValueUtil.isEmpty(candidateCodes)) {
            return null;
        }

        String sql = """
            SELECT * FROM tb_inventory_location
             WHERE loc_group = :locGroup
               AND loc_id IN (:LocIds)
               AND is_enabled = true AND (task_id IS NULL OR task_id = '') AND (stock_id IS NULL OR stock_id = '')
             LIMIT 1 FOR UPDATE SKIP LOCKED
            """;

        Map<String, Object> params = ValueUtil.newMap("locGroup,LocIds", eqGroupId, candidateCodes);
        List<ExtTbInventoryLocation> list = this.queryManager.selectListBySql(sql, params, ExtTbInventoryLocation.class, 0, 0);

        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }

    /** locId 의 loc_level 조회 (없으면 null). */
    public Integer getLevelByLocId(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return null;
        ExtTbInventoryLocation location = findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(location)) return null;
        return location.getLocLevel();
    }

    /** (eqGroup, locId 목록, type) 의 위치좌표 포함 로케이션 조회. type/LocIds 는 옵션 필터. */
    public List<LocWithPosition> findMultipleWithPosition(String eqGroupId, List<String> LocIds, LocType locTypeEnumCode) {
        if (ValueUtil.isEmpty(eqGroupId)) return Collections.emptyList();

        StringBuilder sql = new StringBuilder("""
            SELECT l.id AS loc_id, l.loc_id, l.loc_type, l.loc_group, l.rack_eq_id,
                   l.task_id, l.stock_id, l.is_enabled,
                   r.row AS rack_row, r.bay AS rack_bay, r.level AS rack_level, r.drive_only_yn as rack_drive_only_yn
              FROM tb_inventory_location l
              LEFT OUTER JOIN tb_eq_rack_mst r ON l.loc_id = r.id
             WHERE l.loc_group = :locGroup
            """);

        Map<String, Object> params = ValueUtil.newMap("locGroup", eqGroupId);

        // LocIds 지정 시에만 IN 필터 추가
        if (ValueUtil.isNotEmpty(LocIds)) {
            sql.append("  AND l.loc_id IN (:LocIds) ");
            params.put("LocIds", LocIds);
        }

        // locType 지정 시에만 필터 추가
        if (ValueUtil.isNotEmpty(locTypeEnumCode)) {
            sql.append(" AND l.loc_type = :locType ");
            params.put("locType", locTypeEnumCode.code().toString());
        }

        List<Map> rows = this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 0);
        return toLocWithPositionList(rows);
    }

    /** (eqGroup, type) 의 가용 랙 조회. status=EMPTY 면 미적재, OCCUPIED 면 적재된 랙만 (task_id NULL 공통). */
    public List<ExtTbInventoryLocation> findAvailableRacks(String eqGroupId, LocType type, LocStatus status) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(type)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_group", eqGroupId);
        condition.addFilter("is_enabled", true);
        condition.addFilter("task_id", OrmConstants.IS_NULL, null);

        // EMPTY 는 stock_id NULL, OCCUPIED 는 stock_id NOT NULL 추가 필터
        if (status == LocStatus.EMPTY) {
            condition.addFilter("stock_id", OrmConstants.IS_NULL, null);
        } else if (status == LocStatus.OCCUPIED) {
            condition.addFilter("stock_id", OrmConstants.IS_NOT_NULL, null);
        }

        condition.addOrder("loc_id", true);
        return this.queryManager.selectList(ExtTbInventoryLocation.class, condition);
    }

    /** 신규 로케이션 insert. */
    @Transactional(rollbackFor = Exception.class)
    public ExtTbInventoryLocation insert(ExtTbInventoryLocation entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 지정 컬럼만 update. */
    @Transactional(rollbackFor = Exception.class)
    public ExtTbInventoryLocation update(ExtTbInventoryLocation entity, String... params) {
        this.queryManager.update(entity, params);
        return entity;
    }

    /** stock_id 적재 갱신 (대상 없으면 no-op). */
    @Transactional(rollbackFor = Exception.class)
    public void updateStockId(String eqGroupId, String locId, String stockId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        ExtTbInventoryLocation loc = this.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(loc)) {
            logger.warn("[ Inventory ][ Loc ] updateStockId skipped, not found - eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        loc.setStockId(stockId);
        this.queryManager.update(loc, "stockId");
    }

    /** stock_id + barcode 해제 (대상 없으면 no-op). */
    @Transactional(rollbackFor = Exception.class)
    public void clearStockId(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        ExtTbInventoryLocation loc = this.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(loc)) {
            logger.warn("[ Inventory ][ Loc ] clearStockId skipped, not found - eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        loc.setStockId(null);
        loc.setBarcode(null);
        this.queryManager.update(loc, "stockId", "barcode");
    }

    /** stock_id + barcode 동시 적재 갱신 (대상 없으면 no-op). */
    @Transactional(rollbackFor = Exception.class)
    public void updateStockIdAndBarcode(String eqGroupId, String locId, String stockId, String barcode) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        ExtTbInventoryLocation loc = this.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(loc)) {
            logger.warn("[ Inventory ][ Loc ] updateStockIdAndBarcode skipped, not found - eqGroupId={}, locId={}",
                    eqGroupId, locId);
            return;
        }

        loc.setStockId(stockId);
        loc.setBarcode(barcode);
        this.queryManager.update(loc, "stockId", "barcode");
    }

    /** active_task_count +1 (NULL 은 0 기준). */
    @Transactional(rollbackFor = Exception.class)
    public void incrementActiveTaskCount(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        String sql = """
            UPDATE tb_inventory_location
               SET active_task_count = COALESCE(active_task_count, 0) + 1
             WHERE loc_group = :locGroup AND loc_id = :locId
            """;
        Map<String, Object> params = ValueUtil.newMap("locGroup,locId", eqGroupId, locId);
        AuditReason.run("incrementActiveTaskCount", () -> this.queryManager.executeBySql(sql, params));
    }

    /** active_task_count -1 (0 미만 방지). */
    @Transactional(rollbackFor = Exception.class)
    public void decrementActiveTaskCount(String eqGroupId, String locId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locId)) return;

        String sql = """
            UPDATE tb_inventory_location
               SET active_task_count = GREATEST(COALESCE(active_task_count, 0) - 1, 0)
             WHERE loc_group = :locGroup AND loc_id = :locId
            """;
        Map<String, Object> params = ValueUtil.newMap("locGroup,locId", eqGroupId, locId);
        AuditReason.run("decrementActiveTaskCount", () -> this.queryManager.executeBySql(sql, params));
    }

    /** Map row 목록을 LocWithPosition 목록으로 변환. */
    @SuppressWarnings("rawtypes")
    private List<LocWithPosition> toLocWithPositionList(List<Map> rows) {
        if (ValueUtil.isEmpty(rows)) return Collections.emptyList();
        List<LocWithPosition> result = new ArrayList<>(rows.size());
        // row 별로 로케이션 엔티티 + 랙 좌표 매핑
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

    /** Number 면 int, 아니면 0. */
    private int safeInt(Object v) { return v instanceof Number n ? n.intValue() : 0; }

    /** Boolean/문자/숫자 표현을 boolean 으로 정규화. */
    private boolean safeBool(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return "Y".equalsIgnoreCase(s) || "1".equals(s);
        if (v instanceof Number n) return n.intValue() == 1;
        return false;
    }

    /** null 안전 toString. */
    private String str(Object v) { return v == null ? null : v.toString(); }

    /**
     * BCR 매칭 후보 조회 — INBOUND_READY 상태 stock 과 매핑된 enabled location 의 JOIN.
     * Map row 키: loc_id, stock_id, loc_deep, loc_row, loc_col, barcode, sku, lot_no, item_owner.
     */
    public List<Map> findInboundReadyJoinedStock(String eqGroupId, Integer inboundReadyStatusValue) {
        if (ValueUtil.isEmpty(eqGroupId)) return Collections.emptyList();

        String sql = """
            SELECT l.loc_id     AS loc_id,
                   l.stock_id   AS stock_id,
                   l.loc_deep   AS loc_deep,
                   l.loc_row    AS loc_row,
                   l.loc_col    AS loc_col,
                   l.barcode    AS barcode,
                   s.sku        AS sku,
                   s.lot_no     AS lot_no,
                   s.item_owner AS item_owner
              FROM tb_inventory_location l
              JOIN tb_inventory_stock s
                ON s.eq_group_id = l.loc_group
               AND s.stock_id    = l.stock_id
             WHERE l.loc_group     = :eqGroupId
               AND l.is_enabled    = true
               AND s.stock_status  = :readyStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,readyStatus",
                eqGroupId, inboundReadyStatusValue);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * Soft Reserve — host_order 단계 점유 마킹. task_id IS NULL 인 경우에만 hostOrderKey 로 set (CAS).
     * 성공 시 true, 이미 다른 작업이 잡고 있으면 false.
     */
    public boolean softReserve(String eqGroupId, String locId, String hostOrderKey) {
        String sql = """
            UPDATE tb_inventory_location
               SET task_id = :hostOrderKey,
                   updated_at = NOW()
             WHERE loc_group = :eqGroupId
               AND loc_id = :locId
               AND task_id IS NULL
            """;

        Map<String, Object> params = ValueUtil.newMap(
                "hostOrderKey,eqGroupId,locId", hostOrderKey, eqGroupId, locId
        );

        int affected = this.queryManager.executeBySql(sql, params);
        return affected > 0;
    }

    /**
     * Soft Release — 자기 host_order_key 가 잡고 있던 task_id 만 해제.
     * shuttle 단계로 넘어가 wcsOrderKey 로 덮어쓰여있으면 no-op.
     * 풀리면 true, 자기 키 아니면 false.
     */
    public boolean softRelease(String eqGroupId, String locId, String hostOrderKey) {
        String sql = """
            UPDATE tb_inventory_location
               SET task_id = NULL,
                   updated_at = NOW()
             WHERE loc_group = :eqGroupId
               AND loc_id = :locId
               AND task_id = :hostOrderKey
            """;

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,locId,hostOrderKey", eqGroupId, locId, hostOrderKey
        );

        int affected = this.queryManager.executeBySql(sql, params);
        return affected > 0;
    }

    /**
     * Lock with CAS — soft reservation 위에도 덮어쓰기 허용.
     * task_id IS NULL 이거나 expectedTaskId 와 같을 때만 newOrderKey 로 set. 성공 여부 반환.
     */
    public boolean lockOverride(String eqGroupId, String locId,
                                String newOrderKey, String expectedTaskId) {
        // expectedTaskId 가 있으면 그 값과의 일치도 허용하도록 OR 절을 동적 구성
        String expectedClause = ValueUtil.isNotEmpty(expectedTaskId) ? "    OR task_id = :expectedTaskId" : "";
        String sql = """
            UPDATE tb_inventory_location
               SET task_id = :newOrderKey,
                   updated_at = NOW()
             WHERE loc_group = :eqGroupId
               AND loc_id = :locId
               AND (task_id IS NULL
            %s
                   )
            """.formatted(expectedClause);

        Map<String, Object> params = ValueUtil.newMap(
                "newOrderKey,eqGroupId,locId", newOrderKey, eqGroupId, locId
        );
        if (ValueUtil.isNotEmpty(expectedTaskId)) {
            params.put("expectedTaskId", expectedTaskId);
        }

        int affected = this.queryManager.executeBySql(sql, params);
        return affected > 0;
    }
}
