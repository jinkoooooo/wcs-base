package operato.logis.wcs.service.impl.order.host;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.HostOrderType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.service.impl.external.ExternalOrderNotifier;
import operato.logis.wcs.service.impl.qctest.QcResultService;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;

/**
 * WCS 주문 생성 단일 창구 + 멱등성 체크.
 *
 * 유형별 초기 상태 전이:
 *   - INBOUND  : testRequired=true 면 시험 의뢰만 마킹 (READY_FOR_ALLOC 은 BCR release 후)
 *   - OUTBOUND : 예약일 미래면 WAITING_SCHEDULE, 아니면 READY_FOR_ALLOC
 *   - 그 외    : 즉시 READY_FOR_ALLOC
 *
 * Reservation (HOST_PENDING 점유) 은 HostOrderStateWriter.saveHostOrderFromCreateRequest
 * 안에서 처리. Release 도 HostOrderStateWriter 의 markError / transitionStatus(종결) 에서 처리.
 * 즉 reserve/release 라이프사이클은 StateWriter 가 단일 책임.
 */
@Service
@RequiredArgsConstructor
public class HostOrderCreator {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderCreator.class);

    private final HostOrderStateWriter stateWriter;
    private final ExternalOrderNotifier externalNotifier;
    private final QcResultService qcResultService;
    private final HostOrderAuditLogger auditLogger;
    private final HostOrderRepository hostOrderRepository;

    /**
     * (hostSystemCode, hostOrderKey) 멱등성 체크.
     * null = 신규, non-null = 중복 수신.
     */
    public TbWcsHostOrder findExisting(HostOrderApi.Request request) {
        if (ValueUtil.isEmpty(request)) return null;

        TbWcsHostOrder existing = hostOrderRepository.findByHostOrderKey(
                request.getHostSystemCode(), request.getHostOrderKey());

        if (ValueUtil.isNotEmpty(existing)) {
            logger.info("[ Order ][ Host ] duplicate received - hostOrderKey={}, wcsOrderKey={}",
                    existing.getHostOrderKey(), existing.getWcsOrderKey());
        }
        return existing;
    }

    /**
     * 주문 생성 진입점 — persist + 외부 통보 + 유형별 상태 전이.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsHostOrder create(HostOrderApi.Request req) {

        // 주문 저장 (내부에서 reserveForHostOrder 호출 — from/to 지정 시 HOST_PENDING 점유)
        TbWcsHostOrder saved = stateWriter.saveHostOrderFromCreateRequest(req);

        // 외부 시스템 통보
        externalNotifier.notifyOrderCreated(saved);

        // 유형별 상태 전이 처리
        OrderType type = HostOrderType.resolveBaseType(saved.getOrderType());
        switch (type) {
            case INBOUND  -> handleInbound(saved);
            case OUTBOUND -> handleOutbound(saved);
            default       -> stateWriter.markReadyForAllocation(saved);
        }

        logger.info("[ Order ][ Host ] created - hostOrderKey={}, type={}, testRequired={}, testStatus={}, fromLoc={}, toLoc={}, status={}",
                saved.getHostOrderKey(), saved.getOrderType(),
                saved.getTestRequired(), saved.getTestStatus(),
                saved.getFromLocCode(), saved.getToLocCode(),
                saved.getOrderStatus());

        // 감사 이력 기록
        auditLogger.log(saved, null, HostOrderEvents.CREATED, null, null);
        return saved;
    }

    /**
     * INBOUND 분기 — testRequired 면 시험 의뢰 (READY_FOR_ALLOC 은 BCR release 후).
     */
    private void handleInbound(TbWcsHostOrder saved) {
        if (Boolean.TRUE.equals(saved.getTestRequired())) {
            qcResultService.requestTest(saved);
        }
    }

    /**
     * OUTBOUND 분기 — 예약일 미래면 WAITING_SCHEDULE, 아니면 READY_FOR_ALLOC.
     */
    private void handleOutbound(TbWcsHostOrder saved) {
        LocalDate sched = LocalDateUtils.toLocalDate(saved.getScheduledDate());
        if (ValueUtil.isNotEmpty(sched) && sched.isAfter(LocalDate.now())) {
            stateWriter.markWaitingSchedule(saved);
        } else {
            stateWriter.markReadyForAllocation(saved);
        }
    }

}
