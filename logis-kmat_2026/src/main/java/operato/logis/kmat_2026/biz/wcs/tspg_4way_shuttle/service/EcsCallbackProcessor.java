package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.kmat_2026.service.KMat2026TspgCallbackHandler;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsCallbackStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ErrorEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.HostOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.EcsCallbackRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.EcsCallbackResponse;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler.WcsOrderHandler;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * ECS 콜백의 상태별 후처리를 담당한다.
 *
 * Facade 에서 상태 분기와 후처리 상세를 분리해
 * receive / callback 흐름이 한눈에 보이도록 한다.
 */
@Service
public class EcsCallbackProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EcsCallbackProcessor.class);

    @Autowired
    private TbWcsShuttleOrderService shuttleOrderService;

    @Autowired
    private HostOrderPersistenceService hostOrderPersistenceService;

    @Autowired
    @Lazy
    private KMat2026TspgCallbackHandler kmat2026CallbackHandler;

    // Facade의 @Transactional에 참여하되, 독립 호출 시에도 트랜잭션이 보장되도록 명시
    @Transactional(propagation = Propagation.REQUIRED)
    public EcsCallbackResponse process(EcsCallbackRequest request,
                                       TbWcsShuttleOrder shuttleOrder,
                                       List<TbWcsShuttleOrderItem> items,
                                       WcsOrderHandler handler) {

        if (shuttleOrder.getOrderStatus() == ShuttleOrderStatusEnumCode.COMPLETED.codeAsIntOrNull()) {
            logger.warn("[DUP_CALLBACK] 이미 종료된 오더입니다. 중복 처리 방지. orderKey={}, status={}",
                    shuttleOrder.getOrderKey(), shuttleOrder.getOrderStatus());
            return EcsCallbackResponse.success("Already processed (idempotent)");
        }

        EcsCallbackStatusEnumCode status = EcsCallbackStatusEnumCode.from(request.getStatus());
        if (status == null) {
            return EcsCallbackResponse.fail(
                    ErrorEnumCode.INVALID_REQUEST.codeAsString(),
                    "Unknown status: " + request.getStatus()
            );
        }

        return switch (status) {
            case STARTED -> handleStarted(shuttleOrder);
            case COMPLETE -> handleCompleted(shuttleOrder, items, handler);
            case RACK_CONVEYOR_ARRIVED -> handleRackConveyorArrived(shuttleOrder, items, handler);
            case ERROR -> handleFailed(shuttleOrder, handler, request.getErrorCode(), request.getMessage());
            case CANCELLED -> handleCancelled(shuttleOrder, handler);
            case ACCEPTED, IN_PROGRESS -> EcsCallbackResponse.success("Callback received: " + status.codeAsString());
        };
    }

    private EcsCallbackResponse handleStarted(TbWcsShuttleOrder shuttleOrder) {
        logger.info("Handling STARTED callback. orderKey={}", shuttleOrder.getOrderKey());

        shuttleOrder.setOrderStatus((Integer) ShuttleOrderStatusEnumCode.RUNNING.code());
        shuttleOrderService.update(shuttleOrder);

        hostOrderPersistenceService.updateStatusByWcsOrderKey(
                shuttleOrder.getOrderKey(),
                (Integer) HostOrderStatusEnumCode.WAITING_EXEC.code()
        );

        return EcsCallbackResponse.success("Order started");
    }

    private EcsCallbackResponse handleCompleted(TbWcsShuttleOrder shuttleOrder,
                                                List<TbWcsShuttleOrderItem> items,
                                                WcsOrderHandler handler) {
        logger.info("Handling COMPLETED callback. orderKey={}", shuttleOrder.getOrderKey());

        handler.handleCompletion(shuttleOrder, items);
        shuttleOrder.setOrderStatus((Integer) ShuttleOrderStatusEnumCode.COMPLETED.code());
        shuttleOrderService.update(shuttleOrder, "orderStatus");

        hostOrderPersistenceService.updateStatusByWcsOrderKey(
                shuttleOrder.getOrderKey(),
                (Integer) HostOrderStatusEnumCode.COMPLETED.code()
        );

        // [수정 포인트 2] 트랜잭션 커밋 "후"에 실행되도록 예약 (데드락 방지)
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerKmat2026Complete(shuttleOrder);
                }
            });
        }

        return EcsCallbackResponse.success("Order completed");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void triggerKmat2026Complete(TbWcsShuttleOrder shuttleOrder) {
        try {
            kmat2026CallbackHandler.handleComplete(shuttleOrder);
        } catch (Exception e) {
            logger.error("KMAT 2026 handleComplete failed. orderKey={}, error={}",
                    shuttleOrder.getOrderKey(), e.getMessage(), e);
        }
    }

    private EcsCallbackResponse handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder,
                                                List<TbWcsShuttleOrderItem> items,
                                                WcsOrderHandler handler) {
        logger.info("Handling RACK CONVEYOR ARRIVED callback. orderKey={}", shuttleOrder.getOrderKey());

        handler.handleRackConveyorArrived(shuttleOrder);

        triggerKmat2026ConveyorArrived(shuttleOrder.getOrderKey());

        return EcsCallbackResponse.success("Order Rack Conveyor Arrived Completed");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void triggerKmat2026ConveyorArrived(String orderKey) {
        try {
            kmat2026CallbackHandler.handleConveyorArrived(orderKey);
        } catch (Exception e) {
            logger.error("KMAT 2026 handleConveyorArrived failed. orderKey={}, error={}",
                    orderKey, e.getMessage(), e);
        }
    }

    private EcsCallbackResponse handleFailed(TbWcsShuttleOrder shuttleOrder,
                                             WcsOrderHandler handler,
                                             String errorCode,
                                             String errorMessage) {
        logger.error(
                "Handling FAILED callback. orderKey={}, errorCode={}, errorMessage={}",
                shuttleOrder.getOrderKey(),
                errorCode,
                errorMessage
        );

        handler.handleFailure(shuttleOrder, errorCode, errorMessage);

        shuttleOrder.setOrderStatus((Integer) ShuttleOrderStatusEnumCode.ERROR_HARDWARE.code());
        shuttleOrderService.update(shuttleOrder);

        hostOrderPersistenceService.markErrorByWcsOrderKey(
                shuttleOrder.getOrderKey(),
                errorCode,
                errorMessage
        );

        return EcsCallbackResponse.success("Failure recorded");
    }

    private EcsCallbackResponse handleCancelled(TbWcsShuttleOrder shuttleOrder,
                                                WcsOrderHandler handler) {
        logger.info("Handling CANCELLED callback. orderKey={}", shuttleOrder.getOrderKey());

        handler.handleCancellation(shuttleOrder);

        shuttleOrder.setOrderStatus((Integer) ShuttleOrderStatusEnumCode.CANCELLED.code());
        shuttleOrderService.update(shuttleOrder);

        hostOrderPersistenceService.updateStatusByWcsOrderKey(
                shuttleOrder.getOrderKey(),
                (Integer) HostOrderStatusEnumCode.CANCELLED.code()
        );

        return EcsCallbackResponse.success("Order cancelled");
    }
}