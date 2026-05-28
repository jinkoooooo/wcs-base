package operato.logis.wcs.service.impl.alarm;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.event.RealtimeEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 재입고 대기 알람 주기 브로드캐스트.
 *
 * WcsJobLauncher 가 고정 주기로 broadcastTick 을 구동한다 (대시보드 RealTimeBroadcastScheduler 와 동일 사상).
 * 프론트는 구독만 — 경과(elapsed_min)·due 판정은 백엔드 push 페이로드로만 갱신한다.
 * 발행 정책: 대기 목록이 있으면 매 주기 발행(경과 갱신), 빈 목록은 비워진 전환 시 1회만(클리어용).
 */
@Component
@RequiredArgsConstructor
public class ReinboundAlarmBroadcaster {

    private final ReinboundAlarmService reinboundAlarmService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    // 직전 주기의 빈 여부 — 빈 목록 반복 발행 억제용.
    private volatile boolean lastEmpty = true;

    /** 한 주기 — 대기 목록을 STOMP 토픽으로 발행. */
    public void broadcastTick() {
        List<Map> pallets = reinboundAlarmService.getWaitingPallets();
        boolean empty = pallets.isEmpty();

        // 계속 빈 상태면 발행 생략(있을 때만). 비워진 첫 주기는 발행해 프론트 클리어.
        if (empty && lastEmpty) return;

        realtimeEventPublisher.publishReinboundAlarm(reinboundAlarmService.getIntervalMin(), pallets);
        lastEmpty = empty;
    }
}
