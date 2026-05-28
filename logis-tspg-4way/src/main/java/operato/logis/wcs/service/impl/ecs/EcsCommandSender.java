package operato.logis.wcs.service.impl.ecs;

import operato.logis.ecs.tspg4way.service.InternalWcsCallbackService;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.inventory.reservation.InboundReservationService;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.impl.allocation.port.PortTrafficService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.time.Instant;

/**
 * WCS → ECS 명령 송신.
 *
 * 외부 I/O 정책: 본 클래스는 ECS 측 비동기 채널(DB polling) 을 위한 상태 마킹만 수행하고
 * 실제 HTTP/MQ 호출은 하지 않는다. 따라서 호출자 트랜잭션 안에서 호출되어도 안전하다.
 * 헤더 order_status 전이는 ShuttleOrderStateWriter 단일 창구를 경유한다.
 */
@Service
public class EcsCommandSender {

    private static final Logger logger = LoggerFactory.getLogger(EcsCommandSender.class);

    private final ShuttleOrderStateWriter shuttleOrderStateWriter;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;
    private final HostOrderRepository hostOrderRepository;
    private final InternalWcsCallbackService internalWcsCallbackService;
    private final TaskScheduler taskScheduler;
    private final InboundReservationService inboundReservationService;

    // PortTrafficService → EcsCommandSender(sendCommand) 순환 의존 해소 (@Lazy)
    private final PortTrafficService portTrafficController;

    public EcsCommandSender(ShuttleOrderStateWriter shuttleOrderStateWriter,
                            ShuttleOrderItemRepository shuttleOrderItemRepository,
                            HostOrderRepository hostOrderRepository,
                            InternalWcsCallbackService internalWcsCallbackService,
                            TaskScheduler taskScheduler,
                            InboundReservationService inboundReservationService,
                            @Lazy PortTrafficService portTrafficController) {
        this.shuttleOrderStateWriter = shuttleOrderStateWriter;
        this.shuttleOrderItemRepository = shuttleOrderItemRepository;
        this.hostOrderRepository = hostOrderRepository;
        this.internalWcsCallbackService = internalWcsCallbackService;
        this.taskScheduler = taskScheduler;
        this.inboundReservationService = inboundReservationService;
        this.portTrafficController = portTrafficController;
    }

    /**
     * 셔틀 작업 지시 전송. 헤더 order_status 는 StateWriter 의 역행 차단 가드를 통과해 SENT 전이,
     * 이어서 item line_status 도 SENT 로 일괄 맞춘다. 이미 종결된 오더면 송신하지 않는다.
     */
    public boolean sendCommand(TbWcsShuttleOrder shuttleOrder) {
        // 입력 가드
        if (ValueUtil.isEmpty(shuttleOrder)) {
            logger.error("[ Ecs ][ Send ] missing order data");
            return false;
        }

        String orderKey = shuttleOrder.getOrderKey();
        try {
            logger.info("[ Ecs ][ Send ] start - orderKey={}, type={}",
                    orderKey, shuttleOrder.getOrderType());

            // 헤더 SENT 전이 (역행 차단 가드). 이미 종결이면 송신 중단
            if (!shuttleOrderStateWriter.markSent(shuttleOrder)) {
                logger.warn("[ Ecs ][ Send ] transition blocked - orderKey={}", orderKey);
                return false;
            }

            // item line_status SENT 일괄 갱신
            shuttleOrderItemRepository.updateAllLineStatus(orderKey, ShuttleOrderStatus.SENT.code());

            logger.info("[ Ecs ][ Send ] completed - orderKey={}", orderKey);
            return true;
        } catch (Exception e) {
            logger.error("[ Ecs ][ Send ] failed - orderKey={}", orderKey, e);
            return false;
        }
    }

    /**
     * 입고 BCR 통합 진입점.
     *
     * 처리 흐름:
     *   1) InboundReservationService.confirmInboundLocationOnBcrScan 으로
     *      toLocation / carryingStockId 확정 + INBOUND_READY → INBOUND 전이.
     *   2) host=TEST_FAILED 면 재입고 거부 (폐기 대기).
     *   3) PortTrafficService.handleInboundEntry 로 겸용 포트 모드 전환.
     *      OUTBOUND_PRIORITY 모드면 진입 거부.
     *   4) 오더 헤더+아이템 상태 SENT 일괄 갱신.
     *   5) 1초 안정화 대기 후 InternalWcsCallbackService.inboundConveyorBcrRead 호출.
     */
    public boolean processInboundBcrScanByBarcode(String eqGroupId, String barcode, String scanPortCode) {
        // 입력 가드
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(barcode)) {
            logger.warn("[ Ecs ][ Bcr ] skip - invalid parameter. eqGroupId={}, barcode={}",
                    eqGroupId, barcode);
            return false;
        }

        // 로케이션·재고 확정 + INBOUND 전이
        TbWcsShuttleOrder confirmed;
        try {
            confirmed = inboundReservationService.confirmInboundLocationOnBcrScan(
                    eqGroupId, barcode, scanPortCode);
        } catch (Exception e) {
            logger.error("[ Ecs ][ Bcr ] confirm failed - eqGroupId={}, barcode={}", eqGroupId, barcode, e);
            return false;
        }

        if (ValueUtil.isEmpty(confirmed)) {
            logger.warn("[ Ecs ][ Bcr ] no matching INBOUND order - eqGroupId={}, barcode={}",
                    eqGroupId, barcode);
            return false;
        }

        // host=TEST_FAILED 면 재입고 거부
        if (ValueUtil.isNotEmpty(confirmed.getHostOrderKey())) {
            TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(confirmed.getHostOrderKey());
            if (ValueUtil.isNotEmpty(host) && host.getOrderStatus() == HostOrderStatus.TEST_FAILED.code()) {
                logger.warn("[ Ecs ][ Bcr ] rejected - host TEST_FAILED. orderKey={}, host={}",
                        confirmed.getOrderKey(), confirmed.getHostOrderKey());
                return false;
            }
        }

        String orderKey = confirmed.getOrderKey();
        String portCode = ValueUtil.isNotEmpty(scanPortCode) ? scanPortCode : confirmed.getFromLocCode();

        try {
            // 포트 트래픽 평가 — 겸용 포트 모드 전환·차단
            if (ValueUtil.isNotEmpty(portCode)) {
                boolean entryAllowed = portTrafficController.handleInboundEntry(
                        eqGroupId, portCode, confirmed.getHostOrderKey());
                if (!entryAllowed) {
                    logger.warn("[ Ecs ][ Bcr ] port traffic blocked - orderKey={}, port={}",
                            orderKey, portCode);
                    return false;
                }
            }

            // 셔틀 작업 지시 송신
            if (!sendCommand(confirmed)) {
                logger.warn("[ Ecs ][ Bcr ] sendCommand failed - orderKey={}", orderKey);
                return false;
            }

            // 1초 안정화 후 콜백 호출 (ECS 측 비동기 큐 안정화 대기)
            taskScheduler.schedule(() -> {
                try {
                    logger.info("[ Ecs ][ Bcr ] callback start - orderKey={}", orderKey);
                    internalWcsCallbackService.inboundConveyorBcrRead(orderKey);
                } catch (Exception ex) {
                    logger.error("[ Ecs ][ Bcr ] callback failed - orderKey={}", orderKey, ex);
                }
            }, Instant.now().plusSeconds(1));
            return true;
        } catch (Exception e) {
            logger.error("[ Ecs ][ Bcr ] dispatch failed - orderKey={}", orderKey, e);
            return false;
        }
    }
}
