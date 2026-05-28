package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.HostOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HOST 시스템 콜백 서비스
 * - WCS 처리 결과를 HOST 시스템에 통보
 */
@Service
public class HostCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(HostCallbackService.class);

    /**
     * 주문 수신 결과 통보
     */
    public void notifyOrderReceived(TbWcsHostOrder hostOrder, boolean success, String errorCode, String errorDesc) {
        logger.info("Notifying HOST order received: hostOrderKey={}, success={}, errorCode={}",
                hostOrder.getHostOrderKey(), success, errorCode);

        // TODO: HOST 시스템 연동 구현
        // HTTP/MQ 등을 통해 HOST 시스템에 결과 전송
        String hostSystemCode = hostOrder.getHostSystemCode();
        String hostOrderKey = hostOrder.getHostOrderKey();

        if (success) {
            logger.info("Order received successfully: hostSystemCode={}, hostOrderKey={}",
                    hostSystemCode, hostOrderKey);
        } else {
            logger.error("Order receive failed: hostSystemCode={}, hostOrderKey={}, error={}:{}",
                    hostSystemCode, hostOrderKey, errorCode, errorDesc);
        }
    }

    /**
     * 주문 상태 변경 통보
     */
    public void notifyOrderStatusChanged(TbWcsHostOrder hostOrder, int newStatus) {
        logger.info("Notifying HOST order status changed: hostOrderKey={}, newStatus={}",
                hostOrder.getHostOrderKey(), newStatus);

        String statusName = getStatusName(newStatus);
        logger.info("Order status changed to {}: hostSystemCode={}, hostOrderKey={}",
                statusName, hostOrder.getHostSystemCode(), hostOrder.getHostOrderKey());

        // TODO: HOST 시스템 연동 구현
    }

    /**
     * 주문 완료 통보
     */
    public void notifyOrderCompleted(TbWcsHostOrder hostOrder, TbWcsShuttleOrder shuttleOrder) {
        logger.info("Notifying HOST order completed: hostOrderKey={}, wcsOrderKey={}",
                hostOrder.getHostOrderKey(), shuttleOrder.getOrderKey());

        // TODO: HOST 시스템 연동 구현
        // 완료 정보: 처리 시간, 실제 위치, 작업 결과 등
    }

    /**
     * 주문 실패 통보
     */
    public void notifyOrderFailed(TbWcsHostOrder hostOrder, String errorCode, String errorDesc) {
        logger.error("Notifying HOST order failed: hostOrderKey={}, error={}:{}",
                hostOrder.getHostOrderKey(), errorCode, errorDesc);

        // TODO: HOST 시스템 연동 구현
    }

    /**
     * 상태 코드를 이름으로 변환
     */
    private String getStatusName(int status) {

        HostOrderStatusEnumCode code = HostOrderStatusEnumCode.from(status);

        if (code != null) {
            return code.name(); // RECEIVED, ALLOCATED, WAITING_EXEC, COMPLETED ...
        }

        return "UNKNOWN(" + status + ")";
    }
}
