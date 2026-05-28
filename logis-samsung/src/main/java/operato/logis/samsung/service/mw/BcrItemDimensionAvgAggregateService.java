package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BcrItemDimensionAvgAggregateService extends AbstractQueryService {

    private final IQueryManager queryManager;

    /**
     * Logger
     */
    protected Logger logger = LoggerFactory.getLogger(BcrItemDimensionAvgAggregateService.class);

    /**
     * 최근 N일(기본 7일) 데이터로 평균 집계 후 마스터에 UPSERT
     * @return 집계 대상 item_code 수(=upsert rows)
     */
    public AggregateResult aggregateLastDays(int periodDays) {
        if (periodDays <= 0) periodDays = 7;

        // 1. 이상치 먼저 기록
        int outlierCnt = insertOutliersByLastDays(periodDays);

        int targetCnt = countTargets(periodDays);
        int upsertCnt = upsertByLastDays(periodDays);

        logger.info("[BCR-AVG] aggregate done. days={}, targets={}, upsertCnt={}, outliers={}"
                , periodDays, targetCnt, upsertCnt, outlierCnt);
        return new AggregateResult(periodDays, targetCnt, upsertCnt, outlierCnt);
    }

    private int countTargets(int periodDays) {
        String sql =
                "SELECT COUNT(DISTINCT item_code) " +
                        "FROM samsung_mw.tb_mw_bcr_data " +
                        "WHERE item_code IS NOT NULL " +
                        "  AND reg_dt >= NOW() - (:periodDays || ' days')::interval";

        Map<String, Object> p = new HashMap<>();
        p.put("periodDays", periodDays);

        Integer v = queryManager.selectBySql(sql, p, Integer.class);
        return v == null ? 0 : v;
    }

    /**
     * 단일 SQL UPSERT
     * - 신규: UUID 생성하여 insert
     * - 기존: UPDATE
     */
    private int upsertByLastDays(int periodDays) {
        String sql =
                "INSERT INTO samsung_mw.tb_mw_bcr_item_dimension_avg ( " +
                        "  id, inner_item_code, item_code, " +
                        "  avg_length_mm, avg_width_mm, avg_height_mm, " +
                        "  sample_cnt, last_scan_dt, last_calc_dt, " +
                        "  domain_id, creator_id, updater_id, created_at, updated_at " +
                        ") " +
                        "SELECT " +
                        "  gen_random_uuid()::text                       AS id, " +
                        "  d.item_code                                   AS inner_item_code, " +   // 88~ 내부코드
                        "  m.item_code                                   AS item_code, " +         // 상품마스터 item_code
                        "  ROUND(AVG(d.length))::int                     AS avg_length_mm, " +
                        "  ROUND(AVG(d.width))::int                      AS avg_width_mm, " +
                        "  ROUND(AVG(d.height))::int                     AS avg_height_mm, " +
                        "  COUNT(*)::int                                 AS sample_cnt, " +
                        "  MAX(d.reg_dt)::date                           AS last_scan_dt, " +
                        "  NOW()::date                                   AS last_calc_dt, " +
                        "  7                                             AS domain_id, " +
                        "  'SYS'                                         AS creator_id, " +
                        "  'SYS'                                         AS updater_id, " +
                        "  NOW()                                         AS created_at, " +
                        "  NOW()                                         AS updated_at " +
                        "FROM samsung_mw.tb_mw_bcr_data d " +
                        "INNER JOIN ( " +
                        "  SELECT inner_item_code, MAX(item_code) AS item_code " +
                        "  FROM samsung_mw.tb_mw_item_master " +
                        "  GROUP BY inner_item_code " +
                        ") m ON m.inner_item_code = d.item_code " +
                        "WHERE d.item_code IS NOT NULL " +
                        "  AND btrim(d.item_code) <> '' " +
                        "  AND d.reg_dt >= NOW() - (:periodDays * INTERVAL '1 day') " +
                        "GROUP BY d.item_code, m.item_code " +
                        "ON CONFLICT (item_code) DO UPDATE SET " +
                        "  inner_item_code = EXCLUDED.inner_item_code, " +
                        "  avg_length_mm   = EXCLUDED.avg_length_mm, " +
                        "  avg_width_mm    = EXCLUDED.avg_width_mm, " +
                        "  avg_height_mm   = EXCLUDED.avg_height_mm, " +
                        "  sample_cnt      = EXCLUDED.sample_cnt, " +
                        "  last_scan_dt    = EXCLUDED.last_scan_dt, " +
                        "  last_calc_dt    = EXCLUDED.last_calc_dt, " +
                        "  domain_id       = EXCLUDED.domain_id, " +
                        "  updater_id      = EXCLUDED.updater_id, " +
                        "  updated_at      = NOW() ";

        Map<String, Object> p = new HashMap<>();
        p.put("periodDays", periodDays);

        return queryManager.executeBySql(sql, p);
    }

    private int insertOutliersByLastDays(int periodDays) {
        String sql =
                "INSERT INTO samsung_mw.tb_mw_bcr_data_outlier ( " +
                        "  id, bcr_data_id, inner_item_code, item_code, barcodedata, " +
                        "  length_mm, width_mm, height_mm, " +
                        "  master_length_mm, master_width_mm, master_height_mm, " +
                        "  diff_length_mm, diff_width_mm, diff_height_mm, " +
                        "  reg_dt, detected_at, resolved_yn, " +
                        "  domain_id, creator_id, updater_id, created_at, updated_at " +
                        ") " +
                        "SELECT " +
                        "  gen_random_uuid()::text, " +
                        "  d.id, " +
                        "  d.item_code, " +
                        "  m.item_code, " +
                        "  d.barcodedata, " +
                        "  d.length, d.width, d.height, " +
                        "  m.item_length, m.item_width, m.item_height, " +
                        "  d.length - m.item_length, " +
                        "  d.width  - m.item_width, " +
                        "  d.height - m.item_height, " +
                        "  d.reg_dt, NOW(), 'N', " +
                        "  7, 'SYS', 'SYS', NOW(), NOW() " +
                        "FROM samsung_mw.tb_mw_bcr_data d " +
                        "JOIN samsung_mw.tb_mw_item_master m " +
                        "  ON m.inner_item_code = d.item_code " +
                        "WHERE d.item_code IS NOT NULL " +
                        "  AND btrim(d.item_code) <> '' " +
                        "  AND d.reg_dt >= NOW() - (:periodDays * INTERVAL '1 day') " +
                        "  AND ( " +
                        "       ABS(d.length - m.item_length) >= 20 " +
                        "    OR ABS(d.width  - m.item_width)  >= 20 " +
                        "    OR ABS(d.height - m.item_height) >= 20 " +
                        "  ) " +
                        "ON CONFLICT (bcr_data_id) DO NOTHING";

        Map<String, Object> p = Map.of("periodDays", periodDays);
        return queryManager.executeBySql(sql, p);
    }

    public record AggregateResult(int periodDays, int targetCnt, int upsertCnt, int outlierCnt) {}
}
