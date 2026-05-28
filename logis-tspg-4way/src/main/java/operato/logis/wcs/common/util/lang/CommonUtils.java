package operato.logis.wcs.common.util.lang;

import xyz.elidom.util.ValueUtil;

// 공통 값 변환 유틸 - null/empty 안전 처리
public class CommonUtils {

    private CommonUtils() {}

    // Integer → int (null/empty 면 0)
    public static int nz(Integer v) {
        return ValueUtil.isEmpty(v) ? 0 : v;
    }

    // String → String (null/empty 면 "")
    public static String nullToEmpty(String s) {
        return ValueUtil.isEmpty(s) ? "" : s;
    }

    // String → String (null/empty 면 fallback 반환) - 정렬용 기본값 처리에 사용
    public static String orDefault(String s, String fallback) {
        return ValueUtil.isEmpty(s) ? fallback : s;
    }

    // Object → int (null/숫자아님 → 0)
    public static int toInt(Object v) {
        if (ValueUtil.isEmpty(v)) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    // Object → trim 한 String (null 이면 null) - UI Map 페이로드 파싱용
    public static String toTrimmedString(Object v) {
        return v == null ? null : v.toString().trim();
    }

    // Object → String (null/empty 면 "") - String.valueOf 래핑
    public static String stringOf(Object v) {
        return ValueUtil.isEmpty(v) ? "" : String.valueOf(v);
    }

    // Object → trim 한 String (null/공백 이면 fallback)
    public static String strOr(Object v, String fallback) {
        if (ValueUtil.isEmpty(v)) return fallback;
        String s = v.toString().trim();
        return ValueUtil.isEmpty(s) ? fallback : s;
    }
}
