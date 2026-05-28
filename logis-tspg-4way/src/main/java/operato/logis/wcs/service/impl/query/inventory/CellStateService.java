package operato.logis.wcs.service.impl.query.inventory;

import operato.logis.wcs.common.service.audit.AuditReason;
import operato.logis.wcs.dto.CellClassificationUpdateRequest;
import operato.logis.wcs.dto.CellClassificationUpdateRequest.FieldChange;
import operato.logis.wcs.dto.CellStatusUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 셀 상태 관리(BUSINESS302) 서비스.
 *
 * state_code 결정 로직은 CellStateClassifier 가 SQL CASE 문을 SSOT 로 생성한다.
 * 본 서비스는 그 결과를 ZONE+Level 단위로 묶어 그리드 응답을 만들고
 * 운영자 액션(허용/금지/락/언락/분류)을 일괄 UPDATE 한다.
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

    // 사용 가능한 ZONE 목록 (em.eq_group_id 또는 r.eq_id).
    @Transactional(readOnly = true)
    public List<Map> getZoneGroups() {
        String sql = """
                SELECT DISTINCT COALESCE(em.eq_group_id, r.eq_id) AS eq_group_id
                  FROM tb_eq_rack_mst r
                  LEFT JOIN tb_eq_mst em ON em.id = r.eq_id
                 WHERE COALESCE(em.eq_group_id, r.eq_id) IS NOT NULL
                 ORDER BY 1 ASC
                """;
        return this.queryManager.selectListBySql(sql, new HashMap<>(), Map.class, 0, 0);
    }

    // 특정 ZONE 의 level 옵션 distinct.
    @Transactional(readOnly = true)
    public List<Map> getLevelOptions(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }
        String sql = """
                SELECT DISTINCT r.level AS level
                  FROM tb_eq_rack_mst r
                  LEFT JOIN tb_eq_mst em ON em.id = r.eq_id
                 WHERE COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId
                 ORDER BY r.level ASC
                """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * ZONE + (선택) level 의 셀 목록 조회.
     *
     * state_code 결정은 CellStateClassifier (SSOT) 에 위임.
     * 같은 stock_id 에 여러 inventory_stock row 가 있을 때 우선순위 1위 1개만
     * LATERAL 로 가져와 셀당 stock 스캔 1회로 처리.
     *
     * JOIN 조건 포인트:
     *   - loc.loc_id     = r.rack_id                       (셀 ID 매칭)
     *   - loc.rack_eq_id = r.eq_id                          (랙의 장비 ID 매칭)
     *   - loc.loc_group  = COALESCE(em.eq_group_id, r.eq_id) (ZONE 스코프 고정)
     */
    @Transactional(readOnly = true)
    public List<Map> getCellsByGroup(String eqGroupId, Integer level) {
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }

        // state_code CASE 문 + LATERAL JOIN 동적 생성 (Classifier SSOT)
        final String stateCodeSql = CellStateClassifier.stateCodeCaseSql("r", "loc", "stk", null);
        final String lateralSql   = CellStateClassifier.lateralStockSubquerySql("loc", "stk");

        String sql = """
            SELECT r.rack_id, r.eq_id, r.type, r.row, r.bay, r.level,
                   r.drive_only_yn,
                   COALESCE(em.eq_group_id, r.eq_id) AS eq_group_id,
                   CONCAT(COALESCE(em.eq_group_id, r.eq_id), '-',
                          LPAD(r.row::text, 2, '0'), '-',
                          LPAD(r.bay::text, 2, '0'), '-',
                          LPAD(r.level::text, 2, '0')) AS stor_loc,
                   loc.task_id, loc.stock_id,

                   /* LATERAL stock 컬럼 — Dashboard2D 의 stock_type 색상/액션 메뉴 분기용 */
                   stk.stock_type         AS stock_type,
                   stk.expired_datetime   AS expired_datetime,

                   /* 재고 축 상태 (Classifier 동적 주입) */
                   %s AS state_code,

                   /* 금지 축 플래그 (NULL → false) */
                   COALESCE(loc.is_enabled, true) = false                                           AS locked,
                   COALESCE(loc.is_inbound_enabled, true) = false                                   AS inbound_forbidden,
                   COALESCE(loc.is_outbound_enabled, true) = false                                  AS outbound_forbidden,

                   /* 분류·제약 축 — tb_inventory_location 의 4필드 (NOT NULL, 빈값 가능) */
                   COALESCE(loc.item_type,  '') AS item_type,
                   COALESCE(loc.item_group, '') AS item_group,
                   COALESCE(loc.max_weight, 0)  AS max_weight,
                   COALESCE(loc.max_height, 0)  AS max_height

              FROM tb_eq_rack_mst r
              LEFT JOIN tb_eq_mst em ON em.id = r.eq_id
              LEFT JOIN tb_inventory_location loc
                     ON loc.loc_id     = r.rack_id
                    AND loc.rack_eq_id = r.eq_id
                    AND loc.loc_group  = COALESCE(em.eq_group_id, r.eq_id)
              %s   /* LATERAL JOIN — 우선순위 1위 stock row */

             WHERE COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId
            """.formatted(stateCodeSql, lateralSql);

        if (ValueUtil.isNotEmpty(level)) {
            sql += "   AND r.level = :level ";
        }
        sql += " ORDER BY r.row DESC, r.bay DESC, r.level DESC ";

        Map<String, Object> params = new HashMap<>();
        params.put("eqGroupId", eqGroupId);
        if (ValueUtil.isNotEmpty(level)) {
            params.put("level", level);
        }

        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * 셀 상태(허용/금지/락/언락) 일괄 변경.
     *
     * WHERE 절에 loc_group = :eqGroupId 필수 — 동일 loc_id 를 가진 다른 ZONE 의 레코드 오염 방지.
     * 따라서 eqGroupId 가 항상 필수다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateCellsStatus(CellStatusUpdateRequest req) {
        if (ValueUtil.isEmpty(req)) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        String action = req.getAction();
        if (ValueUtil.isEmpty(action) || !ALLOW_ACTIONS.contains(action)) {
            throw new ElidomRuntimeException("지원하지 않는 action: " + action);
        }

        List<String> ids = req.getCellIds();
        String eqGroupId = req.getEqGroupId();
        Integer level = req.getLevel();

        // eqGroupId 필수 — 다른 ZONE 의 동일 loc_id 오염 방지
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }

        // cellIds 미지정 시 ZONE+Level 일괄 적용
        boolean useBulkByZone = ValueUtil.isEmpty(ids);
        if (useBulkByZone) {
            if (ValueUtil.isEmpty(level)) {
                throw new ElidomRuntimeException("cellIds 또는 (eqGroupId + level) 중 하나는 필수입니다");
            }
            if (!BULK_ALLOWED_ACTIONS.contains(action)) {
                throw new ElidomRuntimeException("해당 action 은 ZONE+Level 일괄 적용을 지원하지 않습니다: " + action);
            }
            ids = this.fetchCellIdsByZoneLevel(eqGroupId, level);
            if (ValueUtil.isEmpty(ids)) {
                return 0;
            }
        }

        // action 별 SET 절 빌드
        String setClause = switch (action) {
            case "ALLOW_IN" -> "is_inbound_enabled = true";
            case "FORBID_IN", "FORBID_IN_ALL" -> "is_inbound_enabled = false";
            case "ALLOW_OUT" -> "is_outbound_enabled = true";
            case "FORBID_OUT", "FORBID_OUT_ALL" -> "is_outbound_enabled = false";
            case "LOCK" -> "is_enabled = false";
            case "UNLOCK" -> "is_enabled = true, is_inbound_enabled = true, is_outbound_enabled = true";
            default -> throw new ElidomRuntimeException("지원하지 않는 action: " + action);
        };

        // UPDATE + audit
        String locSql = """
                UPDATE tb_inventory_location
                   SET %s,
                       updated_at = NOW()
                 WHERE loc_id IN (:LocIds)
                   AND loc_group = :eqGroupId
                """.formatted(setClause);
        Map<String, Object> locParams = new HashMap<>();
        locParams.put("LocIds", ids);
        locParams.put("eqGroupId", eqGroupId);

        String reason = String.format("%s: %s", action,
                ValueUtil.isEmpty(req.getComment()) ? "" : req.getComment().trim());
        int locCount = AuditReason.call(reason, () -> this.queryManager.executeBySql(locSql, locParams));

        // tb_eq_rack_mst.use_yn 동기화 (LOCK/UNLOCK 만)
        if ("LOCK".equals(action) || "UNLOCK".equals(action)) {
            boolean useYn = "UNLOCK".equals(action);
            String rackSql = """
                    UPDATE tb_eq_rack_mst r
                       SET use_yn = :useYn,
                           updated_at = NOW()
                      FROM tb_eq_mst em
                     WHERE r.eq_id = em.id
                       AND r.rack_id IN (:ids)
                       AND r.drive_only_yn = false
                       AND COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId
                    """;
            Map<String, Object> rackParams = new HashMap<>();
            rackParams.put("useYn", useYn);
            rackParams.put("ids", ids);
            rackParams.put("eqGroupId", eqGroupId);
            this.queryManager.executeBySql(rackSql, rackParams);
        }

        return locCount;
    }

    // ZONE + level 에 속하는 셀 id 전체 조회 (drive_only 제외).
    private List<String> fetchCellIdsByZoneLevel(String eqGroupId, Integer level) {
        String sql = """
                SELECT r.rack_id
                  FROM tb_eq_rack_mst r
                  LEFT JOIN tb_eq_mst em ON em.id = r.eq_id
                 WHERE COALESCE(em.eq_group_id, r.eq_id) = :eqGroupId
                   AND r.level = :level
                   AND r.drive_only_yn = false
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("eqGroupId", eqGroupId);
        params.put("level", level);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        return rows.stream()
                .map(r -> String.valueOf(r.get("rack_id")))
                .filter(rackId -> ValueUtil.isNotEmpty(rackId) && !"null".equals(rackId))
                .toList();
    }

    private static final String MODE_SET   = "set";
    private static final String MODE_CLEAR = "clear";

    /**
     * 분류·제약 4필드(item_type / item_group / max_weight / max_height) 일괄 UPDATE.
     *
     * 각 필드 mode 가 set/clear 인 것만 SET 절에 포함한다.
     * cellIds 가 비어 있고 level 이 있으면 ZONE+Level 전체 적용.
     * WHERE 절에 loc_group = :eqGroupId 항상 필수 (다른 ZONE 오염 방지).
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateCellsClassification(CellClassificationUpdateRequest req) {
        if (ValueUtil.isEmpty(req)) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        String eqGroupId = req.getEqGroupId();
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }

        // 대상 셀 ids 확정
        List<String> ids = req.getCellIds();
        boolean useBulkByZone = ValueUtil.isEmpty(ids);
        if (useBulkByZone) {
            if (ValueUtil.isEmpty(req.getLevel())) {
                throw new ElidomRuntimeException("cellIds 또는 (eqGroupId + level) 중 하나는 필수입니다");
            }
            ids = this.fetchCellIdsByZoneLevel(eqGroupId, req.getLevel());
            if (ValueUtil.isEmpty(ids)) {
                return 0;
            }
        }

        // SET 절 빌드 — mode=set/clear 만 포함
        List<String> setFragments = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        appendStringField(setFragments, params, "item_type",  req.getItemType());
        appendStringField(setFragments, params, "item_group", req.getItemGroup());
        appendIntField(setFragments, params, "max_weight", req.getMaxWeight());
        appendIntField(setFragments, params, "max_height", req.getMaxHeight());

        if (setFragments.isEmpty()) {
            throw new ElidomRuntimeException("변경할 필드가 없습니다 (모든 필드가 skip)");
        }

        // UPDATE 실행
        setFragments.add("updated_at = NOW()");
        String sql = """
                UPDATE tb_inventory_location
                   SET %s
                 WHERE loc_id IN (:LocIds)
                   AND loc_group = :eqGroupId
                """.formatted(String.join(", ", setFragments));
        params.put("LocIds", ids);
        params.put("eqGroupId", eqGroupId);
        return this.queryManager.executeBySql(sql, params);
    }

    /**
     * 문자열 필드(SET / CLEAR='') SET 절 조립. NOT NULL 컬럼이므로 clear 는 빈 문자열.
     */
    private void appendStringField(List<String> frags, Map<String, Object> params,
                                   String column, FieldChange<String> change) {
        if (change == null || ValueUtil.isEmpty(change.getMode())) return;
        String mode = change.getMode();
        if (MODE_CLEAR.equalsIgnoreCase(mode)) {
            frags.add(column + " = :" + column + "Val");
            params.put(column + "Val", "");
        } else if (MODE_SET.equalsIgnoreCase(mode)) {
            String v = change.getValue() == null ? "" : change.getValue().trim();
            frags.add(column + " = :" + column + "Val");
            params.put(column + "Val", v);
        }
        // skip 또는 알 수 없는 mode → 무시
    }

    /**
     * 정수 필드(SET / CLEAR=0) SET 절 조립. NOT NULL 컬럼이므로 clear 는 0 (= 미설정/무제한).
     */
    private void appendIntField(List<String> frags, Map<String, Object> params,
                                String column, FieldChange<Integer> change) {
        if (change == null || ValueUtil.isEmpty(change.getMode())) return;
        String mode = change.getMode();
        if (MODE_CLEAR.equalsIgnoreCase(mode)) {
            frags.add(column + " = :" + column + "Val");
            params.put(column + "Val", 0);
        } else if (MODE_SET.equalsIgnoreCase(mode)) {
            Integer v = change.getValue() == null ? 0 : change.getValue();
            if (v < 0) {
                throw new ElidomRuntimeException(column + " 는 음수일 수 없습니다");
            }
            frags.add(column + " = :" + column + "Val");
            params.put(column + "Val", v);
        }
    }

    /**
     * ZONE 내 기존 item_type / item_group 의 distinct 값 목록 (빈 문자열 제외).
     * 콤보박스 옵션 + 자유 입력 패턴의 옵션 소스.
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> getClassifyOptions(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eqGroupId 는 필수입니다");
        }
        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);

        String itemTypeSql = """
                SELECT DISTINCT item_type FROM tb_inventory_location
                 WHERE loc_group = :eqGroupId AND item_type IS NOT NULL AND item_type <> ''
                 ORDER BY item_type ASC
                """;
        String itemGroupSql = """
                SELECT DISTINCT item_group FROM tb_inventory_location
                 WHERE loc_group = :eqGroupId AND item_group IS NOT NULL AND item_group <> ''
                 ORDER BY item_group ASC
                """;

        List<Map> typeRows  = this.queryManager.selectListBySql(itemTypeSql,  params, Map.class, 0, 0);
        List<Map> groupRows = this.queryManager.selectListBySql(itemGroupSql, params, Map.class, 0, 0);

        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("item_types",  typeRows.stream().map(r  -> String.valueOf(r.get("item_type"))).toList());
        out.put("item_groups", groupRows.stream().map(r -> String.valueOf(r.get("item_group"))).toList());
        return out;
    }

    /**
     * 단일 셀의 재고 상세 조회 — eqGroupId 스코프 필수.
     */
    @Transactional(readOnly = true)
    public List<Map> getCellStockDetail(String cellId, String eqGroupId) {
        if (ValueUtil.isEmpty(cellId) || ValueUtil.isEmpty(eqGroupId)) {
            return List.of();
        }
        String sql = """
                SELECT loc.loc_id AS stor_loc, stk.item_code, itm.item_name,
                       itm.item_type AS item_spec, '' AS item_memo, stk.lot_no,
                       stk.item_qty, 'BOX' AS unit,
                       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS produce_date,
                       TO_CHAR(stk.expired_datetime, 'YYYY-MM-DD') AS expire_date,
                       stk.stock_status, stk.stock_id AS task_id,
                       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS inbound_date
                  FROM tb_inventory_location loc
                  LEFT JOIN tb_inventory_stock stk ON stk.id = loc.stock_id
                  LEFT JOIN tb_inventory_item_mst itm ON itm.item_code = stk.item_code
                 WHERE loc.loc_id = :cellId
                   AND loc.loc_group = :eqGroupId
                 ORDER BY stk.inb_datetime DESC
                """;
        Map<String, Object> params = ValueUtil.newMap("cellId,eqGroupId", cellId, eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    // 다중 셀 재고 상세 조회. loc_group = eqGroupId 로 반드시 필터(다른 ZONE 오염 방지).
    @Transactional(readOnly = true)
    public List<Map> getCellStockDetailMulti(List<String> cellIds, String eqGroupId) {
        if (ValueUtil.isEmpty(cellIds) || ValueUtil.isEmpty(eqGroupId)) {
            return List.of();
        }
        String sql = """
                SELECT loc.loc_id AS stor_loc, stk.item_code, itm.item_name,
                       itm.item_type AS item_spec, '' AS item_memo, stk.lot_no,
                       stk.item_qty, 'BOX' AS unit,
                       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS produce_date,
                       TO_CHAR(stk.expired_datetime, 'YYYY-MM-DD') AS expire_date,
                       stk.stock_status, stk.stock_id AS task_id,
                       TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD') AS inbound_date
                  FROM tb_inventory_location loc
                  LEFT JOIN tb_inventory_stock stk ON stk.id = loc.stock_id
                  LEFT JOIN tb_inventory_item_mst itm ON itm.item_code = stk.item_code
                 WHERE loc.loc_id IN (:cellIds)
                   AND loc.loc_group = :eqGroupId
                 ORDER BY loc.loc_id ASC, stk.inb_datetime DESC
                """;
        Map<String, Object> params = ValueUtil.newMap("cellIds,eqGroupId", cellIds, eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }
}
