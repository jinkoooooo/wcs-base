package operato.logis.wcs.service.impl.query.common;

import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 단순 JOIN형 조회(최상위 GROUP BY 없음). Postgres 평탄화로 바깥 t.alias 필터가 베이스 인덱스를 탄다.
public abstract class AbstractFlattenedPagedService extends AbstractPagedQuerySupport {

    protected abstract String getInnerSql();
    protected abstract String getDefaultOrder();
    protected abstract Set<String> allowedColumns();

    // 표시용 ::date alias → 원본 timestamp alias. 기본 없음.
    // 매핑된 alias 로 들어온 날짜 필터를 원본 timestamp 기준 반열림 범위로 재작성.
    protected Map<String, String> dateColumns() { return Map.of(); }

    public Map<String, Object> search(String queryJson, String sortJson, int page, int limit) {
        return searchWithParams(queryJson, sortJson, page, limit, new HashMap<>());
    }

    // 추가 바인딩 파라미터(예: :stockType)를 미리 넣고 호출하는 확장점
    protected Map<String, Object> searchWithParams(String queryJson, String sortJson,
                                                   int page, int limit, Map<String, Object> params) {
        StringBuilder where = new StringBuilder();
        Map<String, String> dateCols = dateColumns();

        // query JSON → 바깥 WHERE
        List<Map<String, Object>> filters = parseFilters(queryJson);
        int idx = 0;
        for (Map<String, Object> filter : filters) {
            String name = (String) filter.get("name");
            String operator = (String) filter.getOrDefault("operator", "eq");
            Object value = filter.get("value");
            if (ValueUtil.isEmpty(name) || ValueUtil.isEmpty(value) || ValueUtil.isEmpty(value.toString())) continue;
            if (!isAllowedColumn(name, allowedColumns())) {
                logger.warn("[ Query ][ Security ] rejected column - service={}, name={}", getClass().getSimpleName(), name);
                continue;
            }
            // 날짜 매핑이면 원본 timestamp alias 기준 반열림, 아니면 t.alias 평문
            String rawAlias = dateCols.get(name);
            String columnRef = "t." + (rawAlias != null ? rawAlias : name);
            ColumnKind kind = (rawAlias != null) ? ColumnKind.TIMESTAMP_AS_DATE : ColumnKind.TEXT;
            appendPredicate(where, columnRef, operator, value, kind, "p" + idx++, params);
        }

        String order = buildOrder(parseSorters(sortJson), allowedColumns(), getDefaultOrder());

        String wrapped = "SELECT t.*, COUNT(*) OVER() AS _total FROM ("
                + getInnerSql() + ") t WHERE 1=1 \n" + where + order + " LIMIT :_limit OFFSET :_offset";

        return executePagedQuery(wrapped, params, page, limit);
    }
}
