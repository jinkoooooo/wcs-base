package operato.logis.samsung.utils.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiRegexUtils {

    private AiRegexUtils() {
    }

    private static final Pattern SKU_BY_KEYWORD =
            Pattern.compile("(?i)(sku|상품코드|item\\s*code)\\s*[:：]?\\s*([A-Z0-9_-]+)");

    private static final Pattern SKU_TOKEN =
            Pattern.compile("(?i)\\b(SKU[A-Z0-9_-]+)\\b");

    private static final Pattern SERIAL_BY_KEYWORD =
            Pattern.compile("(?i)(시리얼|serial|상품고유번호)\\s*[:：]?\\s*([A-Z0-9_-]+)");

    private static final Pattern ERROR_BY_KEYWORD =
            Pattern.compile("(?i)(에러코드|알람코드|error\\s*code|alarm\\s*code)\\s*[:：]?\\s*([A-Z]{1,5}-?\\d{2,5})");

    private static final Pattern ERROR_TOKEN =
            Pattern.compile("\\b([A-Z]{1,5}-?\\d{2,5})\\b");

    public static String extractSkuCode(String question) {
        if (question == null) return null;

        Matcher byKeyword = SKU_BY_KEYWORD.matcher(question);
        if (byKeyword.find()) {
            return byKeyword.group(2).toUpperCase();
        }

        Matcher token = SKU_TOKEN.matcher(question);
        if (token.find()) {
            return token.group(1).toUpperCase();
        }

        return null;
    }

    public static String extractSerialNo(String question) {
        if (question == null) return null;

        Matcher byKeyword = SERIAL_BY_KEYWORD.matcher(question);
        if (byKeyword.find()) {
            return byKeyword.group(2).toUpperCase();
        }

        return null;
    }

    public static String extractErrorCode(String question) {
        if (question == null) return null;

        Matcher byKeyword = ERROR_BY_KEYWORD.matcher(question);
        if (byKeyword.find()) {
            return byKeyword.group(2).toUpperCase();
        }

        if (containsAny(question.toLowerCase(), "에러", "알람", "장애", "조치", "가이드")) {
            Matcher token = ERROR_TOKEN.matcher(question.toUpperCase());
            if (token.find()) {
                String value = token.group(1).toUpperCase();
                if (!value.startsWith("SKU")) {
                    return value;
                }
            }
        }

        return null;
    }

    public static boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}