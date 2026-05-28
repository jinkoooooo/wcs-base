package operato.logis.wcs.service.impl.query.common;

import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 최상위 GROUP BY형 조회. 집계 전 베이스 컬럼 필터를 inner WHERE 로 푸시다운.
public abstract class AbstractAggregatePagedService extends AbstractPagedQuerySupport {

    // inner SQL 의 GROUP BY 앞 주입 지점
    protected static final String DYNAMIC_PREDICATE_MARKER = "/*__DYN_WHERE__*/";

    // 집계 전 푸시 가능한 베이스 컬럼 매핑(소스식 + 종류)
    public record PushdownColumn(String sourceExpr, ColumnKind kind) {}

    protected abstract String getInnerSql();
    protected abstract String getDefaultOrder();
    protected abstract Set<String> allowedColumns();
    protected abstract Map<String, PushdownColumn> pushdownColumns();

    public Map<String, Object> search(String queryJson, String sortJson, int page, int limit) {
        String innerSql = getInnerSql();
        // 마커 누락 → fail fast(계약 위반 즉시 노출)
        if (!innerSql.contains(DYNAMIC_PREDICATE_MARKER)) {
            throw new IllegalStateException(getClass().getSimpleName() + " inner SQL missing " + DYNAMIC_PREDICATE_MARKER);
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder innerWhere = new StringBuilder();   // 집계 전 푸시 술어
        StringBuilder outerWhere = new StringBuilder();   // 집계 결과 컬럼 필터
        Map<String, PushdownColumn> pushdown = pushdownColumns();

        List<Map<String, Object>> filters = parseFilters(queryJson);
        int idx = 0;
        for (Map<String, Object> filter : filters) {
            String name = (String) filter.get("name");
            String operator = (String) filter.getOrDefault("operator", "eq");
            Object value = filter.get("value");
            if (ValueUtil.isEmpty(name) || ValueUtil.isEmpty(value) || ValueUtil.isEmpty(value.toString())) continue;

            String p = "p" + idx++;
            PushdownColumn pc = pushdown.get(name);
            if (pc != null) {
                // 집계 전 소스식으로 주입
                appendPredicate(innerWhere, pc.sourceExpr(), operator, value, pc.kind(), p, params);
            } else if (isAllowedColumn(name, allowedColumns())) {
                // 집계 결과 컬럼 → 바깥 필터
                appendPredicate(outerWhere, "t." + name, operator, value, ColumnKind.TEXT, p, params);
            } else {
                logger.warn("[ Query ][ Security ] rejected column - service={}, name={}", getClass().getSimpleName(), name);
            }
        }

        String injectedInner = innerSql.replace(DYNAMIC_PREDICATE_MARKER, innerWhere.toString());
        String order = buildOrder(parseSorters(sortJson), allowedColumns(), getDefaultOrder());

        String wrapped = "SELECT t.*, COUNT(*) OVER() AS _total FROM ("
                + injectedInner + ") t WHERE 1=1 \n" + outerWhere + order + " LIMIT :_limit OFFSET :_offset";

        return executePagedQuery(wrapped, params, page, limit);
    }
}
