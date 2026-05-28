package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.scheduler;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.EcsCommandService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderItemService;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.util.List;

/**
 * [WCS 통합 복구 엔진]
 * WCS 관점에서 발생한 모든 오더 장애(상태 100 이상)를 감시하고,
 * 스스로 상태를 바로잡아 작업을 완수시키는 WCS 자가 치유 스케줄러입니다.
 */
@Component
public class WcsOrderRecoveryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WcsOrderRecoveryScheduler.class);

    @Autowired private TbWcsShuttleOrderService shuttleOrderService;
    @Autowired private TbWcsShuttleOrderItemService itemService;
    @Autowired private EcsCommandService ecsCommandService;

    /**
     * 10초 간격으로 WCS 장애 오더들을 스캔하여 복구 시도
     */
    @Scheduled(fixedDelay = 10000)
    public void executeWcsSelfHealing() {
        // 1. WCS DB에서 상태가 100 이상인 '아픈' 오더들을 싹 긁어옵니다.
        List<TbWcsShuttleOrder> sickOrders = shuttleOrderService.findByStatusGreaterThanEqual(ShuttleOrderStatusEnumCode.ERROR_GENERAL.codeAsIntOrNull());

        if (ValueUtil.isEmpty(sickOrders)) {
            return;
        }

        logger.info("[WCS_RECOVERY] {}건의 장애 데이터 발견. 복구 시나리오를 가동합니다.", sickOrders.size());

        for (TbWcsShuttleOrder order : sickOrders) {
            ShuttleOrderStatusEnumCode errorStatus = ShuttleOrderStatusEnumCode.from(order.getOrderStatus());
            if (errorStatus == null) continue;

            // 2. 에러 코드별 복구 가이드라인 (WCS의 판단)
            switch (errorStatus) {

                case ERROR_SEND_FAIL: // 110: WCS -> ECS 전송 실패 건
                    retryWcsCommand(order, "전송 실패 건 자동 재시도");
                    break;

                case ERROR_TIMEOUT: // 120: 서버 재시작 등으로 인한 WCS 상태 불명 건
                    // ECS가 이미 가져갔는지 확인하는 절차가 있으면 좋지만,
                    // 기본적으로 WCS는 '확신이 없으면 다시 보낸다'는 정책으로 대응합니다.
                    retryWcsCommand(order, "WCS 재시작 오더 상태 동기화");
                    break;

                case ERROR_HARDWARE: // 130: 실제 설비 물리 장애
                    // 기계가 아픈 건 WCS가 소프트웨어적으로 고칠 수 없습니다.
                    // 로그를 남겨 운영자가 현장으로 가도록 알립니다.
                    logger.error("[WCS_RECOVERY_BLOCK] 설비 물리 장애 확인 필요 (WCS 대기 중) - orderKey: {}", order.getOrderKey());
                    break;

                case ERROR_LOCATION: // 140: 로케이션 데이터 부정합 (Full/Error 등)
                    // 운영자가 WCS 마스터에서 위치를 수정할 때까지 기다립니다.
                    logger.warn("[WCS_RECOVERY_WAIT] 로케이션 마스터 수정 대기 중 - orderKey: {}", order.getOrderKey());
                    break;

                case ERROR_INVENTORY: // 150: 재고 데이터 부정합
                    logger.error("[WCS_RECOVERY_CRITICAL] 재고 실물 확인 필요 (데이터 불일치) - orderKey: {}", order.getOrderKey());
                    break;

                default:
                    logger.warn("[WCS_RECOVERY_UNKNOWN] 알 수 없는 에러 상태({}): orderKey={}",
                            order.getOrderStatus(), order.getOrderKey());
                    break;
            }
        }
    }

    /**
     * WCS 명령 재전송 처리
     */
    private void retryWcsCommand(TbWcsShuttleOrder order, String reason) {
        logger.info("[WCS_ACTION] {} - orderKey: {}", reason, order.getOrderKey());

        // WCS의 책임: 오더 헤더와 상세 아이템을 함께 챙겨서 보냅니다.
        List<TbWcsShuttleOrderItem> items = itemService.findByOrderKey(order.getOrderKey());

        // EcsCommandService를 통해 다시 문 앞에(DB Polling 지점) 갖다 놓습니다.
        boolean isSuccess = ecsCommandService.sendCommand(order, items);

        if (isSuccess) {
            logger.info("[WCS_RECOVERY_OK] 복구 성공 - orderKey: {}", order.getOrderKey());
        } else {
            logger.error("[WCS_RECOVERY_STILL_FAIL] 복구 실패 (다음 주기에 재시도) - orderKey: {}", order.getOrderKey());
        }
    }
}