package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.LocWithPosition;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * [TbWcsLocMst Entity Service - FINAL]
 * Lambda 방식을 제거하고 기존의 Query 객체 및 Raw SQL 방식으로 복구한 최종 서비스입니다.
 */
@Service
public class TbWcsLocMstService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbWcsLocMstService.class);

    // ========================================================================
    // 1. Atomic Operations (원자적 상태 변경)
    // ========================================================================

    @Transactional
    public boolean lock(String eqGroupId, String locCode, String lockBy) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locCode)) return false;

        String sql = "UPDATE tb_wcs_loc_mst SET lock_yn = 1, lock_by = :lockBy " +
                " WHERE eq_group_id = :eqGroupId AND loc_code = :locCode AND lock_yn = 0";
        Map<String, Object> params = ValueUtil.newMap("lockBy,eqGroupId,locCode", lockBy, eqGroupId, locCode);
        return this.queryManager.executeBySql(sql, params) > 0;
    }

    @Transactional
    public void unlock(String eqGroupId, String locCode, LocStatusEnumCode nextStatus) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locCode)) return;

        String sql = "UPDATE tb_wcs_loc_mst SET lock_yn = 0, lock_by = NULL, status = :status " +
                " WHERE eq_group_id = :eqGroupId AND loc_code = :locCode";
        Map<String, Object> params = ValueUtil.newMap("status,eqGroupId,locCode",
                nextStatus == null ? null : nextStatus.code(), eqGroupId, locCode);
        this.queryManager.executeBySql(sql, params);
    }

    // ========================================================================
    // 2. Basic Retrieval (기본 조회)
    // ========================================================================

    public TbWcsLocMst findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(TbWcsLocMst.class, id);
    }

    public TbWcsLocMst findByEqGroupIdAndLocCode(String eqGroupId, String locCode) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locCode)) return null;

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_code", locCode);

        return this.queryManager.selectByCondition(TbWcsLocMst.class, condition);
    }

    public List<TbWcsLocMst> findByEqGroupIdAndLocCode(String eqGroupId, List<String> locCodeList) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locCodeList)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_code", OrmConstants.IN, locCodeList);

        return this.queryManager.selectList(TbWcsLocMst.class, condition);
    }

    public List<TbWcsLocMst> findAllByLocCode(String locCode) {
        if (ValueUtil.isEmpty(locCode)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("loc_code", locCode);

        return this.queryManager.selectList(TbWcsLocMst.class, condition);
    }

    public List<TbWcsLocMst> findByEqGroupIdAndLocType(String eqGroupId, String locType) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locType)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_type", locType);

        return this.queryManager.selectList(TbWcsLocMst.class, condition);
    }

    public List<TbWcsLocMst> findByEqGroupIdAndLocTypeList(String eqGroupId, List<String> locTypeList) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locTypeList)) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_type", OrmConstants.IN,locTypeList);
        return this.queryManager.selectList(TbWcsLocMst.class, condition);
    }

    /**
     * 후보군 중에서 실제로 사용 가능한 로케이션 하나를 '선점(Lock)'하며 가져옵니다.
     * 동시성 제어를 위해 DB Native SQL의 FOR UPDATE를 활용합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TbWcsLocMst findAndLockBestOne(String eqGroupId, List<String> candidateCodes) {
        if (ValueUtil.isEmpty(candidateCodes)) return null;

        String sql = "SELECT * FROM tb_wcs_loc_mst " +
                " WHERE eq_group_id = :eqGroupId " +
                "   AND loc_code IN (:locCodes) " +
                "   AND use_yn = 1 AND lock_yn = 0 AND status = 0 " +
                " ORDER BY loc_seq ASC " +
                " LIMIT 1 FOR UPDATE SKIP LOCKED";

        Map<String, Object> params = ValueUtil.newMap("eqGroupId,locCodes", eqGroupId, candidateCodes);
        List<TbWcsLocMst> list = this.queryManager.selectListBySql(sql, params, TbWcsLocMst.class, 0, 1);

        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }

    // ========================================================================
    // 3. Availability Checks (상태 체크)
    // ========================================================================

    public boolean isAvailable(String eqGroupId, String locCode) {
        TbWcsLocMst loc = findByEqGroupIdAndLocCode(eqGroupId, locCode);
        return loc != null && loc.getUseYn() == 1 && loc.getLockYn() == 0;
    }

    public boolean isInboundAllowed(String eqGroupId, String locCode) {
        TbWcsLocMst loc = findByEqGroupIdAndLocCode(eqGroupId, locCode);
        return loc != null && loc.getUseYn() == 1 && loc.getLockYn() == 0 && LocStatusEnumCode.EMPTY.code().equals(loc.getStatus());
    }

    public boolean isOutboundAllowed(String eqGroupId, String locCode) {
        TbWcsLocMst loc = findByEqGroupIdAndLocCode(eqGroupId, locCode);
        return loc != null && loc.getUseYn() == 1 && loc.getLockYn() == 0 && LocStatusEnumCode.OCCUPIED.code().equals(loc.getStatus());
    }

    public boolean exists(String eqGroupId, String locCode) {
        return findByEqGroupIdAndLocCode(eqGroupId, locCode) != null;
    }

    // ========================================================================
    // 4. Complex Search & Business Logic (조인 및 필터링)
    // ========================================================================

    public List<LocWithPosition> findMultipleWithPosition(String eqGroupId, List<String> locCodes, LocTypeEnumCode locTypeEnumCode) {
        if (ValueUtil.isEmpty(eqGroupId)) return Collections.emptyList();

        StringBuilder sql = new StringBuilder(
                "SELECT l.id AS loc_id, l.loc_code, l.loc_type, l.eq_group_id, l.rack_cell_id, l.rack_eq_id, " +
                        "       l.capacity, l.lock_yn, l.lock_by, l.use_yn, l.status, l.loc_seq, " +
                        "       r.row AS rack_row, r.bay AS rack_bay, r.level AS rack_level, r.drive_only_yn as rack_drive_only_yn " +
                        "  FROM tb_wcs_loc_mst l " +
                        "  LEFT OUTER JOIN tb_eq_rack_mst r ON l.rack_cell_id = r.id " +
                        " WHERE l.eq_group_id = :eqGroupId "
        );

        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);

        if (!ValueUtil.isEmpty(locCodes)) {
            sql.append("  AND l.loc_code IN (:locCodes) ");
            params.put("locCodes", locCodes);
        }

        if (locTypeEnumCode != null) {
            sql.append(" AND l.loc_type = :locType ");
            params.put("locType", locTypeEnumCode.code().toString());
        }

        List<Map> rows = this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 0);
        return toLocWithPositionList(rows);
    }

    public List<TbWcsLocMst> findAvailableRacks(String eqGroupId, LocTypeEnumCode type, LocStatusEnumCode status) {
        if (ValueUtil.isEmpty(eqGroupId) || type == null) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_type", type.code());
        condition.addFilter("use_yn", 1);
        condition.addFilter("lock_yn", 0);
        condition.addFilter("status", status.code());
        condition.addOrder("rack_cell_id", true);

        return this.queryManager.selectList(TbWcsLocMst.class, condition);
    }

    public List<TbWcsLocMst> findAvailableByEqGroupIdAndLocCodes(String eqGroupId, List<String> locCodes, LocStatusEnumCode locStatus) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locCodes) || locStatus == null) return Collections.emptyList();

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_code", OrmConstants.IN, locCodes);
        condition.addFilter("use_yn", 1);
        condition.addFilter("lock_yn", 0);
        condition.addFilter("status", locStatus.code());
        condition.addFilter("loc_seq", OrmConstants.IS_NOT_NULL, null);
        condition.addOrder("loc_seq", true);

        return this.queryManager.selectList(TbWcsLocMst.class, condition);
    }

    public List<TbWcsLocMst> findEmptyByEqGroupIdAndLocCodes(String eqGroupId, List<String> locCodes) {
        return findAvailableByEqGroupIdAndLocCodes(eqGroupId, locCodes, LocStatusEnumCode.EMPTY);
    }

    public List<TbWcsLocMst> findOccupiedByEqGroupIdAndLocCodes(String eqGroupId, List<String> locCodes) {
        return findAvailableByEqGroupIdAndLocCodes(eqGroupId, locCodes, LocStatusEnumCode.OCCUPIED);
    }

    public List<TbWcsLocMst> selectNextByCursor(List<TbWcsLocMst> sortedCandidates, Integer lastSeq, int count) {
        if (ValueUtil.isEmpty(sortedCandidates)) return Collections.emptyList();
        List<TbWcsLocMst> result = new ArrayList<>();
        if (lastSeq != null) {
            sortedCandidates.stream().filter(l -> l.getLocSeq() != null && l.getLocSeq() > lastSeq).limit(count).forEach(result::add);
        }
        if (result.size() < count) {
            sortedCandidates.stream().filter(l -> !result.contains(l)).limit(count - result.size()).forEach(result::add);
        }
        return result;
    }

    public List<TbWcsLocMst> selectSmallestByLocSeq(List<TbWcsLocMst> sortedCandidates, int count) {
        if (ValueUtil.isEmpty(sortedCandidates)) return Collections.emptyList();
        return sortedCandidates.stream().sorted(Comparator.comparingInt(TbWcsLocMst::getLocSeq)).limit(count).collect(Collectors.toList());
    }

    // ========================================================================
    // 5. Mutations (생성/수정)
    // ========================================================================

    @Transactional public TbWcsLocMst insert(TbWcsLocMst entity) { this.queryManager.insert(entity); return entity; }
    @Transactional public TbWcsLocMst update(TbWcsLocMst entity) { this.queryManager.update(entity); return entity; }

    @Transactional
    public void updateLocStatus(String eqGroupId, String locCode, LocStatusEnumCode statusEnum) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locCode)) return;

        String sql = "UPDATE tb_wcs_loc_mst SET status = :status WHERE eq_group_id = :eqGroupId AND loc_code = :locCode";
        Map<String, Object> params = ValueUtil.newMap("status,eqGroupId,locCode",
                statusEnum == null ? null : statusEnum.code(), eqGroupId, locCode);

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
            TbWcsLocMst loc = new TbWcsLocMst();
            loc.setId(str(row.get("loc_id")));
            loc.setLocCode(str(row.get("loc_code")));
            loc.setLocType(str(row.get("loc_type")));
            loc.setEqGroupId(str(row.get("eq_group_id")));
            loc.setRackCellId(str(row.get("rack_cell_id")));
            loc.setRackEqId(str(row.get("rack_eq_id")));
            loc.setLockYn(safeInt(row.get("lock_yn")));
            loc.setLockBy(str(row.get("lock_by")));
            loc.setStatus(safeIntObj(row.get("status")));
            loc.setLocSeq(safeIntObj(row.get("loc_seq")));

            result.add(new LocWithPosition(loc, safeInt(row.get("rack_row")), safeInt(row.get("rack_bay")), safeInt(row.get("rack_level")), safeBool(row.get("rack_drive_only_yn"))));
        }
        return result;
    }

    private int safeInt(Object v) { return v instanceof Number ? ((Number) v).intValue() : 0; }
    private Integer safeIntObj(Object v) { return v instanceof Number ? ((Number) v).intValue() : null; }
    private boolean safeBool(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return "Y".equalsIgnoreCase((String) v) || "1".equals(v);
        if (v instanceof Number) return ((Number) v).intValue() == 1;
        return false;
    }
    private String str(Object v) { return v == null ? null : v.toString(); }
}