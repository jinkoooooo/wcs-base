package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.kmat_2026.biz.ecs.sineva.SinevaEcsFacade;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.*;
import operato.logis.kmat_2026.biz.ecs.sineva.event.SinevaTaskReportEvent;
import operato.logis.kmat_2026.entity.TbEcsLocMst;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import operato.logis.kmat_2026.service.impl.TbEcsTaskProcessService;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.Optional;

/**
 * ============================================================================
 * Equip Callback Routing Service
 * ============================================================================
 *
 * [역할]
 * - 기존 AgvProcessService 정책을 유지한 callback routing 본체
 *
 * [중요]
 * - cbkStatus 기준 분기
 * - commandType 기준 후속 execute/callback 분기
 * - skip 해제 후 createOrReleaseWcsOrder 재실행 정책 유지
 */
@Service
public class EquipCallbackRoutingService {

    private static final Logger logger = LoggerFactory.getLogger(EquipCallbackRoutingService.class);

    @Autowired
    protected TbWcsOrderService tbWcsOrderService;

    @Autowired
    protected OrderCommandService orderCommandService;

    @Autowired
    protected TbEcsTaskProcessService tbEcsTaskProcessService;

    @Autowired
    protected SinevaEcsFacade sinevaEcsFacade;

    @Autowired
    protected TbEcsLocMstService tbEcsLocMstService;

    @Autowired
    protected LocationStateService locationStateService;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Transactional
    public void route(TbWcsOrder order, String errorCode) throws InterruptedException {
        CbkStatus cbkStatus = CbkStatus.fromCode(order.getCbkStatus());

        switch (cbkStatus) {
            case SUCCESS:
                handleTaskComplete(order);
                break;
            case END:
                handleTaskEnd(order);
                break;
            case IN_PROGRESS:
                handleTaskInProgress(order);
                break;
            case FINISH_LOADING:
                handleFinishFromSideLoading(order);
                break;
            case ERROR:
                handleTaskError(order, errorCode);
                break;
            case ERROR_RECOVERY:
                handleTaskErrorRecovery(order);
                break;
            default:
                logger.warn("[EquipCallbackRoutingService] unknown cbkStatus - orderId={}, cbkStatus={}",
                        order.getOrderId(), order.getCbkStatus());
        }

        // AGF 콜백 이벤트 발행 (KMAT 등 다른 모듈에서 수신 가능)
        publishSinevaTaskReportEvent(order, cbkStatus, errorCode);
    }

    @Transactional
    public void handleTaskComplete(TbWcsOrder order) throws InterruptedException {
        tbEcsTaskProcessService.saveResTaskCallback(order, "TASK_SUCCESS");

        if (!tbWcsOrderService.isTaskInProgress(order)) {
            throw new ElidomRuntimeException(
                    "작업 완료 처리 불가 - orderId=" + order.getOrderId() +
                            ", processStatus=" + order.getProcessStatus()
            );
        }

        CommandType commandType = CommandType.fromCode(order.getCommandType());

        switch (commandType) {
            case FREIGHT_MOVE: {
                Boolean isSkipped = orderCommandService.unskipTask(order);
                locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.FULL);

                if (Boolean.TRUE.equals(isSkipped)) {
                    orderCommandService.createOrReleaseWcsOrder(order);
                }
                break;
            }

            default:
                logger.warn("[handleTaskComplete] unsupported commandType - orderId={}, commandType={}",
                        order.getOrderId(), commandType.getCode());
        }
    }

    @Transactional
    public void handleTaskEnd(TbWcsOrder order) throws InterruptedException {
        tbEcsTaskProcessService.saveResTaskCallback(order, "TASK_END");

        CommandType commandType = CommandType.fromCode(order.getCommandType());

        if (EquipTaskType.SIMPLE_MOVE.getCode().equals(order.getTaskType())) {
            tbEcsLocMstService.unlockLocation(order.getFromPositionCod());
            tbEcsLocMstService.unlockLocation(order.getToPositionCod());
            tbEcsLocMstService.updateLocationAmrId(order.getToPositionCod(), order.getEquipId());
        }

        switch (commandType) {
            case K_MAT_TSPG_CONVEYOR_INBOUND:
            case K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND:
                sinevaEcsFacade.handleTspgConveyorInboundRefillCallback(order);
                break;
            case K_MAT_TSPG_CONVEYOR_OUTBOUND:
            case K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND:
                sinevaEcsFacade.handleTspgConveyorOutboundCallback(order);
                break;
            case FREIGHT_MOVE:
                handleMoveTask(order);
                break;

            case SIMPLE_MOVE:
                tbEcsLocMstService.unlockLocation(order.getFromPositionCod());
                tbEcsLocMstService.unlockLocation(order.getToPositionCod());
                orderCommandService.completeTask(order);
                break;

            case MANUAL_COMMAND:
                handleManualCommandEnd(order);
                break;

            default:
                locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.FULL);
                orderCommandService.completeTask(order);
                break;
        }
    }

    private void handleMoveTask(TbWcsOrder order) throws InterruptedException {
        Boolean isSkipped = orderCommandService.unskipTask(order);
        locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.FULL);

        CommandType commandType = CommandType.fromCode(order.getCommandType());

        switch (commandType) {
            default:
                orderCommandService.completeTask(order);
                break;
        }

        Optional.ofNullable(isSkipped)
                .filter(Boolean.TRUE::equals)
                .ifPresent(x -> orderCommandService.createOrReleaseWcsOrder(order));
    }

    private void handleManualCommandEnd(TbWcsOrder order) {
        String fromPositionCd = order.getFromPositionCod();
        String toPositionCd = order.getToPositionCod();

        if (ValueUtil.isNotEmpty(fromPositionCd) && ValueUtil.isNotEmpty(toPositionCd)) {
            TbEcsLocMst fromLocationObj = tbEcsLocMstService.selectLocationPointsByLocationCd(fromPositionCd);
            TbEcsLocMst toLocationObj = tbEcsLocMstService.selectLocationPointsByLocationCd(toPositionCd);

            if ("AMRSRT".equals(fromLocationObj.getGroupCd()) &&
                    "AMRSRT".equals(toLocationObj.getGroupCd())) {
                locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.FULL);
            } else {
                locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.POD);
            }
        } else {
            locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.POD);
        }

        TbEcsLocMst toLocation = tbEcsLocMstService.selectLocationPointsByLocationCd(order.getToPositionCod());
        if (ValueUtil.isNotEmpty(toLocation)) {
            if (!toLocation.getGroupCd().equals("HR_P") || !toLocation.getGroupCd().equals("PODSTG")) {
                orderCommandService.completeTask(order);
                return;
            }
        }

        order.setProcessStatus(ProcessStatus.AWAITING_FINAL_RELEASE.getCode());
    }

    private void handleTaskInProgress(TbWcsOrder order) {
        tbWcsOrderService.startRobotTask(order);
        locationStateService.updateLocationsOnRobotTaskStart(order);
        tbEcsTaskProcessService.saveResTaskCallback(order, "IN_PROGRESS");
    }

    private void handleFinishFromSideLoading(TbWcsOrder order) {
        tbWcsOrderService.updateProcessStatus(order, ProcessStatus.FINISH_FROM_SIDE_LOADING);
        locationStateService.updateLocationsOnRobotLoadingFinishOnFromSide(order);
        tbEcsTaskProcessService.saveResTaskCallback(order, ProcessStatus.FINISH_FROM_SIDE_LOADING.getDesc());
    }

    private void handleTaskError(TbWcsOrder order, String errorCode) {
        orderCommandService.handleErrorCallback(order, errorCode);
        tbEcsTaskProcessService.saveResTaskCallback(order, "ERROR");
    }

    private void handleTaskErrorRecovery(TbWcsOrder order) {
        orderCommandService.handleErrorRecoveryCallback(order);
        tbEcsTaskProcessService.saveResTaskCallback(order, "ERROR_RECOVERY");
    }

    /**
     * AGF 콜백 이벤트 발행
     * KMAT 등 다른 모듈에서 이 이벤트를 수신하여 추가 처리 가능
     */
    private void publishSinevaTaskReportEvent(TbWcsOrder order, CbkStatus cbkStatus, String errorCode) {
        try {
            SinevaTaskReportEvent event = new SinevaTaskReportEvent(this, order, cbkStatus, errorCode);
            eventPublisher.publishEvent(event);
            logger.debug("[EquipCallbackRoutingService] SinevaTaskReportEvent 발행: {}", event);
        } catch (Exception e) {
            logger.warn("[EquipCallbackRoutingService] SinevaTaskReportEvent 발행 실패: orderId={}, cbkStatus={}",
                    order.getOrderId(), cbkStatus, e);
        }
    }
}