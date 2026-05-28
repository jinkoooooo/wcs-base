package operato.logis.samsung.service.mw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GtrInspectionService extends AbstractQueryService {
    private static final Logger log = LoggerFactory.getLogger(GtrInspectionService.class);

    public Map<String, Object> getDashboardData(Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();

        // 파라미터 추출 (startDate, endDate, itemCode)
        String startDate = (String) params.getOrDefault("startDate", "");
        String endDate = (String) params.getOrDefault("endDate", "");

        // SQL 파라미터 세팅
        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("startDate", startDate);
        sqlParams.put("endDate", endDate);

        // ---------------------------------------------------------
        // 1. GTR 데이터 조회
        // ---------------------------------------------------------
        Map<String, Object> gtrData = new HashMap<>();

        // 1-1. GTR 요약 통계 (총 검사, 정상, 불량)
        String gtrStatsSql =
                "SELECT " +
                        "   COUNT(1) AS total, " +
                        "   SUM(CASE WHEN overall_result = 'Normal' THEN 1 ELSE 0 END) AS normal, " +
                        "   SUM(CASE WHEN overall_result = 'Damaged' THEN 1 ELSE 0 END) AS damaged " +
                        "FROM tb_mw_gtr_inspection_results " +
                        "WHERE timestamp >= CAST(:startDate AS TIMESTAMP) " +
                        "  AND timestamp <= CAST(:endDate AS TIMESTAMP)";

        Map<String, Object> gtrStats = this.queryManager.selectBySql(gtrStatsSql, sqlParams, Map.class);

        // 1-2. GTR 불량 사유 Top 3
        String gtrReasonsSql =
                "SELECT overall_reason AS name, COUNT(1) AS count " +
                        "FROM tb_mw_gtr_inspection_results " +
                        "WHERE overall_result = 'Damaged' " +
                        "  AND timestamp >= CAST(:startDate AS TIMESTAMP) " +
                        "  AND timestamp <= CAST(:endDate AS TIMESTAMP) " +
                        "GROUP BY overall_reason " +
                        "ORDER BY count DESC LIMIT 3";

        List<Map> gtrReasons = this.queryManager.selectListBySql(gtrReasonsSql, sqlParams, Map.class,0,0);
        if (gtrStats != null) {
            gtrStats.put("reasons", gtrReasons);
        }
        gtrData.put("stats", gtrStats != null ? gtrStats : new HashMap<>());

        // 1-3. GTR 상세 목록
        String gtrListSql =
                "SELECT " +
                        "   id, " +
                        "   TO_CHAR(timestamp, 'YYYY-MM-DD HH24:MI:SS') AS timestamp, " +
                        "   serial_numbers AS serial, " +
                        "   overall_result AS result, " +
                        "   overall_reason AS reason, " +
                        "   overall_confidence AS confidence " +
                        "FROM tb_mw_gtr_inspection_results " +
                        "WHERE timestamp >= CAST(:startDate AS TIMESTAMP) " +
                        "  AND timestamp <= CAST(:endDate AS TIMESTAMP) " +
                        "ORDER BY timestamp DESC " +
                        "LIMIT 500"; // 화면 성능을 위해 최근 500건 제한

        List<Map> gtrList = this.queryManager.selectListBySql(gtrListSql, sqlParams, Map.class, 0, 0);
        gtrData.put("list", gtrList);


        // ---------------------------------------------------------
        // 2. VISION 데이터 조회
        // ---------------------------------------------------------
        Map<String, Object> visionData = new HashMap<>();

        // 2-1. VISION 요약 통계 (result 컬럼 사용: 예 'OK', 'NG')
        String visionStatsSql =
                "SELECT " +
                        "   COUNT(1) AS total, " +
                        "   SUM(CASE WHEN result = '1' THEN 1 ELSE 0 END) AS ok, " +
                        "   SUM(CASE WHEN result = '0' THEN 1 ELSE 0 END) AS ng " +
                        "FROM tb_mw_vision_data " +
                        "WHERE reg_dt >= CAST(:startDate AS TIMESTAMP) " +
                        "  AND reg_dt <= CAST(:endDate AS TIMESTAMP) ";

        Map<String, Object> visionStats = this.queryManager.selectBySql(visionStatsSql, sqlParams, Map.class);
        visionData.put("stats", visionStats != null ? visionStats : new HashMap<>());

        // 2-2. VISION 상세 목록
        String visionListSql =
                "SELECT " +
                        "   id, " +
                        "   TO_CHAR(reg_dt, 'YYYY-MM-DD HH24:MI:SS') AS timestamp, " +
                        "   seqno AS serial, " +
                        "   result, " +
                        "   result_code AS reason " +
                        "FROM tb_mw_vision_data " +
                        "WHERE reg_dt >= CAST(:startDate AS TIMESTAMP) " +
                        "  AND reg_dt <= CAST(:endDate AS TIMESTAMP) " +
                        "ORDER BY reg_dt DESC " +
                        "LIMIT 500";

        List<Map> visionList = this.queryManager.selectListBySql(visionListSql, sqlParams, Map.class,0,0);
        visionData.put("list", visionList);

        // 최종 응답 조합
        response.put("gtr", gtrData);
        response.put("vision", visionData);

        return response;
    }
}