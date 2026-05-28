package operato.logis.wcs.scheduler;

import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.host.HostOrderAuditLogger;
import operato.logis.wcs.service.impl.order.host.HostOrderEvents;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.impl.order.intake.OrderIntakeService;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import operato.logis.wcs.service.impl.system.SystemModeService;
import operato.logis.wcs.dto.WcsOrderCommandMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 호스트 오더 관련 3개 주기 작업 묶음 (단일 잡으로 통합 실행).
 *
 * 실행 순서:
 *   1) runScheduledDateTransition() — 예약일 도래 호스트 오더 WAITING_SCHEDULE → READY_FOR_ALLOC 전이
 *   2) runUnifiedOrderExecution()   — 신규 할당 + 장애 재산출 통합 실행
 *   3) runOrderRecovery()           — ERROR_GENERAL 이상 셔틀 오더 진단 로깅
 *
 * WcsJobLauncher 가 10초 주기로 runHostOrderCycle() 을 호출.
 * 각 단계는 try-catch 로 격리되어 한 단계 실패가 다른 단계 실행을 막지 않는다.
 * 잡 단위 트랜잭션 경계는 본 클래스에 두지 않고, 장애 격리는 오더 단위 try-catch 로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class HostOrderJobs {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderJobs.class);

    private static final int ERROR_THRESHOLD = ShuttleOrderStatus.ERROR_GENERAL.codeAsIntOrNull();

    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final OrderIntakeService orderIntakeService;
    private final WcsOrderCommandMapper commandMapper;
    private final SystemModeService systemModeService;
    private final HostOrderAuditLogger auditLogger;
    private final HostOrderStateWriter hostOrderStateWriter;

    /**
     * 통합 사이클 — 예약일 전이 → 신규/재시도 산출 → 장애 진단. 각 단계 try-catch 격리.
     */
    public void runHostOrderCycle() {
        try {
            runScheduledDateTransition();
        } catch (Exception e) {
            logger.error("[ Scheduler ][ Host ] scheduledDateTransition failed", e);
        }
        try {
            runUnifiedOrderExecution();
        } catch (Exception e) {
            logger.error("[ Scheduler ][ Host ] unifiedExecution failed", e);
        }
        try {
            runOrderRecovery();
        } catch (Exception e) {
            logger.error("[ Scheduler ][ Host ] orderRecovery failed", e);
        }
    }

    /**
     * 예약일 도래 호스트 오더 WAITING_SCHEDULE → READY_FOR_ALLOC 전이.
     */
    public void runScheduledDateTransition() {
        LocalDate today = LocalDate.now();
        List<TbWcsHostOrder> due = hostOrderRepository.findWaitingScheduleDueBy(today);
        if (ValueUtil.isEmpty(due)) return;

        logger.info("[ Scheduler ][ Host ] scheduledDateTransition - count={}, today={}", due.size(), today);
        for (TbWcsHostOrder h : due) {
            try {
                int prev = h.getOrderStatus();
                hostOrderStateWriter.markReadyForAllocation(h);
                auditLogger.log(h, prev, HostOrderEvents.SCHEDULE_DUE, null,
                        "scheduled_date reached: " + LocalDateUtils.toLocalDate(h.getScheduledDate()));
            } catch (Exception e) {
                logger.error("[ Scheduler ][ Host ] scheduledDateTransition failed - hostOrderKey={}",
                        h.getHostOrderKey(), e);
            }
        }
    }

    /**
     * 신규 할당(ReadyForAlloc) + 장애 재산출(Retry) 통합 실행.
     */
    public void runUnifiedOrderExecution() {
        List<TbWcsHostOrder> ready = nullSafe(hostOrderRepository.findReadyForAllocation());
        List<TbWcsHostOrder> retry = nullSafe(hostOrderRepository.findOrdersForRetry());

        if (ValueUtil.isEmpty(ready) && ValueUtil.isEmpty(retry)) return;

        // 두 리스트 병합
        List<TbWcsHostOrder> all = new ArrayList<>(ready.size() + retry.size());
        all.addAll(ready);
        all.addAll(retry);

        logger.info("[ Scheduler ][ Host ] unifiedExecution - total={}, new={}, retry={}",
                all.size(), ready.size(), retry.size());

        int idx = 0;
        for (TbWcsHostOrder h : all) {
            logger.info("[ Scheduler ][ Host ] execute - seq={}, hostOrderKey={}", ++idx, h.getHostOrderKey());
            String ctx = HostOrderStatus.READY_FOR_ALLOC.code().equals(h.getOrderStatus())
                    ? "ReadyForAlloc" : "Retry";
            executeOne(h, ctx);
        }
    }

    /**
     * ERROR_GENERAL 이상의 셔틀 오더 진단 로깅 — 수동 복구 가이드.
     */
    public void runOrderRecovery() {
        List<TbWcsShuttleOrder> sick = shuttleOrderRepository.findByStatusGreaterThanEqual(ERROR_THRESHOLD);
        if (ValueUtil.isEmpty(sick)) return;

        logger.info("[ Scheduler ][ Host ] orderRecovery - count={}", sick.size());
        for (TbWcsShuttleOrder o : sick) {
            ShuttleOrderStatus status = ShuttleOrderStatus.from(o.getOrderStatus());
            if (ValueUtil.isEmpty(status)) continue;

            // 에러 유형별 진단 로그
            switch (status) {
                case ERROR_HARDWARE ->
                        logger.error("[ Scheduler ][ Host ] recovery - hardware error blocked - orderKey={}", o.getOrderKey());
                case ERROR_LOCATION ->
                        logger.warn("[ Scheduler ][ Host ] recovery - location master fix wait - orderKey={}", o.getOrderKey());
                case ERROR_INVENTORY ->
                        logger.error("[ Scheduler ][ Host ] recovery - inventory check required - orderKey={}", o.getOrderKey());
                case ERROR_SYSTEM_RESTART ->
                        logger.warn("[ Scheduler ][ Host ] recovery - orphan after restart - orderKey={}", o.getOrderKey());
                default ->
                        logger.warn("[ Scheduler ][ Host ] recovery - unknown error status={}, orderKey={}",
                                o.getOrderStatus(), o.getOrderKey());
            }
        }
    }

    /**
     * 단일 호스트 오더 산출 실행 — 게이팅 → 커맨드 변환 → Intake → 결과 처리.
     */
    private void executeOne(TbWcsHostOrder h, String ctx) {
        try {
            // 운영 모드 게이팅
            SystemModeService.GatingResult gate = systemModeService.check(h);
            if (!gate.allowed()) {
                logger.info("[ Scheduler ][ Host ] gated - ctx={}, hostOrderKey={}, reason={}",
                        ctx, h.getHostOrderKey(), gate.reason());
                return;
            }

            // 호스트 → 내부 커맨드 변환 후 산출
            List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByHostOrderKey(
                    h.getHostSystemCode(), h.getHostOrderKey());
            WcsOrderCommand cmd = commandMapper.fromHostOrder(h, items);
            HostOrderApi.Response resp = orderIntakeService.execute(cmd);

            // 결과 분기
            if (resp.isSuccess()) {
                logger.info("[ Scheduler ][ Host ] success - ctx={}, hostOrderKey={}, wcsOrderKey={}",
                        ctx, h.getHostOrderKey(), resp.getWcsOrderKey());
            } else {
                logger.warn("[ Scheduler ][ Host ] failed - ctx={}, hostOrderKey={}, errorCode={}, errorDesc={}",
                        ctx, h.getHostOrderKey(), resp.getErrorCode(), resp.getErrorDesc());
                hostOrderStateWriter.markError(h, resp.getErrorCode(), resp.getErrorDesc());
            }
        } catch (Exception e) {
            logger.error("[ Scheduler ][ Host ] executeOne failed - ctx={}, hostOrderKey={}",
                    ctx, h.getHostOrderKey(), e);
            hostOrderStateWriter.markError(h, WcsError.INTERNAL_ERROR.codeAsString(), e.getMessage());
        }
    }

    /**
     * null 안전 빈 리스트 보장.
     */
    private static <T> List<T> nullSafe(List<T> list) {
        return ValueUtil.isEmpty(list) ? List.of() : list;
    }
}
