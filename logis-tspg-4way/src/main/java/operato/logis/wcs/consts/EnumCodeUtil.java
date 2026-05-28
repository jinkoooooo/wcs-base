package operato.logis.wcs.consts;

import java.util.List;

import xyz.elidom.util.ValueUtil;

/**
 * EnumCode 공통 해석 헬퍼. 코드·별칭(alias) 매칭으로 enum 상수를 찾는다.
 */
public final class EnumCodeUtil {

    private EnumCodeUtil() {}

    /**
     * 코드 또는 별칭으로 enum 상수를 해석한다. 대소문자는 무시한다.
     * Integer 코드 일치 → 코드 문자열 일치 → 별칭 일치 순으로 탐색. 미일치 시 null.
     */
    public static <E extends Enum<E> & EnumCode> E fromCodeOrNull(Class<E> enumType, Object code) {
        if (ValueUtil.isEmpty(code)) return null;

        // 입력을 문자열·정수 양쪽으로 준비 (정수 변환 실패는 정상 — 문자열 매칭으로 진행)
        String inStr = String.valueOf(code);
        Integer inInt = null;
        try {
            inInt = Integer.parseInt(inStr);
        } catch (NumberFormatException ignored) {
            // 문자열 코드 enum — 정수 매칭 건너뜀
        }

        for (E e : enumType.getEnumConstants()) {
            Object ec = e.code();
            // Integer 코드 일치
            if (ValueUtil.isNotEmpty(inInt) && ec instanceof Integer && inInt.equals(ec)) {
                return e;
            }
            // 코드 문자열 일치
            if (ValueUtil.isNotEmpty(ec) && equalsStr(String.valueOf(ec), inStr)) {
                return e;
            }
            // 별칭 일치
            List<String> aliases = e.aliases();
            if (ValueUtil.isNotEmpty(aliases)) {
                for (String a : aliases) {
                    if (ValueUtil.isNotEmpty(a) && equalsStr(a, inStr)) {
                        return e;
                    }
                }
            }
        }
        return null;
    }

    /** null·빈 값 안전 대소문자 무시 문자열 비교. */
    private static boolean equalsStr(String a, String b) {
        if (ValueUtil.isEmpty(a) || ValueUtil.isEmpty(b)) return false;
        return a.equalsIgnoreCase(b);
    }
}
