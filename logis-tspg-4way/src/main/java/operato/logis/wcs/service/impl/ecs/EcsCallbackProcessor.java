package operato.logis.wcs.service.impl.ecs;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.EcsCallbackStatus;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.EcsCallbackApi;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.order.host.HostOrderCompletion;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import operato.logis.wcs.service.impl.inventory.reservation.OutboundReservationService;
import operato.logis.wcs.service.impl.pallet.OutboundFinalizer;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import operato.logis.wcs.event.WcsHostCallbackEvent;
import operato.logis.wcs.handler.WcsOrderHandler;
import operato.logis.wcs.service.impl.allocation.location.LocationService;
import operato.logis.wcs.service.impl.allocation.port.PortTrafficService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.List;

/**
 * ECS 콜백 상태별 후처리.
 *
 * 트랜잭션 정책:
 *   - process / completeShuttleAfterFinalize 는 Propagation.REQUIRED 로
 *     호출자 트랜잭션에 합류, 독립 호출 시에도 새 트랜잭션을 시작.
 *   - rollbackFor = Exception.class — 체크 예외에도 롤백 보장.
 *   - HOST 통보는 ApplicationEventPublisher 로 이벤트만 발행하고
 *     실제 외부 송신은 AFTER_COMMIT 의 HostCallbackPublisher 에서 비동기 수행.
 */
@Service
@RequiredArgsConstructor
public class EcsCallbackProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EcsCallbackProcessor.class);

    private final ShuttleOrderRepository shuttleOrderRepository;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;
    private final ShuttleOrderStateWriter shuttleOrderStateWriter;
    private final HostOrderStateWriter hostOrderStateWriter;
    private final HostOrderCompletion hostOrderCompletion;
    private final PortTrafficService portTrafficController;
    private final LocationService wcsLocationService;
    private final OutboundFinalizer outboundFinalizer;
    private final OutboundReservationService outboundReservationService;
    private final ApplicationEventPublisher eventPublisher;
    private final List<WcsOrderHandler> orderHandlers;

    /**
     * ECS 콜백 진입점. 멱등 가드 후 상태별 핸들러로 분기.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public EcsCallbackApi.Response process(EcsCallbackApi.Request request,
                                           TbWcsShuttleOrder shuttleOrder,
                                           List<TbWcsShuttleOrderItem> items,
                                           WcsOrderHandler handler) {

        Integer currentStatus = shuttleOrder.getOrderStatus();

        // [멱등 가드 #1] 종료 상태 오더는 어떤 콜백도 무시
        if (ShuttleOrderStatus.isFinalStatus(currentStatus)) {
            logger.warn("[ Ecs ][ Callback ] duplicated - terminal state. orderKey={}, currentStatus={}, incoming={}",
                    shuttleOrder.getOrderKey(), currentStatus, request.getStatus());
            return EcsCallbackApi.Response.success("Already in terminal state (idempotent)");
        }

        // 상태 코드 파싱
        EcsCallbackStatus status = EcsCallbackStatus.from(request.getStatus());
        if (ValueUtil.isEmpty(status)) {
            return EcsCallbackApi.Response.fail(
                    WcsError.INVALID_REQUEST.codeAsString(),
                    "Unknown status: " + request.getStatus());
        }

        // [멱등 가드 #2] STARTED 재수신 — 이미 RUNNING 이상이면 무시
        Integer runningCode = ShuttleOrderStatus.RUNNING.codeAsIntOrNull();
        if (status == EcsCallbackStatus.STARTED
                && ValueUtil.isNotEmpty(currentStatus)
                && ValueUtil.isNotEmpty(runningCode)
                && currentStatus >= runningCode) {
            logger.warn("[ Ecs ][ Callback ] duplicated - STARTED already running. orderKey={}, currentStatus={}",
                    shuttleOrder.getOrderKey(), currentStatus);
            return EcsCallbackApi.Response.success("Already started (idempotent)");
        }

        // 상태별 분기
        return switch (status) {
            case STARTED -> handleStarted(shuttleOrder);
            case FROM_LOADING_COMPLETE -> handleFromLoadingComplete(shuttleOrder, items, handler, request);
            case TO_UNLOADING_COMPLETE -> handleToUnloadingComplete(shuttleOrder, items, handler);
            case COMPLETE -> handleCompleted(shuttleOrder, items, handler);
            case RACK_CONVEYOR_ARRIVED -> handleRackConveyorArrived(shuttleOrder, handler);
            case ERROR -> handleFailed(shuttleOrder, handler, request);
            case CANCELLED -> handleCancelled(shuttleOrder, handler);
            case ACCEPTED, IN_PROGRESS -> EcsCallbackApi.Response.success("Callback received: " + status.codeAsString());
        };
    }

    /**
     * 박스 finalize 후 진입 — ECS callback 후처리 흐름(handler.handleCompletion + COMPLETED + host transition) 을 동일 실행.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void completeShuttleAfterFinalize(TbWcsShuttleOrder shuttleOrder) {
        List<TbWcsShuttleOrderItem> items = shuttleOrderItemRepository.findByOrderKey(shuttleOrder.getOrderKey());
        doCompleteShuttle(shuttleOrder, items, handlerFor(shuttleOrder.getOrderType()));
    }

    /**
     * STARTED — shuttle RUNNING + host EXECUTING + HOST 통보 이벤트.
     */
    private EcsCallbackApi.Response handleStarted(TbWcsShuttleOrder shuttleOrder) {
        logger.info("[ Ecs ][ Callback ] started - orderKey={}", shuttleOrder.getOrderKey());

        // 셔틀 상태 전이
        shuttleOrderStateWriter.markRunning(shuttleOrder);

        // host 동반 전이 (hostOrderKey 가 있는 경우만)
        String hostOrderKey = shuttleOrder.getHostOrderKey();
        if (ValueUtil.isNotEmpty(hostOrderKey)) {
            hostOrderStateWriter.markExecutingByHostOrderKey(hostOrderKey);
            eventPublisher.publishEvent(
                    WcsHostCallbackEvent.ofStarted(shuttleOrder.getOrderKey(), HostOrderStatus.EXECUTING.code()));
        } else {
            // 방해물 제거 MOVE 등 hostOrderKey 가 없는 오더
            logger.info("[ Ecs ][ Callback ] no hostOrderKey - skip host transition. orderKey={}",
                    shuttleOrder.getOrderKey());
        }
        return EcsCallbackApi.Response.success("Order started");
    }

    /**
     * 출발지 적재 완료 — handler 위임만.
     */
    private EcsCallbackApi.Response handleFromLoadingComplete(TbWcsShuttleOrder shuttleOrder,
                                                              List<TbWcsShuttleOrderItem> items,
                                                              WcsOrderHandler handler,
                                                              EcsCallbackApi.Request request) {
        logger.info("[ Ecs ][ Callback ] fromLoadingComplete - orderKey={}", shuttleOrder.getOrderKey());
        handler.handleFromLoadingComplete(shuttleOrder, items, request);
        return EcsCallbackApi.Response.success("From loading complete");
    }

    /**
     * 목적지 하역 완료 — handler 위임만.
     */
    private EcsCallbackApi.Response handleToUnloadingComplete(TbWcsShuttleOrder shuttleOrder,
                                                              List<TbWcsShuttleOrderItem> items,
                                                              WcsOrderHandler handler) {
        logger.info("[ Ecs ][ Callback ] toUnloadingComplete - orderKey={}", shuttleOrder.getOrderKey());
        handler.handleToUnloadingComplete(shuttleOrder, items);
        return EcsCallbackApi.Response.success("To unloading complete");
    }

    /**
     * COMPLETE — OUTBOUND 는 sub 별 finalize 정책 분기, 그 외는 즉시 셔틀 종료.
     */
    private EcsCallbackApi.Response handleCompleted(TbWcsShuttleOrder shuttleOrder,
                                                    List<TbWcsShuttleOrderItem> items,
                                                    WcsOrderHandler handler) {
        // OUTBOUND finalize 정책 분기
        if (OrderType.OUTBOUND.matches(shuttleOrder.getOrderType())) {
            SubOrderType sub = SubOrderType.fromOrNormal(shuttleOrder.getSubOrderType());

            // 자동 finalize 군 (SAMPLE_OUT / SAMPLE_DISCARD / DISPOSAL_OUT) — 박스 스캔 없이 즉시 finalize
            if (sub.isAutoFinalize()) {
                if (sub == SubOrderType.DISPOSAL_OUT) {
                    outboundReservationService.finalizeDisposalOutbound(
                            shuttleOrder.getEqGroupId(),
                            shuttleOrder.getCarryingStockId(),
                            shuttleOrder.getFromLocCode());
                }
                logger.info("[ Ecs ][ Callback ] auto finalize - orderKey={}, sub={}",
                        shuttleOrder.getOrderKey(), sub.code());
                return doCompleteShuttle(shuttleOrder, items, handler);
            }

            // NORMAL / PARTIAL_OUT — ARRIVED 마킹까지만. 박스 finalize 는 작업자 [출고 확정] 시 OutboundFinalizer 가 처리
            shuttleOrderStateWriter.markArrived(shuttleOrder);
            logger.info("[ Ecs ][ Callback ] arrived - awaiting outbound finalize. orderKey={}, sub={}",
                    shuttleOrder.getOrderKey(), sub.code());

            // 통로 비움 + 포트 도착 → 형제 셔틀 진행 안전
            shuttleOrderStateWriter.wakeUpDependents(shuttleOrder.getOrderKey());

            return EcsCallbackApi.Response.success("Awaiting outbound finalize");
        }
        return doCompleteShuttle(shuttleOrder, items, handler);
    }

    /**
     * 셔틀 COMPLETED 공통 처리 — 박스 finalize(OUTBOUND), handler.handleCompletion, 상태 전이, 의존성 해소, host 통보.
     */
    private EcsCallbackApi.Response doCompleteShuttle(TbWcsShuttleOrder shuttleOrder,
                                                      List<TbWcsShuttleOrderItem> items,
                                                      WcsOrderHandler handler) {
        // OUTBOUND 박스 finalize 보장 (picked_qty → remaining_qty 차감 + reset, idempotent)
        if (OrderType.OUTBOUND.matches(shuttleOrder.getOrderType())) {
            outboundFinalizer.finalizeBoxesForOutbound(shuttleOrder);
        }

        // handler 후처리 + 셔틀 COMPLETED 전이
        handler.handleCompletion(shuttleOrder, items);
        shuttleOrderStateWriter.markCompleted(shuttleOrder);

        // 의존성 해소 — prereq/parent WAITING 후속들 wake
        shuttleOrderStateWriter.wakeUpDependents(shuttleOrder.getOrderKey());

        // host 완료 평가 + 통보 이벤트
        boolean hostCompleted = hostOrderCompletion.onShuttleCompleted(shuttleOrder);
        if (hostCompleted) {
            eventPublisher.publishEvent(
                    WcsHostCallbackEvent.ofCompleted(shuttleOrder.getOrderKey(), shuttleOrder));
        } else {
            logger.info("[ Ecs ][ Callback ] completed - host pending. orderKey={}, hostOrderKey={}",
                    shuttleOrder.getOrderKey(), shuttleOrder.getHostOrderKey());
        }

        // 포트 트래픽 감소
        portTrafficController.decrementByOrder(shuttleOrder);
        return EcsCallbackApi.Response.success("Order completed");
    }

    /**
     * 랙 컨베이어 도착 — handler 위임 + HOST 통보 이벤트.
     */
    private EcsCallbackApi.Response handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder,
                                                              WcsOrderHandler handler) {
        logger.info("[ Ecs ][ Callback ] rackConveyorArrived - orderKey={}", shuttleOrder.getOrderKey());
        handler.handleRackConveyorArrived(shuttleOrder);
        eventPublisher.publishEvent(
                WcsHostCallbackEvent.ofRackConveyorArrived(shuttleOrder.getOrderKey()));
        return EcsCallbackApi.Response.success("Order Rack Conveyor Arrived Completed");
    }

    /**
     * ERROR — 논리 에러(이중입고/공출고)는 별도 처리, 그 외는 하드웨어 에러 흐름.
     */
    private EcsCallbackApi.Response handleFailed(TbWcsShuttleOrder shuttleOrder,
                                                 WcsOrderHandler handler,
                                                 EcsCallbackApi.Request request) {
        String errorCode = request.getErrorCode();
        String errorMessage = request.getMessage();
        String errorType = request.getType();

        logger.error("[ Ecs ][ Callback ] failed - orderKey={}, type={}, errorCode={}, errorMessage={}",
                shuttleOrder.getOrderKey(), errorType, errorCode, errorMessage);

        // 논리 에러(이중입고/공출고) 통합 처리
        if ("DOUBLE_IN".equals(errorType) || "EMPTY_OUT".equals(errorType)) {
            return handleLogicalError(shuttleOrder, errorType, errorCode, errorMessage);
        }

        // 하드웨어 에러
        handler.handleFailure(shuttleOrder, errorCode, errorMessage);
        shuttleOrderStateWriter.markErrorHardware(shuttleOrder);

        eventPublisher.publishEvent(
                WcsHostCallbackEvent.ofFailed(shuttleOrder.getOrderKey(), errorCode, errorMessage));
        portTrafficController.decrementByOrder(shuttleOrder);
        return EcsCallbackApi.Response.success("Hardware failure recorded");
    }

    /**
     * 논리 에러 처리 — 대상 로케이션 격리 + ERROR_INVENTORY 마킹 + HOST 실패 통보.
     * DOUBLE_IN: 목적지 격리 / EMPTY_OUT: 출발지 격리.
     */
    private EcsCallbackApi.Response handleLogicalError(TbWcsShuttleOrder shuttleOrder,
                                                       String errorType,
                                                       String errorCode,
                                                       String errorMessage) {
        String eqGroupId = shuttleOrder.getEqGroupId();
        String orderKey = shuttleOrder.getOrderKey();
        String targetLocId = "DOUBLE_IN".equals(errorType)
                ? shuttleOrder.getToLocCode()
                : shuttleOrder.getFromLocCode();

        // 로케이션 격리
        wcsLocationService.blockLocationWithSpecificTask(eqGroupId, targetLocId, errorType, orderKey);

        // 오더 ERROR_INVENTORY 마킹
        shuttleOrderStateWriter.markErrorInventory(shuttleOrder);

        // HOST 통보 이벤트 + 포트 감소
        eventPublisher.publishEvent(
                WcsHostCallbackEvent.ofFailed(orderKey, errorCode, errorMessage));
        portTrafficController.decrementByOrder(shuttleOrder);

        logger.error("[ Ecs ][ Callback ] logical error isolated - type={}, orderKey={}, locId={}",
                errorType, orderKey, targetLocId);
        return EcsCallbackApi.Response.success("Logical error isolated: " + errorType + ". Manual resolution required.");
    }

    /**
     * CANCELLED — handler 위임 + 셔틀 취소 마킹 + HOST 통보 이벤트.
     */
    private EcsCallbackApi.Response handleCancelled(TbWcsShuttleOrder shuttleOrder,
                                                    WcsOrderHandler handler) {
        logger.info("[ Ecs ][ Callback ] cancelled - orderKey={}", shuttleOrder.getOrderKey());

        handler.handleCancellation(shuttleOrder);
        shuttleOrderStateWriter.markCancelled(shuttleOrder);

        int newHostStatus = HostOrderStatus.CANCELLED.code();
        eventPublisher.publishEvent(
                WcsHostCallbackEvent.ofCancelled(shuttleOrder.getOrderKey(), newHostStatus));
        return EcsCallbackApi.Response.success("Order cancelled");
    }

    /**
     * orderType 으로 handler 매칭. 없으면 즉시 예외.
     */
    private WcsOrderHandler handlerFor(String orderType) {
        for (WcsOrderHandler h : orderHandlers) {
            if (h.supports(orderType)) return h;
        }
        throw new IllegalStateException("No handler for orderType=" + orderType);
    }
}
