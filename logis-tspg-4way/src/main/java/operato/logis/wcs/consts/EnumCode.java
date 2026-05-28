package operato.logis.wcs.consts;

import java.util.List;

import xyz.elidom.util.ValueUtil;

/**
 * 코드값(String/Integer) 과 설명을 갖는 WCS enum 공통 계약.
 */
public interface EnumCode {
    Object code();
    String desc();

    /** 외부 시스템이 보내는 동의어 코드. 기본은 없음, 필요한 enum 만 override. */
    default List<String> aliases() { return List.of(); }

    /** 코드를 문자열로. null 이면 null. */
    default String codeAsString() {
        Object c = code();
        return c == null ? null : String.valueOf(c);
    }

    /** 코드를 Integer 로. 정수 변환 불가 시 null. */
    default Integer codeAsIntOrNull() {
        Object c = code();
        if (ValueUtil.isEmpty(c)) return null;
        if (c instanceof Integer i) return i;
        try {
            return Integer.parseInt(String.valueOf(c));
        } catch (NumberFormatException e) {
            // 정수 코드가 아닌 enum — null 반환이 정상 흐름
            return null;
        }
    }

    /** 코드 일치 여부 — codeAsString() 대소문자 무시 비교. 한쪽이라도 null 이면 false. */
    default boolean matches(Object other) {
        String self = codeAsString();
        return self != null && other != null && self.equalsIgnoreCase(String.valueOf(other));
    }
}
