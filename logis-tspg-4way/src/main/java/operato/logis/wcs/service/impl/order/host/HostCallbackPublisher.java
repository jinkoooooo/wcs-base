package operato.logis.wcs.service.impl.order.host;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.event.WcsHostCallbackEvent;
import operato.logis.wcs.service.repository.HostOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import xyz.elidom.util.ValueUtil;

/**
 * WCS → HOST 콜백 비동기 송신.
 *
 * 트랜잭션 격리:
 *   - @TransactionalEventListener(AFTER_COMMIT) : 커밋 이후에만 동작 (롤백 시 통보 차단)
 *   - @Async("wcsCallbackExecutor") : 별도 스레드 풀 실행 (DB 커넥션 잡은 채 외부 호출 방지)
 *
 * 비동기 스레드 예외는 본 트랜잭션과 무관 (이미 커밋 후이므로 롤백되지 않음).
 */
@Service
@RequiredArgsConstructor
public class HostCallbackPublisher {

    private static final Logger logger = LoggerFactory.getLogger(HostCallbackPublisher.class);

    private final HostOrderRepository hostOrderRepository;

    /**
     * 콜백 이벤트 수신 진입점. 이벤트 유형별로 통보 메서드로 분기한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("wcsCallbackExecutor")
    public void onWcsHostCallbackEvent(WcsHostCallbackEvent event) {
        logger.debug("[ Order ][ Host ] callback received - event={}", event);

        // HOST 주문 조회 - 없으면 통보 생략
        String wcsOrderKey = event.getWcsOrderKey();
        TbWcsHostOrder hostOrder = hostOrderRepository.findByWcsOrderKey(wcsOrderKey);
        if (ValueUtil.isEmpty(hostOrder)) {
            logger.warn("[ Order ][ Host ] callback skipped - host order not found, wcsOrderKey={}", wcsOrderKey);
            return;
        }

        // 이벤트 유형별 통보 분기
        try {
            switch (event.getEventType()) {
                case STARTED   -> notifyStatusChanged(hostOrder, event.getNewHostStatus());
                case COMPLETED -> notifyCompleted(hostOrder, event.getShuttleOrder());
                case RACK_CONVEYOR_ARRIVED -> logger.info(
                        "[ Order ][ Host ] callback skipped - rack conveyor arrived (no host notify), wcsOrderKey={}",
                        wcsOrderKey);
                case FAILED    -> notifyFailed(hostOrder, event.getErrorCode(), event.getErrorDesc());
                case CANCELLED -> notifyStatusChanged(hostOrder, event.getNewHostStatus());
            }
        } catch (Exception e) {
            logger.error("[ Order ][ Host ] callback failed - wcsOrderKey={}, eventType={}",
                    wcsOrderKey, event.getEventType(), e);
        }
    }

    /**
     * 상태 변경 통보 (STARTED / CANCELLED 공통).
     */
    private void notifyStatusChanged(TbWcsHostOrder hostOrder, int newStatus) {
        logger.info("[ Order ][ Host ] status changed - hostSystemCode={}, hostOrderKey={}, newStatus={}",
                hostOrder.getHostSystemCode(), hostOrder.getHostOrderKey(), statusName(newStatus));
    }

    /**
     * 완료 통보.
     */
    private void notifyCompleted(TbWcsHostOrder hostOrder, TbWcsShuttleOrder shuttleOrder) {
        String shuttleKey = ValueUtil.isNotEmpty(shuttleOrder) ? shuttleOrder.getOrderKey() : "null";
        logger.info("[ Order ][ Host ] completed - hostOrderKey={}, wcsOrderKey={}",
                hostOrder.getHostOrderKey(), shuttleKey);
    }

    /**
     * 실패 통보.
     */
    private void notifyFailed(TbWcsHostOrder hostOrder, String errorCode, String errorDesc) {
        logger.error("[ Order ][ Host ] failed - hostOrderKey={}, errorCode={}, errorDesc={}",
                hostOrder.getHostOrderKey(), errorCode, errorDesc);
    }

    /**
     * 상태 코드 → 이름 변환 (로그 가독성).
     */
    private String statusName(int status) {
        HostOrderStatus code = HostOrderStatus.from(status);
        return ValueUtil.isNotEmpty(code) ? code.name() : "UNKNOWN(" + status + ")";
    }
}
