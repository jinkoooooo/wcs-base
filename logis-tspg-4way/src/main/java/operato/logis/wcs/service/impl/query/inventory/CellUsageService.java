package operato.logis.wcs.service.impl.query.inventory;

import operato.logis.wcs.service.impl.query.common.AbstractAggregatePagedService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 보관층(level) 별 셀 사용 현황 집계 서비스 (BUSINESS305).
 *
 * 집계 기준:
 *   - total_cells: drive_only_yn = false 인 모든 보관 셀
 *   - used_cells: is_enabled = true (활성 셀 — 재고/작업 유무 무관)
 *   - empty_cells: is_enabled = true && stock_id 없음 && task_id 없음 (빈 셀)
 *   - work_cells: is_enabled = true && task_id 있음 (작업 중)
 *   - forbid_cells: is_enabled = false (금지)
 *   - usage_rate: used_cells / total_cells × 100 (소수 2자리)
 *
 * 집계 단위: (eq_group_id, level). COALESCE(em.eq_group_id, r.eq_id) — CellStateService 와 동일.
 */
@Service
public class CellUsageService extends AbstractAggregatePagedService {

    private static final String INNER_SQL = """
        SELECT COALESCE(em.eq_group_id, r.eq_id) AS eq_group_id,
               r.level                            AS layer,
               COUNT(*)                           AS total_cells,
               COUNT(*) FILTER (
                   WHERE COALESCE(loc.is_enabled, true) = true
               ) AS used_cells,
               COUNT(*) FILTER (
                   WHERE NULLIF(loc.stock_id, '') IS NULL
                     AND NULLIF(loc.task_id,  '') IS NULL
                     AND COALESCE(loc.is_enabled, true) = true
               ) AS empty_cells,
               COUNT(*) FILTER (
                   WHERE NULLIF(loc.task_id, '') IS NOT NULL
                     AND COALESCE(loc.is_enabled, true) = true
               ) AS work_cells,
               COUNT(*) FILTER (
                   WHERE loc.is_enabled = false
               ) AS forbid_cells,
               CASE WHEN COUNT(*) = 0 THEN 0
                    ELSE ROUND(
                             COUNT(*) FILTER (
                                 WHERE COALESCE(loc.is_enabled, true) = true
                             ) * 10000.0 / COUNT(*)
                         ) / 100.0
               END AS usage_rate
          FROM tb_eq_rack_mst r
          LEFT JOIN tb_eq_mst em            ON em.id = r.eq_id
          LEFT JOIN tb_inventory_location loc ON loc.loc_id = r.id
         WHERE r.drive_only_yn = false
         /*__DYN_WHERE__*/
         GROUP BY COALESCE(em.eq_group_id, r.eq_id), r.level
        """;

    private static final String DEFAULT_ORDER = " ORDER BY t.eq_group_id ASC, t.layer ASC ";

    /** INNER_SQL SELECT alias 화이트리스트 — WHERE/ORDER BY 합성 시 검증. */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "eq_group_id", "layer",
            "total_cells", "used_cells", "empty_cells", "work_cells", "forbid_cells",
            "usage_rate"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 집계 키만 푸시. 셀 카운트 집계결과는 바깥 필터.
    @Override protected Map<String, PushdownColumn> pushdownColumns() {
        return Map.of(
                "eq_group_id", new PushdownColumn("COALESCE(em.eq_group_id, r.eq_id)", ColumnKind.TEXT),
                "layer",       new PushdownColumn("r.level", ColumnKind.TEXT));
    }

    /**
     * 사용 가능한 eqGroupId 목록 — 화면 좌측 필터 드롭다운용.
     */
    @Transactional(readOnly = true)
    public List<Map> getZones() {
        String sql = """
                SELECT DISTINCT COALESCE(em.eq_group_id, r.eq_id) AS eq_group_id
                  FROM tb_eq_rack_mst r
                  LEFT JOIN tb_eq_mst em ON em.id = r.eq_id
                 WHERE COALESCE(em.eq_group_id, r.eq_id) IS NOT NULL
                 ORDER BY 1 ASC
                """;
        return this.queryManager.selectListBySql(sql, new HashMap<>(), Map.class, 0, 0);
    }
}
