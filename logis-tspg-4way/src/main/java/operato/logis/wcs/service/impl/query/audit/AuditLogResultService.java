package operato.logis.wcs.service.impl.query.audit;

import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 감사 이력 조회 서비스.
 *
 * tb_wcs_audit_log 건별 이력을 반환한다.
 * 공통 페이징/필터/정렬 로직은 AbstractFlattenedPagedService 참고.
 */
@Service
public class AuditLogResultService extends AbstractFlattenedPagedService {

    /** PDF 보고서 단일 출력 최대 행수. */
    private static final int REPORT_MAX_ROWS = 5000;

    /** 건별 SQL — SELECT alias 가 메뉴 메타 컬럼 name 과 일치해야 한다. */
    private static final String INNER_SQL = """
        SELECT a.id,
               a.created_at,
               a.created_at::date AS audit_date,
               a.actor_type,
               a.actor_id,
               a.actor_name,
               a.channel,
               a.action,
               a.entity_class,
               a.table_name,
               a.pk_value,
               a.changed_columns,
               a.before_json,
               a.after_json,
               a.caller,
               a.reason
          FROM tb_wcs_audit_log a
        """;

    private static final String DEFAULT_ORDER = " ORDER BY t.created_at DESC ";

    /** INNER_SQL SELECT alias 화이트리스트 (필터/정렬 허용 컬럼). */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "id", "created_at", "audit_date",
            "actor_type", "actor_id", "actor_name", "channel",
            "action", "entity_class", "table_name", "pk_value",
            "changed_columns", "caller", "reason"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 표시용 audit_date(::date) → 원본 timestamp created_at (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of("audit_date", "created_at");
    }

    /** PDF 보고서용 — 동일 필터/정렬로 전체 행을 조회 (페이징 없이 상한까지). */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listForReport(String queryJson, String sortJson) {
        Map<String, Object> result = search(queryJson, sortJson, 1, REPORT_MAX_ROWS);
        Object items = result.get("items");
        return (items instanceof List<?>) ? (List<Map<String, Object>>) items : new ArrayList<>();
    }
}
