package operato.logis.wcs.service.impl.query.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// 페이징/필터/정렬 공통 지원. flattened/aggregate tier 가 공유한다.
public abstract class AbstractPagedQuerySupport extends AbstractQueryService {

    // 컬럼명 형식 검증: 영문/숫자/언더스코어, 첫 글자 영문/언더스코어
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    // 서브클래스 이름으로 로깅
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ObjectMapper objectMapper;

    // 컬럼 값 종류. TIMESTAMP_AS_DATE 만 날짜 필터를 반열림 범위로 변환
    public enum ColumnKind { TEXT, TIMESTAMP_AS_DATE }

    // 화이트리스트 + 정규식 이중 검증
    protected boolean isAllowedColumn(String name, Set<String> allowed) {
        if (ValueUtil.isEmpty(name)) return false;
        if (!COLUMN_NAME_PATTERN.matcher(name).matches()) return false;
        return ValueUtil.isNotEmpty(allowed) && allowed.contains(name);
    }

    // query JSON 필터 배열 파싱. 실패 시 빈 리스트
    protected List<Map<String, Object>> parseFilters(String queryJson) {
        if (ValueUtil.isEmpty(queryJson)) return new ArrayList<>();
        try {
            return objectMapper.readValue(queryJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            logger.error("[ Query ][ Paged ] parseFilters failed - queryJson={}", queryJson, e);
            return new ArrayList<>();
        }
    }

    // sort JSON 배열 파싱. 실패 시 빈 리스트
    protected List<Map<String, Object>> parseSorters(String sortJson) {
        if (ValueUtil.isEmpty(sortJson)) return new ArrayList<>();
        try {
            return objectMapper.readValue(sortJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            logger.error("[ Query ][ Paged ] parseSorters failed - sortJson={}", sortJson, e);
            return new ArrayList<>();
        }
    }

    // 연산자 → WHERE 조각. columnRef 만 바꿔 inner(소스식)/outer(t.alias) 양쪽에서 호출
    protected void appendPredicate(StringBuilder where, String columnRef, String operator,
                                   Object value, ColumnKind kind, String p,
                                   Map<String, Object> params) {
        // 날짜를 반열림 일자 범위로 → 원본 timestamp 인덱스 sargable
        boolean dateRange = (kind == ColumnKind.TIMESTAMP_AS_DATE);

        switch (operator) {
            case "eq" -> {
                if (dateRange) {
                    where.append("AND ").append(columnRef).append(" >= CAST(:").append(p).append(" AS date)")
                         .append(" AND ").append(columnRef).append(" < CAST(:").append(p).append(" AS date) + INTERVAL '1 day' \n");
                } else {
                    where.append("AND ").append(columnRef).append(" = :").append(p).append(" \n");
                }
                params.put(p, value.toString());
            }
            case "like" -> {
                where.append("AND ").append(columnRef).append(" LIKE :").append(p).append(" \n");
                String likeVal = value.toString();
                if (!likeVal.contains("%")) likeVal = "%" + likeVal + "%";
                params.put(p, likeVal);
            }
            case "between" -> {
                String[] range = value.toString().split(",");
                if (range.length == 2) {
                    if (dateRange) {
                        where.append("AND ").append(columnRef).append(" >= CAST(:").append(p).append("_s AS date)")
                             .append(" AND ").append(columnRef).append(" < CAST(:").append(p).append("_e AS date) + INTERVAL '1 day' \n");
                    } else {
                        where.append("AND ").append(columnRef).append(" >= :").append(p).append("_s")
                             .append(" AND ").append(columnRef).append(" <= :").append(p).append("_e \n");
                    }
                    params.put(p + "_s", range[0].trim());
                    params.put(p + "_e", range[1].trim());
                }
            }
            case "in" -> {
                String[] inVals = value.toString().split(",");
                StringBuilder inClause = new StringBuilder("(");
                for (int i = 0; i < inVals.length; i++) {
                    String pn = p + "_" + i;
                    if (i > 0) inClause.append(",");
                    inClause.append(":").append(pn);
                    params.put(pn, inVals[i].trim());
                }
                inClause.append(")");
                where.append("AND ").append(columnRef).append(" IN ").append(inClause).append(" \n");
            }
            case "gt" -> { where.append("AND ").append(columnRef).append(" > :").append(p).append(" \n"); params.put(p, value.toString()); }
            case "gte", "ge" -> { where.append("AND ").append(columnRef).append(" >= :").append(p).append(" \n"); params.put(p, value.toString()); }
            case "lt" -> { where.append("AND ").append(columnRef).append(" < :").append(p).append(" \n"); params.put(p, value.toString()); }
            case "lte", "le" -> { where.append("AND ").append(columnRef).append(" <= :").append(p).append(" \n"); params.put(p, value.toString()); }
            case "ne", "neq" -> { where.append("AND ").append(columnRef).append(" != :").append(p).append(" \n"); params.put(p, value.toString()); }
            default -> logger.warn("[ Query ][ Paged ] unsupported operator - operator={}, column={}", operator, columnRef);
        }
    }

    // 정렬 절 — 화이트리스트 검증 후 t.alias. 비면 defaultOrder
    protected String buildOrder(List<Map<String, Object>> sorters, Set<String> allowed, String defaultOrder) {
        if (ValueUtil.isEmpty(sorters)) return defaultOrder;
        List<String> clauses = new ArrayList<>();
        for (Map<String, Object> s : sorters) {
            String field = (String) s.get("field");
            boolean asc = Boolean.TRUE.equals(s.get("ascending"));
            if (ValueUtil.isEmpty(field)) continue;
            if (!isAllowedColumn(field, allowed)) {
                logger.warn("[ Query ][ Security ] rejected sort column - service={}, field={}", getClass().getSimpleName(), field);
                continue;
            }
            clauses.add("t." + field + (asc ? " ASC" : " DESC"));
        }
        if (ValueUtil.isEmpty(clauses)) return defaultOrder;
        return " ORDER BY " + String.join(", ", clauses) + " ";
    }

    // 윈도우 카운트 단일 쿼리 실행 → {total, items}. _total 은 응답에서 제거
    protected Map<String, Object> executePagedQuery(String wrappedSql, Map<String, Object> params,
                                                     int page, int limit) {
        int offset = (page - 1) * limit;
        params.put("_limit", limit);
        params.put("_offset", offset);

        @SuppressWarnings("rawtypes")
        List<Map> records = this.queryManager.selectListBySql(wrappedSql, params, Map.class, 0, 0);

        int total = 0;
        if (ValueUtil.isNotEmpty(records)) {
            Object t = records.get(0).get("_total");
            if (t instanceof Number n) total = n.intValue();
            for (Map r : records) r.remove("_total");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("items", ValueUtil.isNotEmpty(records) ? records : new ArrayList<>());
        return result;
    }
}
