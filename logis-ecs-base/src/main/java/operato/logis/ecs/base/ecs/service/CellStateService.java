package operato.logis.ecs.base.ecs.service;

import operato.logis.ecs.base.ecs.dashboard.realtime.dto.CellStatusUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 셀 상태 관리(BUSINESS302) 서비스.
 *
 * ============================================
 * v8.1 — eq_group_id 스코프 격리
 * ============================================
 *  tb_inventory_location 은 (loc_id + rack_eq_id + loc_group) 복합키로
 *  동일 loc_id 가 여러 ZONE 에 병존한다.
 *  과거 버전은 loc_id 만으로 조회/업데이트해서 다른 ZONE 의 동일 loc_id
 *  레코드까지 영향을 주는 버그가 있었음.
 *  이 버전부터는 모든 SELECT/UPDATE 에서 loc_group = :eqGroupId 조건을
 *  반드시 포함한다.
 *
 * ============================================
 * 금지 상태 3축 분리
 * ============================================
 *      is_enabled           → 전체 사용 가능 (= LOCK 여부)
 *      is_inbound_enabled   → 입고 허용
 *      is_outbound_enabled  → 출고 허용
 */
@Service
public class CellStateService extends AbstractQueryService {

    private static final List<String> ALLOW_ACTIONS = Arrays.asList(
            "ALLOW_IN", "FORBID_IN", "FORBID_IN_ALL",
            "ALLOW_OUT", "FORBID_OUT", "FORBID_OUT_ALL",
            "LOCK", "UNLOCK");

    private static final List<String> BULK_ALLOWED_ACTIONS = Arrays.asList(
            "ALLOW_IN", "ALLOW_OUT",
            "FORBID_IN_ALL", "FORBID_OUT_ALL",
            "LOCK", "UNLOCK");

    // ============================================
    // 조회: 존 / 레벨
    // ============================================

    @Transactional(readOnly = true)
    public List<Map> getZoneGroups() {
        String sql =
                "SELECT DISTINCT COALESCE(em.eq_group_id, r.eq_id) AS eq_group_id " +
                        "  FROM tb_eq_rack_mst r " +
                        "  LEFT JOIN tb_eq_mst em ON em.id = r.eq_id " +
                        " WHERE COALESCE(em.eq_group_id, r.eq_id) IS NOT NULL " +
                        " ORDER BY 1 ASC";
        return this.queryManager.selectListBySql(sql, new HashMap<>(), Map.class, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<Map> getLevelOptions(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }
        String sql =
                "SELECT DISTINCT r.level AS level " +
                        "  FROM tb_eq_rack_mst r " +
                        "  LEFT JOIN tb_eq_mst em ON em.id = r.eq_id " +
                        " WHERE COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId " +
                        " ORDER BY r.level ASC";
        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    // ============================================
    // 조회: ZONE + (선택) level 의 셀 목록
    // ============================================

    /**
     * 셀 목록 조회.
     *
     *  ※ JOIN 조건 포인트:
     *    - loc.loc_id  = r.rack_id                              (셀 ID 매칭)
     *    - loc.rack_eq_id = r.eq_id                          (랙의 장비 ID 매칭)
     *    - loc.loc_group = COALESCE(em.eq_group_id, r.eq_id) (ZONE 스코프 고정)
     *
     *    이 3개를 모두 묶어야 다른 ZONE 의 동일 loc_id 레코드가
     *    섞여 들어오지 않는다.
     */
    @Transactional(readOnly = true)
    public List<Map> getCellsByGroup(String eqGroupId, Integer level) {
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }

        String sql = """
            SELECT r.rack_id, r.eq_id, r.type, r.row, r.bay, r.level,
                   r.drive_only_yn,
                   COALESCE(em.eq_group_id, r.eq_id) AS eq_group_id,
                   CONCAT(COALESCE(em.eq_group_id, r.eq_id), '-',
                          LPAD(r.row::text, 2, '0'), '-',
                          LPAD(r.bay::text, 2, '0'), '-',
                          LPAD(r.level::text, 2, '0')) AS stor_loc,
                   loc.task_id, loc.stock_id,

                   /* 재고 축 상태 판정 (금지 여부는 여기서 배제)
                      stock.stock_status: 0=IDLE, 1=INBOUND, 2=OUTBOUND, 3=RELOCATION, 4=INBOUND_READY
                      ※ 한 loc.stock_id 가 여러 tb_inventory_stock row 를 가질 수 있어(혼적 파렛트),
                         JOIN 대신 EXISTS 로 row 중복을 회피한다. 같은 파렛트의 모든 stock 은
                         BCR/픽업 시 동일 상태로 함께 전이되므로 ANY 매칭으로 충분하다. */
                   CASE
                     WHEN r.drive_only_yn = true                                                    THEN 'DRIVE'
                     WHEN NULLIF(loc.stock_id, '') = 'DOUBLE_IN'                                    THEN 'DOUBLE_IN'
                     WHEN NULLIF(loc.stock_id, '') = 'EMPTY_OUT'                                    THEN 'EMPTY_OUT'
                     WHEN NULLIF(loc.task_id, '')  IS NOT NULL
                      AND NULLIF(loc.stock_id, '') IS NULL                                          THEN 'INBOUND'
                     WHEN NULLIF(loc.task_id, '')  IS NOT NULL
                      AND NULLIF(loc.stock_id, '') IS NOT NULL
                      AND EXISTS (
                            SELECT 1 FROM tb_inventory_stock stk
                             WHERE stk.stock_id    = loc.stock_id
                               AND stk.eq_group_id = loc.loc_group
                               AND stk.stock_status = 4
                          )                                                                         THEN 'INBOUND_READY'
                     WHEN NULLIF(loc.task_id, '')  IS NOT NULL
                      AND NULLIF(loc.stock_id, '') IS NOT NULL
                      AND EXISTS (
                            SELECT 1 FROM tb_inventory_stock stk
                             WHERE stk.stock_id    = loc.stock_id
                               AND stk.eq_group_id = loc.loc_group
                               AND stk.stock_status = 1
                          )                                                                         THEN 'INBOUND'
                     WHEN NULLIF(loc.task_id, '')  IS NOT NULL
                      AND NULLIF(loc.stock_id, '') IS NOT NULL                                      THEN 'OUTBOUND'
                     WHEN NULLIF(loc.stock_id, '') IS NOT NULL                                      THEN 'PRODUCT'
                     ELSE 'EMPTY'
                   END AS state_code,

                   /* 금지 축 플래그 (NULL → false) */
                   COALESCE(loc.is_enabled, true) = false                                           AS locked,
                   COALESCE(loc.is_inbound_enabled, true) = false                                   AS inbound_forbidden,
                   COALESCE(loc.is_outbound_enabled, true) = false                                  AS outbound_forbidden

              FROM tb_eq_rack_mst r
              LEFT JOIN tb_eq_mst em ON em.id = r.eq_id
              LEFT JOIN tb_inventory_location loc
                     ON loc.loc_id   = r.rack_id
                    AND loc.rack_eq_id = r.eq_id
                    AND loc.loc_group  = COALESCE(em.eq_group_id, r.eq_id)

             WHERE COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId
            """;

        if (level != null) {
            sql += "   AND r.level = :level ";
        }
        sql += " ORDER BY r.row DESC, r.bay DESC, r.level DESC ";

        Map<String, Object> params = new HashMap<>();
        params.put("eqGroupId", eqGroupId);
        if (level != null) {
            params.put("level", level);
        }

        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    // ============================================
    // 상태 변경 (단일 / Zone+Level 일괄)
    // ============================================

    /**
     * 셀 상태 일괄 변경.
     *
     *  ⚠ v8.1 중요 변경:
     *    UPDATE 의 WHERE 절에 loc_group = :eqGroupId 를 반드시 포함한다.
     *    그렇지 않으면 동일 loc_id 를 가진 다른 ZONE 의 레코드까지 바뀐다.
     *    따라서 이제 요청에는 eqGroupId 가 "항상" 필수다.
     */
    @Transactional
    public int updateCellsStatus(CellStatusUpdateRequest req) {
        if (req == null) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        String action = req.getAction();
        if (ValueUtil.isEmpty(action) || !ALLOW_ACTIONS.contains(action)) {
            throw new ElidomRuntimeException("지원하지 않는 action: " + action);
        }

        List<String> ids = req.getCellIds();
        String eqGroupId = req.getEqGroupId();
        Integer level = req.getLevel();

        // eqGroupId 는 이제 필수. 다른 ZONE 의 동일 loc_id 오염 방지.
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }

        boolean useBulkByZone = (ids == null || ids.isEmpty());
        if (useBulkByZone) {
            if (level == null) {
                throw new ElidomRuntimeException("cellIds 또는 (eqGroupId + level) 중 하나는 필수입니다");
            }
            if (!BULK_ALLOWED_ACTIONS.contains(action)) {
                throw new ElidomRuntimeException("해당 action 은 ZONE+Level 일괄 적용을 지원하지 않습니다: " + action);
            }
            ids = this.fetchCellIdsByZoneLevel(eqGroupId, level);
            if (ids.isEmpty()) {
                return 0;
            }
        }

        // ── tb_inventory_location UPDATE 절 구성 ──
        StringBuilder setClause = new StringBuilder();

        switch (action) {
            case "ALLOW_IN":
                setClause.append("is_inbound_enabled = true");
                break;

            case "FORBID_IN":
            case "FORBID_IN_ALL":
                setClause.append("is_inbound_enabled = false");
                break;

            case "ALLOW_OUT":
                setClause.append("is_outbound_enabled = true");
                break;

            case "FORBID_OUT":
            case "FORBID_OUT_ALL":
                setClause.append("is_outbound_enabled = false");
                break;

            case "LOCK":
                setClause.append("is_enabled = false");
                break;

            case "UNLOCK":
                setClause.append("is_enabled = true, ")
                        .append("is_inbound_enabled = true, ")
                        .append("is_outbound_enabled = true");
                break;

            default:
                throw new ElidomRuntimeException("지원하지 않는 action: " + action);
        }

        //  WHERE 절에 loc_group 필수 — 다른 ZONE 오염 방지
        String locSql = "UPDATE tb_inventory_location " +
                "   SET " + setClause.toString() + ", " +
                "       updated_at = NOW() " +
                " WHERE loc_id IN (:LocIds) " +
                "   AND loc_group = :eqGroupId ";
        Map<String, Object> locParams = new HashMap<>();
        locParams.put("LocIds", ids);
        locParams.put("eqGroupId", eqGroupId);
        int locCount = this.queryManager.executeBySql(locSql, locParams);

        // ── tb_eq_rack_mst.use_yn 동기화 (LOCK/UNLOCK 만) ──
        // tb_eq_rack_mst.id 는 글로벌 PK 이므로 ZONE 스코프 영향 없음.
        // 단, 혹시 모를 교차 참조를 막기 위해 eq_id 도 함께 제한.
        if ("LOCK".equals(action) || "UNLOCK".equals(action)) {
            boolean useYn = "UNLOCK".equals(action);
            String rackSql = "UPDATE tb_eq_rack_mst r " +
                    "   SET use_yn = :useYn, " +
                    "       updated_at = NOW() " +
                    "  FROM tb_eq_mst em " +
                    " WHERE r.eq_id = em.id " +
                    "   AND r.rack_id IN (:ids) " +
                    "   AND r.drive_only_yn = false " +
                    "   AND COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId ";
            Map<String, Object> rackParams = new HashMap<>();
            rackParams.put("useYn", useYn);
            rackParams.put("ids", ids);
            rackParams.put("eqGroupId", eqGroupId);
            this.queryManager.executeBySql(rackSql, rackParams);
        }

        return locCount;
    }

    /**
     * ZONE + level 에 속하는 셀 id 전체 조회 (drive_only 제외).
     */
    private List<String> fetchCellIdsByZoneLevel(String eqGroupId, Integer level) {
        String sql =
                "SELECT r.rack_id " +
                        "  FROM tb_eq_rack_mst r " +
                        "  LEFT JOIN tb_eq_mst em ON em.id = r.eq_id " +
                        " WHERE COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId " +
                        "   AND r.level = :level " +
                        "   AND r.drive_only_yn = false ";
        Map<String, Object> params = new HashMap<>();
        params.put("eqGroupId", eqGroupId);
        params.put("level", level);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        return rows.stream()
                .map(r -> String.valueOf(r.get("rack_id")))
                .filter(rackId -> rackId != null && !rackId.isEmpty() && !"null".equals(rackId))
                .toList();
    }

    // ============================================
    // 재고 상세 조회 — eqGroupId 스코프 필수
    // ============================================

    @Transactional(readOnly = true)
    public List<Map> getCellStockDetail(String cellId, String eqGroupId) {
        if (ValueUtil.isEmpty(cellId) || ValueUtil.isEmpty(eqGroupId)) {
            return List.of();
        }
        String sql =
                "SELECT loc.loc_id AS stor_loc, stk.item_code, itm.item_name, " +
                "       itm.item_type AS item_spec, '' AS item_memo, stk.lot_no, " +
                "       stk.item_qty, 'BOX' AS unit, " +
                "       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS produce_date, " +
                "       TO_CHAR(stk.expired_datetime, 'YYYY-MM-DD') AS expire_date, " +
                "       stk.stock_status, stk.stock_id AS task_id, " +
                "       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS inbound_date " +
                "  FROM tb_inventory_location loc " +
                "  LEFT JOIN tb_inventory_stock stk ON stk.id = loc.stock_id " +
                "  LEFT JOIN tb_inventory_item_mst itm ON itm.item_code = stk.item_code " +
                " WHERE loc.loc_id = :cellId " +
                "   AND loc.loc_group = :eqGroupId " +
                " ORDER BY stk.inb_datetime DESC";
        Map<String, Object> params = ValueUtil.newMap("cellId,eqGroupId", cellId, eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * 다중 셀 재고 상세 조회.
     *  - loc_group = eqGroupId 로 반드시 필터.
     *    (다른 ZONE 의 동일 loc_id 재고가 섞여 나오는 것을 방지)
     */
    @Transactional(readOnly = true)
    public List<Map> getCellStockDetailMulti(List<String> cellIds, String eqGroupId) {
        if (cellIds == null || cellIds.isEmpty() || ValueUtil.isEmpty(eqGroupId)) {
            return List.of();
        }
        String sql =
        "SELECT loc.loc_id AS stor_loc, stk.item_code, itm.item_name, " +
                "       itm.item_type AS item_spec, '' AS item_memo, stk.lot_no, " +
                "       stk.item_qty, 'BOX' AS unit, " +
                "       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS produce_date, " +
                "       TO_CHAR(stk.expired_datetime, 'YYYY-MM-DD') AS expire_date, " +
                "       stk.stock_status, stk.stock_id AS task_id, " +
                "       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS inbound_date " +
                "  FROM tb_inventory_location loc " +
                "  LEFT JOIN tb_inventory_stock stk ON stk.id = loc.stock_id " +
                "  LEFT JOIN tb_inventory_item_mst itm ON itm.item_code = stk.item_code " +
                " WHERE loc.loc_id IN (:cellIds) " +
                "   AND loc.loc_group = :eqGroupId " +
                " ORDER BY loc.loc_id ASC, stk.inb_datetime DESC";
        Map<String, Object> params = ValueUtil.newMap("cellIds,eqGroupId", cellIds, eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }
}