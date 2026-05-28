package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.ecs.tspg4way.service.InternalWcsCallbackService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsIfStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.time.Instant;
import java.util.List;

/**
 * ECS(Equipment Control System) 명령 전송 서비스
 * - WCS에서 ECS로 작업 지시 전송 (DB Polling 방식)
 */
@Service
public class EcsCommandService {

    private static final Logger logger = LoggerFactory.getLogger(EcsCommandService.class);

    @Autowired
    private TbWcsShuttleOrderService shuttleOrderService;

    @Autowired
    private InternalWcsCallbackService internalWcsCallbackService;

    @Autowired
    private TaskScheduler taskScheduler;

    /**
     * 셔틀 작업 지시 전송 (상태 업데이트)
     * @return 성공 여부
     */
    public boolean sendCommand(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        if (shuttleOrder == null) {
            logger.error("[ECS_SEND] 오더 데이터 누락으로 인한 전송 불가");
            return false;
        }

        String orderKey = shuttleOrder.getOrderKey();

        try {
            logger.info("[ECS_SEND_ATTEMPT] 전송 시작 - orderKey: {}, type: {}",
                    orderKey, shuttleOrder.getOrderType());

            // 2. [상태 일괄 업데이트] 헤더(SENT) + 아이템들(SENT) 동시 처리
            // 서비스 내부에서 @Transactional이 선언되어 있으므로 안전합니다.
            shuttleOrderService.updateFullStatus(
                    orderKey,
                    (Integer) ShuttleOrderStatusEnumCode.SENT.code(),
                    (Integer) EcsIfStatusEnumCode.SENT.code()
            );

            logger.info("[ECS_SEND_SUCCESS] 전송 및 상태 동기화 완료 - orderKey: {}", orderKey);

            return true;

        } catch (Exception e) {
            // 예외 발생 시 로그를 남기고 false를 리턴.
            // DB는 updateFullStatus 내부의 @Transactional에 의해 자동 롤백됩니다.
            logger.error("[ECS_SEND_ERROR] 전송 처리 중 예외 발생 - orderKey: {}, message: {}",
                    orderKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * [INBOUND] 컨베이어 BCR 스캔 완료 시 최종 실행 트리거
     * 1. 전제 조건: sendCommand를 통해 오더는 이미 SENT 상태로 준비됨.
     * 2. BCR 스캔 발생: 물건이 물리적으로 입고 지점에 도착함.
     * 3. 1초 대기: 컨베이어 정지 및 센서 안정화를 위한 물리적 시간 확보.
     * 4. 실행 명령: 1초 후 internalWcsCallbackService를 호출하여 설비 가동 시작.
     */
    public void triggerInbound(String orderKey) {
        if (ValueUtil.isEmpty(orderKey)) {
            logger.warn("[INBOUND_TRIGGER] 스킵: orderKey가 유효하지 않습니다.");
            return;
        }

        // [STEP 1] 스캔 확인 로그 (이미 명령은 가 있는 상태)
        logger.info("[BCR_SCANNED] 물체 도착 확인! - orderKey: {}. 이미 명령(sendCommand)은 준비되었습니다. 1초 후 가동을 시작합니다.", orderKey);

        try {
            // [STEP 2] 비동기 1초 대기 (서버 부하 방지를 위해 스케줄러 사용)
            taskScheduler.schedule(
                    () -> {
                        try {
                            // [STEP 3] 1초 후 최종 실행 콜백 호출
                            logger.info("[EXECUTION_START] 1초 대기 완료. 입고 설비 가동 신호를 송출합니다. - orderKey: {}", orderKey);

                            // 여기서 호출하는 서비스 로직은 '준비된 명령'을 찾아
                            // 실제 PLC 모터를 돌리거나 셔틀을 출발시키는 인터페이스를 탑니다.
                            internalWcsCallbackService.inboundConveyorBcrRead(orderKey);

                        } catch (Exception e) {
                            logger.error("[EXECUTION_ERROR] 가동 신호 송출 중 장애 발생 - orderKey: {}", orderKey, e);
                        }
                    },
                    Instant.now().plusSeconds(1) // BCR 스캔 시점으로부터 정확히 1초 후
            );

        } catch (Exception e) {
            logger.error("[TRIGGER_SCHEDULE_ERROR] 실행 예약 실패 - orderKey: {}", orderKey, e);
        }
    }

    /**
     * [고도화 전용] BCR 스캔 발생 시 호출되는 신규 메서드
     * 1. 어느 포트에서 스캔되었는지 주문 정보(fromLocCode) 업데이트
     * 2. ECS(PLC) 측에 물리적 위치 정보 동기화 명령 송출
     * 3. 물리적 안정화(1초) 후 ECS 가동 명령 하달
     */
    public void processInboundBcrScan(String orderKey, String scanPortCode) {
        if (ValueUtil.isEmpty(orderKey) || ValueUtil.isEmpty(scanPortCode)) {
            logger.warn("[BCR_SCAN_PROCESS] 스킵: 데이터 부족 (orderKey={}, port={})", orderKey, scanPortCode);
            return;
        }

        logger.info("[BCR_SCANNED] 물체 도착 확인! - orderKey: {}, Port: {}", orderKey, scanPortCode);

        try {
            // 1. [WCS DB 업데이트] 주문 데이터에 실제 스캔된 포트 정보 업데이트
            TbWcsShuttleOrder order = shuttleOrderService.findByOrderKey(orderKey);
            if (order != null) {
                order.setFromLocCode(scanPortCode);
                shuttleOrderService.update(order, "fromLocCode");
                logger.info("[BCR_UPDATE] WCS 주문 출발지 확정 완료: {}", scanPortCode);

                // TODO: STEP 1 - ECS(PLC) 데이터 동기화 호출]
                logger.info("[ECS_SYNC_WAIT] ECS 측에 위치 동기화 명령 송출 대기 중... (Port: {})", scanPortCode);

            } else {
                logger.error("[BCR_ERROR] 주문 정보를 찾을 수 없습니다: {}", orderKey);
                return;
            }

            // 2. [물리적 안정화 대기] 1초 후 설비 가동 명령 (비동기)
            taskScheduler.schedule(
                    () -> {
                        try {
                            logger.info("[EXECUTION_START] 1초 안정화 완료. {}에서 셔틀 출발 신호 송출!", scanPortCode);
                            internalWcsCallbackService.inboundConveyorBcrRead(orderKey);

                        } catch (Exception e) {
                            logger.error("[EXECUTION_ERROR] 설비 가동 신호 송출 실패 - orderKey: {}", orderKey, e);
                        }
                    },
                    Instant.now().plusSeconds(1)
            );

        } catch (Exception e) {
            logger.error("[BCR_PROCESS_ERROR] BCR 처리 중 장애 발생 - orderKey: {}", orderKey, e);
        }
    }
}