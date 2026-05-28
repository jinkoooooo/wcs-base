package operato.logis.wcs.service.impl.scheduling;

import org.springframework.stereotype.Component;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.toInt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * loc_code → 3D 좌표 (row, bay, level) 해석기.
 *
 * tb_inventory_location 의 (loc_row, loc_col, loc_level) 사용.
 * DB 미스 시 loc_code 자체를 파싱하는 fallback 으로 (0,0,0) 반환 → GA fitness 가 망가지지 않게 한다.
 */
@Component
public class LocationResolver extends AbstractQueryService {

    /**
     * 단건 해석 — 내부적으로 resolveBatch 위임.
     */
    public int[] resolve(String locGroup, String locCode) {
        if (ValueUtil.isEmpty(locCode)) return zero();
        Map<String, int[]> single = resolveBatch(locGroup, Collections.singletonList(locCode));
        return single.getOrDefault(locCode, parseFallback(locCode));
    }

    /**
     * 다건 해석 — locGroup 스코프로 DB 1회 SELECT.
     */
    public Map<String, int[]> resolveBatch(String locGroup, Collection<String> locCodes) {
        Map<String, int[]> result = new HashMap<>();
        if (ValueUtil.isEmpty(locCodes)) return result;

        // 입력 distinct 정제
        Set<String> distinct = new HashSet<>();
        for (String c : locCodes) {
            if (ValueUtil.isNotEmpty(c)) distinct.add(c);
        }
        if (ValueUtil.isEmpty(distinct)) return result;

        // DB 조회
        String sql = """
            SELECT loc_id, loc_row, loc_col, loc_level
              FROM tb_inventory_location
             WHERE loc_group = :locGroup
               AND loc_id    IN (:locIds)
            """;
        Map<String, Object> params = ValueUtil.newMap("locGroup,locIds", locGroup, distinct);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        for (Map row : rows) {
            String locId = (String) row.get("loc_id");
            if (ValueUtil.isEmpty(locId)) continue;
            result.put(locId, new int[]{
                    toInt(row.get("loc_row")),
                    toInt(row.get("loc_col")),
                    toInt(row.get("loc_level"))
            });
        }
        return result;
    }

    /**
     * DB 미스 시 locCode 자체 파싱 — "01-02-03" 또는 "010203" 형식 인식.
     */
    public int[] parseFallback(String locCode) {
        if (ValueUtil.isEmpty(locCode)) return zero();
        try {
            // 하이픈 구분 형식
            if (locCode.contains("-")) {
                String[] parts = locCode.split("-");
                return new int[]{
                        Integer.parseInt(parts[0]),
                        parts.length > 1 ? Integer.parseInt(parts[1]) : 0,
                        parts.length > 2 ? Integer.parseInt(parts[2]) : 0
                };
            }
            // 6자리 숫자 형식
            if (locCode.length() >= 6 && locCode.matches("\\d+")) {
                return new int[]{
                        Integer.parseInt(locCode.substring(0, 2)),
                        Integer.parseInt(locCode.substring(2, 4)),
                        Integer.parseInt(locCode.substring(4, 6))
                };
            }
        } catch (NumberFormatException ignored) {
        }
        return zero();
    }

    private static int[] zero() {
        return new int[]{0, 0, 0};
    }

}
