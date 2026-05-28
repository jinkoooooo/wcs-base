package operato.logis.wcs.common.util.time;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * LocalDate ↔ java.util.Date 변환.
 *
 * Elidom ORM (next-dbist) 가 java.time.LocalDate 를 ResultSet 매핑하지 못해
 * 엔티티 필드는 java.util.Date 로만 둔다. 호출 코드는 LocalDate 를 그대로 쓰고
 * 엔티티 경계에서만 본 유틸로 변환한다.
 *
 * 기준 타임존: Asia/Seoul (서버 운영 TZ).
 */
public final class LocalDateUtils {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private LocalDateUtils() {}

    /** LocalDate → Date (KST 자정 기준). null-safe. */
    public static Date toDate(LocalDate d) {
        return d == null ? null : Date.from(d.atStartOfDay(KST).toInstant());
    }

    /** Date → LocalDate (KST 기준). null-safe. */
    public static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toInstant().atZone(KST).toLocalDate();
    }
}
