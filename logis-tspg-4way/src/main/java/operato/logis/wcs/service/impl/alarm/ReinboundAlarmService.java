package operato.logis.wcs.service.impl.alarm;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.util.SettingUtil;

import java.util.List;
import java.util.Map;

/**
 * 재입고 대기 알람 단일 소스.
 *
 * 정의: shuttle_order.follow_up_since 가 set 된(출고 완료 후 미재입고) 파렛트.
 * follow_up_since 는 출고 COMPLETED 시점 set, 재입고 BCR 스캔 시점 clear.
 *
 * 동일 메서드를 세 경로가 공유한다 — REST(PalletWorkstation 초기 조회),
 * STOMP 주기 브로드캐스트(ReinboundAlarmBroadcaster), 2D AlarmDataProvider.
 */
@Service
@RequiredArgsConstructor
public class ReinboundAlarmService {

    // 알람 간격 설정 키 + 기본값(분). 설정 미존재 시 기본 사용.
    private static final String SETTING_ALARM_INTERVAL_MIN = "wcs.reinbound.alarm.interval.min";
    private static final int DEFAULT_ALARM_INTERVAL_MIN = 20;

    private final ShuttleOrderRepository shuttleOrderRepository;

    // follow-up 알람 응답 — 설정 간격 + 재입고 대기 파렛트 목록.
    public record FollowUpAlarmResponse(int intervalMin, List<Map> pallets) {}

    /** 재입고 대기 파렛트 목록 (followUpSince 기반). 각 행에 elapsed_min(NOW-follow_up_since) 포함. */
    @Transactional(readOnly = true)
    public List<Map> getWaitingPallets() {
        return shuttleOrderRepository.findFollowUpPendingPallets();
    }

    /** 알람 간격(분) — 설정값 파싱, 비숫자·0 이하면 기본값. */
    public int getIntervalMin() {
        String value = SettingUtil.getValue(SETTING_ALARM_INTERVAL_MIN, String.valueOf(DEFAULT_ALARM_INTERVAL_MIN));
        try {
            int n = Integer.parseInt(value.trim());
            return n > 0 ? n : DEFAULT_ALARM_INTERVAL_MIN;
        } catch (NumberFormatException e) {
            return DEFAULT_ALARM_INTERVAL_MIN;
        }
    }

    /** REST 응답 빌드 — 간격 + 대기 목록. */
    @Transactional(readOnly = true)
    public FollowUpAlarmResponse buildResponse() {
        return new FollowUpAlarmResponse(getIntervalMin(), getWaitingPallets());
    }
}
