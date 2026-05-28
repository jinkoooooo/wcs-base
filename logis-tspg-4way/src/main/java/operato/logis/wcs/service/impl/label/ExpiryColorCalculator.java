package operato.logis.wcs.service.impl.label;

import operato.logis.wcs.consts.ExpiryStatus;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사용기한 색상/상태 계산기.
 *
 * 입력: 생산일자 + 자재 마스터의 expiry_days.
 * 출력: ExpiryStatus 코드, 잔여일수(daysToExpiry), 사용기한 날짜(expiryDate).
 *
 * 임계값:
 *   - D-180 (6개월) 이상 → NORMAL
 *   - D-90  (3개월) 이상 D-180 미만 → WARN
 *   - D-0 이상 D-90 미만 → ALERT
 *   - D < 0 → EXPIRED
 *
 * 읽기 전용 — 상태 없음, 트랜잭션 없음.
 */
@Component
public class ExpiryColorCalculator {

    /** D-180 이상이면 NORMAL. */
    public static final int WARN_DAYS = 180;
    /** D-90 이상이면 ALERT 미만, 그 아래는 ALERT. */
    public static final int ALERT_DAYS = 90;

    /**
     * 생산일자 + 사용기한일수 → ExpiryStatus + daysToExpiry + expiryDate.
     * 응답 Map 의 키는 JSON 페이로드에 그대로 노출 가능하다.
     *
     * @param produceDate 생산일자 (null 이면 UNKNOWN).
     * @param expiryDays  자재 마스터의 사용기한 일수 (null/0/음수면 UNKNOWN).
     */
    public Map<String, Object> compute(Date produceDate, Integer expiryDays) {
        // 입력 미상 — UNKNOWN
        if (ValueUtil.isEmpty(produceDate) || ValueUtil.isEmpty(expiryDays) || expiryDays <= 0) {
            return unknown();
        }

        // 잔여일수 계산
        LocalDate produce = toLocalDate(produceDate);
        LocalDate expiry = produce.plusDays(expiryDays);
        return build(expiry);
    }

    /**
     * 미리 계산된 expiryDate(예: 박스/stock 컬럼) 가 있을 때 직접 사용.
     */
    public Map<String, Object> computeFromExpiryDate(Date expiryDate) {
        if (ValueUtil.isEmpty(expiryDate)) {
            return unknown();
        }
        return build(toLocalDate(expiryDate));
    }

    /**
     * 잔여일수 → 상태 매핑 + 응답 Map 구성.
     */
    private static Map<String, Object> build(LocalDate expiry) {
        long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        ExpiryStatus status = statusOf(daysToExpiry);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("expiryStatus", status.code());
        out.put("daysToExpiry", daysToExpiry);
        out.put("expiryDate", expiry.toString());
        return out;
    }

    /**
     * 잔여일수 → ExpiryStatus 매핑.
     */
    private static ExpiryStatus statusOf(long daysToExpiry) {
        if (daysToExpiry < 0)               return ExpiryStatus.EXPIRED;
        if (daysToExpiry < ALERT_DAYS)      return ExpiryStatus.ALERT;
        if (daysToExpiry < WARN_DAYS)       return ExpiryStatus.WARN;
        return ExpiryStatus.NORMAL;
    }

    /**
     * UNKNOWN 응답 Map.
     */
    private static Map<String, Object> unknown() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("expiryStatus", ExpiryStatus.UNKNOWN.code());
        out.put("daysToExpiry", null);
        out.put("expiryDate", null);
        return out;
    }

    /**
     * java.util.Date → java.time.LocalDate (시스템 타임존).
     */
    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
