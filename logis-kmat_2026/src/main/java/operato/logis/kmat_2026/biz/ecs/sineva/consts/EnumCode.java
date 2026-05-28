package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * 모든 코드성 Enum의 공통 인터페이스
 *
 * 규칙:
 * - C: code 타입 (String, Integer 등)
 * - code: DB/API/외부 연동에서 사용하는 실제 값
 * - desc: 사람이 읽는 설명
 *
 * 장점:
 * - String, Integer 모두 지원
 * - getCode()/getDesc()도 기본 지원하여 기존 코드와 호환 가능
 * - 형변환 없이 타입 안전하게 사용 가능
 */
public interface EnumCode<C> {

    /**
     * 실제 코드값
     */
    C code();

    /**
     * 코드 설명
     */
    String desc();

    /**
     * 기존 getter 스타일 호환용
     */
    default C getCode() {
        return code();
    }

    /**
     * 기존 getter 스타일 호환용
     */
    default String getDesc() {
        return desc();
    }

    /**
     * code를 문자열로 변환
     */
    default String codeAsString() {
        C c = code();
        return c == null ? null : String.valueOf(c);
    }

    /**
     * code를 Integer로 변환 시도
     * - 변환 불가 시 null 반환
     */
    default Integer codeAsIntOrNull() {
        C c = code();
        if (c == null) return null;
        if (c instanceof Integer) return (Integer) c;
        try {
            return Integer.parseInt(String.valueOf(c));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 입력값과 code 비교
     * - 숫자/문자 혼합 비교 허용
     */
    default boolean is(Object value) {
        C c = code();

        if (value == null && c == null) return true;
        if (value == null || c == null) return false;

        if (c instanceof Integer) {
            Integer inputInt = null;
            try {
                inputInt = Integer.parseInt(String.valueOf(value));
            } catch (Exception ignored) {
            }
            return inputInt != null && c.equals(inputInt);
        }

        return String.valueOf(c).equals(String.valueOf(value));
    }
}