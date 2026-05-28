package operato.logis.wcs.common.util.lang;

import operato.logis.wcs.consts.WcsError;
import org.springframework.util.StringUtils;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;

import operato.logis.wcs.common.util.time.LocalDateUtils;

/**
 * REST 요청 파라미터 파싱 유틸 — 형식이 틀리면 INVALID_PARAMETER 예외를 던진다.
 *
 * CommonUtils 와 구분: CommonUtils 는 실패해도 기본값(0/""/null)을 조용히 반환하지만,
 * 본 유틸은 잘못된 입력을 명시적으로 거부한다 (외부 입력 검증 성격).
 */
public final class ParseUtils {

    private ParseUtils() {}

    /** "yyyy-MM-dd" 또는 앞 10자리. 비면 null, 형식 오류면 예외. */
    public static LocalDate parseDate(String s) {
        if (ValueUtil.isEmpty(s)) return null;
        try {
            return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
        } catch (DateTimeParseException e) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "date 형식 오류 (YYYY-MM-DD): " + s);
        }
    }

    /** "yyyy-MM-dd" → java.util.Date. 비면 null. */
    public static Date parseDateToDate(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : LocalDateUtils.toDate(d);
    }

    /** Object → Integer. 비면 null, 숫자/문자 허용, 형식 오류면 예외. */
    public static Integer parseIntOrNull(Object v) {
        if (ValueUtil.isEmpty(v)) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "숫자 형식 오류: " + v);
        }
    }
}