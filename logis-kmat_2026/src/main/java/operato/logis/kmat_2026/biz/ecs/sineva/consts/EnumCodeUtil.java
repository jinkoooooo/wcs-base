package operato.logis.kmat_2026.biz.ecs.sineva.consts;

import java.util.Collection;
import java.util.function.Function;

/**
 * EnumCode 조회 유틸
 *
 * 지원 기능:
 * - Integer / String 코드 조회
 * - 대소문자 무시 옵션
 * - alias 조회 지원
 */
public final class EnumCodeUtil {

    private EnumCodeUtil() {
    }

    public static <C, E extends Enum<E> & EnumCode<C>> E fromCodeOrNull(
            Class<E> enumType,
            Object code,
            boolean ignoreCase,
            Function<E, Collection<String>> aliasProvider
    ) {
        if (code == null) return null;

        String inputStr = String.valueOf(code);
        Integer inputInt = null;

        try {
            inputInt = Integer.parseInt(inputStr);
        } catch (Exception ignored) {
        }

        for (E e : enumType.getEnumConstants()) {
            C enumCode = e.code();

            // Integer 직접 비교
            if (inputInt != null && enumCode instanceof Integer && inputInt.equals(enumCode)) {
                return e;
            }

            // String 비교
            if (enumCode != null) {
                String enumCodeStr = String.valueOf(enumCode);
                if (equalsStr(enumCodeStr, inputStr, ignoreCase)) {
                    return e;
                }
            }

            // alias 비교
            if (aliasProvider != null) {
                Collection<String> aliases = aliasProvider.apply(e);
                if (aliases != null) {
                    for (String alias : aliases) {
                        if (alias != null && equalsStr(alias, inputStr, ignoreCase)) {
                            return e;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static <C, E extends Enum<E> & EnumCode<C>> E fromCodeOrThrow(
            Class<E> enumType,
            Object code,
            boolean ignoreCase,
            Function<E, Collection<String>> aliasProvider,
            String errorMessage
    ) {
        E result = fromCodeOrNull(enumType, code, ignoreCase, aliasProvider);
        if (result != null) {
            return result;
        }

        throw new IllegalArgumentException(
                errorMessage + " (code=" + code + ", enum=" + enumType.getSimpleName() + ")"
        );
    }

    private static boolean equalsStr(String a, String b, boolean ignoreCase) {
        if (a == null || b == null) return false;
        return ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);
    }
}