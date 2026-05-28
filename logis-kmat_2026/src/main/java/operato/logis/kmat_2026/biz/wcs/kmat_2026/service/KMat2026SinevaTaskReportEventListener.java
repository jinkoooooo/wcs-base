package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.CbkStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.CommandType;
import operato.logis.kmat_2026.biz.ecs.sineva.event.SinevaTaskReportEvent;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CycleMode;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.EcsCommandService;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

@Component
public class KMat2026SinevaTaskReportEventListener {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026SinevaTaskReportEventListener.class);

    @Autowired
    private KMat2026LocationService locationService;

    @Autowired
    @Lazy
    private KMat2026WcsFacade wcsFacade;

    @Autowired
    private KMat2026ScenarioService scenarioService;

    @Autowired
    private EcsCommandService ecsCommandService;

    @Autowired
    @Lazy
    private KMat2026TspgCallbackHandler tspgCallbackHandler;

    @EventListener
    public void handleSinevaTaskReportEvent(SinevaTaskReportEvent event) {
        CommandType commandType = event.getCommandType();
        if (!isKMatTspgConveyorCommand(commandType)) {
            return;
        }

        CbkStatus cbkStatus = event.getCbkStatus();

        logger.debug("[KMat2026SinevaTaskReportEventListener] event - orderId={}, cbkStatus={}, commandType={}",
                event.getOrderId(), cbkStatus, commandType);

        switch (cbkStatus) {
            case FINISH_LOADING:
                handleFinishLoading(event);
                break;
            case END:
                handleEnd(event);
                break;
            case ERROR:
                handleError(event);
                break;
            default:
                break;
        }
    }

    private void handleFinishLoading(SinevaTaskReportEvent event) {
        String fromEcsLoc = event.getFromSide();

        if (KMat2026LocationMapping.isEcsOutboundLoc(fromEcsLoc)) {
            locationService.updateWcsOutboundLocToEmptyByEcsLoc(fromEcsLoc);
            logger.info("[KMat2026SinevaTaskReportEventListener] 출고단 EMPTY 갱신 - fromEcsLoc={}", fromEcsLoc);
        }
    }

    private void handleEnd(SinevaTaskReportEvent event) {
        TbWcsOrder order = event.getOrder();
        if (order == null) {
            logger.warn("[KMat2026SinevaTaskReportEventListener] END인데 order 없음");
            return;
        }

        if ("TSPG_CONV_IN_01".equals(order.getToSide())) {
            handleInboundTaskComplete(event);
        }
    }

    /**
     * AGF가 입고단(TSPG_CONV_IN_01)까지 가져왔을 때
     *
     * 규칙:
     * 1. 그 시점에 inbound target 조회
     * 2. inbound 오더 생성
     * 3. 무조건 refill 호출
     * 4. CAN_NOT_MOVE면 즉시 ECS 입고 호출
     * 5. CAN_MOVE면
     *    - inbound1: second move conveyor 도착 여부에 따라 즉시/대기
     *    - inbound2: 즉시 ECS 입고 호출
     */
    private void handleInboundTaskComplete(SinevaTaskReportEvent event) {
        logger.info("[KMat2026SinevaTaskReportEventListener] AGF 입고단 도착 완료 - orderId={}", event.getOrderId());

        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null || !ctx.isRunning()) {
            logger.warn("[KMat2026SinevaTaskReportEventListener] 활성 시나리오 없음 또는 비실행 상태");
            return;
        }

        boolean isFirstInbound = (ctx.getInbound1OrderKey() == null);
        boolean isSecondInbound = (!isFirstInbound && ctx.getInbound2OrderKey() == null);

        if (!isFirstInbound && !isSecondInbound) {
            logger.warn("[KMat2026SinevaTaskReportEventListener] inbound1/inbound2 모두 이미 생성됨");
            return;
        }

        String fromLoc = KMat2026LocationMapping.INBOUND_PORT;
        int priority = isFirstInbound ? 5 : 10;

        String targetLoc = scenarioService.findInboundTargetLoc(ctx);
        if (ValueUtil.isEmpty(targetLoc)) {
            ctx.onError("AGF 입고단 도착: inbound target 조회 실패 - mode=" + ctx.getCycleMode());
            wcsFacade.saveToDb();
            return;
        }

        String inboundKey = scenarioService.createInboundOrder(fromLoc, targetLoc, priority);
        if (ValueUtil.isEmpty(inboundKey)) {
            ctx.onError("AGF 입고단 도착: inbound 생성 실패 - to=" + targetLoc);
            wcsFacade.saveToDb();
            return;
        }

        if (isFirstInbound) {
            ctx.registerInbound1Order(inboundKey, targetLoc);
            logger.info("[KMat2026SinevaTaskReportEventListener] inbound1 생성 - key={}, to={}, mode={}",
                    inboundKey, targetLoc, ctx.getCycleMode());
        } else {
            ctx.registerInbound2Order(inboundKey, targetLoc);
            logger.info("[KMat2026SinevaTaskReportEventListener] inbound2 생성 - key={}, to={}, mode={}",
                    inboundKey, targetLoc, ctx.getCycleMode());
        }

        if (ctx.getCycleMode() == CycleMode.FLOOR_2_SHUTTLE_CAN_NOT_MOVE) {
            logger.info("[KMat2026SinevaTaskReportEventListener] CAN_NOT_MOVE 모드 - 즉시 ECS 입고 요청");
            scenarioService.agfInboundPointEmptyAndSendInboundCommand(inboundKey);
            wcsFacade.saveToDb();
            return;
        }

        if (isFirstInbound) {
            logger.info("[KMat2026SinevaTaskReportEventListener] CAN_MOVE 모드 - inbound1 생성 후 conveyor 도착 여부 확인");
            tspgCallbackHandler.handleInboundCreatedInMoveMode(ctx, inboundKey);
        } else {
            logger.info("[KMat2026SinevaTaskReportEventListener] CAN_MOVE 모드 - inbound2 즉시 ECS 입고 요청");
            scenarioService.agfInboundPointEmptyAndSendInboundCommand(inboundKey);
        }

        wcsFacade.saveToDb();
    }

    private void handleError(SinevaTaskReportEvent event) {
        logger.warn("[KMat2026SinevaTaskReportEventListener] ERROR - orderId={}, errorCode={}",
                event.getOrderId(), event.getErrorCode());
    }

    private boolean isKMatTspgConveyorCommand(CommandType commandType) {
        return commandType == CommandType.K_MAT_TSPG_CONVEYOR_INBOUND
                || commandType == CommandType.K_MAT_TSPG_CONVEYOR_OUTBOUND
                || commandType == CommandType.K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND
                || commandType == CommandType.K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND;
    }
}