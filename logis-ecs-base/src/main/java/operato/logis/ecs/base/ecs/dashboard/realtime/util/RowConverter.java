package operato.logis.ecs.base.ecs.dashboard.realtime.util;

import java.util.Map;

/**
 * Provider 들이 raw SQL 결과 Map 에서 값을 꺼낼 때 쓰는 공용 변환 유틸.
 * ConveyorDataProvider / RackInventoryDataProvider 에 흩어져 있던 동일 구현을 통합.
 * 전부 null-safe static 메서드.
 */
public final class RowConverter {

    private RowConverter() {
    }

    /** null / 빈 문자열(공백만) 이 아니면 true. */
    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Object → String. null 은 null 반환. */
    public static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    /** Object → int. Number 면 intValue, String 이면 parse 시도, 실패 시 defaultValue. */
    public static int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Object → double. 나머지는 toInt 와 동일한 규칙. */
    public static double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Object → boolean.
     * - Boolean → 그대로
     * - Number → 0 이 아니면 true
     * - String → "true" / "1" / "Y" / "YES" (대소문자 무시) 는 true
     * - 그 외 / null → false
     */
    public static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        String s = value.toString();
        return "true".equalsIgnoreCase(s)
                || "1".equals(s)
                || "Y".equalsIgnoreCase(s)
                || "YES".equalsIgnoreCase(s);
    }

    /**
     * 여러 alias 키로 Map 에서 값을 꺼냄.
     * 정확히 일치하는 키 우선, 없으면 대소문자 무시 매칭.
     * raw SQL 결과의 컬럼 케이스(cargoYn / cargoyn / cargo_yn) 를 한 번에 흡수.
     */
    public static Object getValue(Map<?, ?> row, String... keys) {
        if (row == null || keys == null) return null;

        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }

        for (Object objKey : row.keySet()) {
            if (objKey == null) continue;
            String actualKey = String.valueOf(objKey);
            for (String expected : keys) {
                if (actualKey.equalsIgnoreCase(expected)) {
                    return row.get(objKey);
                }
            }
        }

        return null;
    }

    /** getValue + toStringValue 합성 shortcut. */
    public static String getString(Map<?, ?> row, String... keys) {
        return toStringValue(getValue(row, keys));
    }
}
