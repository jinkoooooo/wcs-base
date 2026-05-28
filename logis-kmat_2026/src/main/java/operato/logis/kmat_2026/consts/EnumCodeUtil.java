package operato.logis.kmat_2026.consts;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EnumCode;

import java.util.*;
import java.util.function.Function;

/**
 * 코드 매핑 유틸
 * - String/Int 모두 매칭
 * - 대소문자 무시 옵션
 * - 별칭(alias)도 등록 가능
 */
public final class EnumCodeUtil {

    private EnumCodeUtil() {}

    public static <E extends Enum<E> & operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EnumCode> E fromCodeOrNull(
            Class<E> enumType,
            Object code,
            boolean ignoreCase,
            Function<E, Collection<String>> aliasProvider
    ) {
        if (code == null) return null;

        String inStr = String.valueOf(code);
        Integer inInt = null;
        try { inInt = Integer.parseInt(inStr); } catch (Exception ignored) {}

        for (E e : enumType.getEnumConstants()) {
            Object ec = e.code();

            // 1) int 매칭
            if (inInt != null && ec instanceof Integer && inInt.equals(ec)) {
                return e;
            }

            // 2) string 매칭
            if (ec != null) {
                String ecStr = String.valueOf(ec);
                if (equalsStr(ecStr, inStr, ignoreCase)) {
                    return e;
                }
            }

            // 3) alias 매칭
            if (aliasProvider != null) {
                Collection<String> aliases = aliasProvider.apply(e);
                if (aliases != null) {
                    for (String a : aliases) {
                        if (a != null && equalsStr(a, inStr, ignoreCase)) {
                            return e;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static <E extends Enum<E> & EnumCode> E fromCodeOrThrow(
            Class<E> enumType,
            Object code,
            boolean ignoreCase,
            Function<E, Collection<String>> aliasProvider,
            String errorMessage
    ) {
        E r = fromCodeOrNull(enumType, code, ignoreCase, aliasProvider);
        if (r != null) return r;
        throw new IllegalArgumentException(errorMessage + " (code=" + code + ", enum=" + enumType.getSimpleName() + ")");
    }

    private static boolean equalsStr(String a, String b, boolean ignoreCase) {
        if (a == null || b == null) return false;
        return ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);
    }
}