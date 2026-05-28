package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CycleMode;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

@Service
public class KMat2026TspgCallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026TspgCallbackHandler.class);

    private static final int STATUS_COMPLETED = (int) ShuttleOrderStatusEnumCode.COMPLETED.code();

    @Autowired
    @Lazy
    private KMat2026WcsFacade wcsFacade;

    @Autowired
    private KMat2026ScenarioService scenarioService;

    @Autowired
    private TbWcsLocMstService tbWcsLocMstService;

    /**
     * TSPG 셔틀 오더 완료 콜백
     * - 타입별 실제 완료 순서 카운트는 유지
     * - step 개념 없이 바로 필요한 후속 처리만 수행
     */
    @Transactional
    public void handleComplete(TbWcsShuttleOrder order) {
        if (order == null) {
            return;
        }
        if (!isKMat2026Order(order)) {
            return;
        }

        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            logger.warn("[KMat2026TspgCallbackHandler] 활성 시나리오 없음 - key={}", order.getOrderKey());
            return;
        }
        if (!ctx.isRunning()) {
            logger.warn("[KMat2026TspgCallbackHandler] 시나리오 비실행 상태 - step={}, key={}",
                    ctx.getCurrentStep(), order.getOrderKey());
            return;
        }

        if (order.getOrderStatus() != STATUS_COMPLETED) {
            logger.debug("[KMat2026TspgCallbackHandler] COMPLETED 아님 - key={}, status={}",
                    order.getOrderKey(), order.getOrderStatus());
            return;
        }

        String orderKey = order.getOrderKey();

        int completedSeqInType = ctx.markOrderCompleted(orderKey);
        if (completedSeqInType < 0) {
            logger.warn("[KMat2026TspgCallbackHandler] 추적 대상 아님 - key={}", orderKey);
            return;
        }

        String trackedType = ctx.getOrderType(orderKey);

        logger.info("[KMat2026TspgCallbackHandler] 완료 콜백 - key={}, type={}, seqInType={}, cycle={}, mode={}",
                orderKey, trackedType, completedSeqInType, ctx.getCycleNumber(), ctx.getCycleMode());

        if ("OUTBOUND".equals(trackedType)) {
            routeOutboundComplete(ctx, orderKey, completedSeqInType);
            maybeCreateHoldMovesAfterLastStep1OrderCompleted(ctx, completedSeqInType);
        } else if ("MOVE".equals(trackedType)) {
            routeMoveComplete(ctx, orderKey, completedSeqInType);
            maybeCreateHoldMovesAfterLastStep1OrderCompleted(ctx, completedSeqInType);
        } else if ("INBOUND".equals(trackedType)) {
            routeInboundComplete(ctx, orderKey, completedSeqInType);
        } else {
            logger.warn("[KMat2026TspgCallbackHandler] 알 수 없는 trackedType={}", trackedType);
        }

        wcsFacade.saveToDb();
    }

    private void maybeCreateHoldMovesAfterLastStep1OrderCompleted(KMat2026ScenarioContext ctx,
                                                                  int completedSeqInType) {
        if (ctx == null) {
            return;
        }

        if (ctx.getCycleMode() != CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            return;
        }

        if (completedSeqInType != 2) {
            return;
        }

        int completedStep1Count = ctx.getCompletedOutboundCount() + ctx.getCompletedMoveCount();
        if (completedStep1Count != 4) {
            return;
        }

        logger.info("[KMat2026TspgCallbackHandler] step1 마지막 완료 감지 - outboundCompleted={}, moveCompleted={}, inboundCompleted={}",
                ctx.getCompletedOutboundCount(),
                ctx.getCompletedMoveCount(),
                ctx.getCompletedInboundCount());

        scenarioService.createHoldMovesIfInboundNotCompletedInsertOnly(ctx);
    }

    /**
     * OUTBOUND 완료
     * - 첫 번째 / 두 번째 구분 없이 동일 처리
     * - ECS 출고단 FULL 갱신 후 AGF 출고 호출
     */
    private void routeOutboundComplete(KMat2026ScenarioContext ctx, String orderKey, int completedSeqInType) {
        String fromLoc = ctx.getOrderFromLoc(orderKey);
        String role = ctx.getOrderRole(orderKey);

        logger.info("[KMat2026TspgCallbackHandler] OUTBOUND 완료 - key={}, role={}, seq={}, fromLoc={}",
                orderKey, role, completedSeqInType, fromLoc);

        scenarioService.handleOutboundCompleted(ctx, orderKey);
    }

    /**
     * MOVE 완료
     * - CAN_MOVE 모드에서만 유효
     * - 첫 번째 완료 시 ECS 입고단 EMPTY 갱신
     * - 두 번째 완료 시 렉단 컨베이어 도착 콜백 대기
     */
    private void routeMoveComplete(KMat2026ScenarioContext ctx, String orderKey, int completedSeqInType) {
        if (ctx.getCycleMode() != CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            logger.warn("[KMat2026TspgCallbackHandler] MOVE 완료인데 현재 모드는 move 없음 - mode={}, key={}",
                    ctx.getCycleMode(), orderKey);
            return;
        }

        String role = ctx.getOrderRole(orderKey);
        logger.info("[KMat2026TspgCallbackHandler] MOVE 완료 - key={}, role={}, seq={}",
                orderKey, role, completedSeqInType);

        if (completedSeqInType == 1) {
            logger.info("[KMat2026TspgCallbackHandler] MOVE 1번째 완료");
//            scenarioService.markEcsInboundPortEmpty();
            return;
        }

        if (completedSeqInType == 2) {
            logger.info("[KMat2026TspgCallbackHandler] MOVE 2번째 완료");
            return;
        }

        logger.warn("[KMat2026TspgCallbackHandler] MOVE seq 비정상 - key={}, seq={}", orderKey, completedSeqInType);
    }

    /**
     * INBOUND 완료
     * - 첫 번째 완료: ECS 입고단 EMPTY 갱신
     * - 두 번째 완료: 현재 cycle 종료 후 다음 cycle 시작
     */
    private void routeInboundComplete(KMat2026ScenarioContext ctx, String orderKey, int completedSeqInType) {
        String role = ctx.getOrderRole(orderKey);

        logger.info("[KMat2026TspgCallbackHandler] INBOUND 완료 - key={}, role={}, seq={}",
                orderKey, role, completedSeqInType);

        if (completedSeqInType == 1) {
            scenarioService.markEcsInboundPortEmpty();
            return;
        }

        if (completedSeqInType == 2) {
            finishCurrentCycleAndStartNext(ctx);
            return;
        }

        logger.warn("[KMat2026TspgCallbackHandler] INBOUND seq 비정상 - key={}, seq={}", orderKey, completedSeqInType);
    }

    /**
     * 현재 cycle 종료 후 cursor 갱신, 다음 cycle 시작
     */
    private void finishCurrentCycleAndStartNext(KMat2026ScenarioContext ctx) {
        logger.info("[KMat2026TspgCallbackHandler] cycle 종료 시작 - cycle={}, mode={}",
                ctx.getCycleNumber(), ctx.getCycleMode());

        updateFloor1OutboundCursor(ctx);
        updateFloor2MoveCursor(ctx);
        updateFloor2InboundCursor(ctx);

        ctx.advanceToNextCycle();

        try {
            wcsFacade.startNextCycle(ctx);
        } catch (Exception e) {
            logger.error("[KMat2026TspgCallbackHandler] 다음 cycle 시작 실패", e);
            ctx.onError("다음 cycle 시작 실패: " + e.getMessage());
            wcsFacade.saveToDb();
        }
    }

    /**
     * ECS 렉단 컨베이어 도착 콜백
     * - CAN_MOVE 모드에서만 유효
     * - 첫 번째 도착: 기록만
     * - 두 번째 도착:
     *   * inbound1 미생성 -> waiting=true
     *   * inbound1 생성됨 -> 즉시 ECS 입고 명령 전송
     */
    // @Transactional
    public void handleConveyorArrived(String orderKey) {
        logger.info("[KMat2026TspgCallbackHandler] 렉단 컨베이어 도착 - orderKey={}", orderKey);

        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null || !ctx.isRunning()) {
            return;
        }

        if (ctx.getCycleMode() != CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            logger.info("[KMat2026TspgCallbackHandler] 현재 mode={} 이므로 conveyorArrived 무시", ctx.getCycleMode());
            return;
        }

        String trackedType = ctx.getOrderType(orderKey);
        if (!"MOVE".equals(trackedType)) {
            logger.warn("[KMat2026TspgCallbackHandler] MOVE 오더 아님 - key={}", orderKey);
            return;
        }

        int seq = ctx.markMoveConveyorArrived(orderKey);
        if (seq < 0) {
            return;
        }

        if (seq == 1) {
            logger.info("[KMat2026TspgCallbackHandler] MOVE 첫 번째 conveyor 도착 - 기록만");
            wcsFacade.saveToDb();
            return;
        }

        if (seq != 2) {
            logger.warn("[KMat2026TspgCallbackHandler] conveyorArrived seq 비정상 - key={}, seq={}", orderKey, seq);
            wcsFacade.saveToDb();
            return;
        }

        if (ctx.getInbound1OrderKey() == null) {
            ctx.setConveyorArrivedWaiting(true);
            logger.info("[KMat2026TspgCallbackHandler] inbound1 미생성 → conveyorArrivedWaiting=true");
            wcsFacade.saveToDb();
            return;
        }

        String inboundKey = ctx.getInbound1OrderKey();
        String toLoc = ctx.getOrderToLoc(inboundKey);

        if (ValueUtil.isEmpty(toLoc)) {
            ctx.onError("conveyorArrived: inbound1 목적지 없음 - key=" + inboundKey);
            wcsFacade.saveToDb();
            return;
        }

        logger.info("[KMat2026TspgCallbackHandler] second move conveyor 도착 + inbound1 존재 → 즉시 ECS 입고 요청");
        if(sendInboundCommand(inboundKey, toLoc)){
            ctx.setConveyorArrivedWaiting(false);
            wcsFacade.saveToDb();
        }
    }

    /**
     * CAN_MOVE 모드에서 inbound1 생성 직후 호출
     * - second move conveyor 도착이 이미 끝났으면 즉시 ECS 입고 요청
     * - 아니면 대기
     */
    // @Transactional
    public void handleInboundCreatedInMoveMode(KMat2026ScenarioContext ctx, String inboundOrderKey) {
        if (ctx == null || !ctx.isRunning()) {
            return;
        }

        if (ctx.getCycleMode() != CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            return;
        }

        if (!ctx.isConveyorArrivedWaiting()) {
            logger.info("[KMat2026TspgCallbackHandler] inbound 생성됨 - 아직 second move conveyor 도착 전");
            return;
        }

        String toLoc = ctx.getOrderToLoc(inboundOrderKey);
        if (ValueUtil.isEmpty(toLoc)) {
            ctx.onError("handleInboundCreatedInMoveMode: inbound 목적지 없음 - key=" + inboundOrderKey);
            return;
        }

        logger.info("[KMat2026TspgCallbackHandler] inbound 생성 + conveyor 대기 중 → 즉시 ECS 입고 요청");
        if(sendInboundCommand(inboundOrderKey, toLoc)){
            ctx.setConveyorArrivedWaiting(false);
            wcsFacade.saveToDb();
        }
    }

    private boolean sendInboundCommand(String inboundOrderKey, String toLoc) {
        boolean isSuccess = false;
        try {
            String fromLoc = KMat2026LocationMapping.INBOUND_PORT;

            logger.info("[KMat2026TspgCallbackHandler] ECS 입고 명령 전송 - orderKey={}, from={}, to={}",
                    inboundOrderKey, fromLoc, toLoc);

            scenarioService.agfInboundPointEmptyAndSendInboundCommand(inboundOrderKey);
            isSuccess = true;
        }catch (Exception e) {
            logger.error(e.toString());
            isSuccess = false;
        }
        return  isSuccess;
    }

    private void updateFloor1OutboundCursor(KMat2026ScenarioContext ctx) {
        String orderKey = ctx.getOutbound2OrderKey();
        if (ValueUtil.isEmpty(orderKey)) {
            logger.warn("[KMat2026TspgCallbackHandler] floor1 outbound cursor 갱신 스킵 - outbound2OrderKey 없음");
            return;
        }

        String fromLoc = ctx.getOrderFromLoc(orderKey);
        if (ValueUtil.isEmpty(fromLoc)) {
            logger.warn("[KMat2026TspgCallbackHandler] floor1 outbound cursor 갱신 스킵 - fromLoc 없음, key={}", orderKey);
            return;
        }

        TbWcsLocMst loc = tbWcsLocMstService.findByEqGroupIdAndLocCode(KMat2026LocationMapping.EQ_GROUP_ID, fromLoc);
        if (loc == null || loc.getLocSeq() == null) {
            logger.warn("[KMat2026TspgCallbackHandler] floor1 outbound cursor 갱신 스킵 - loc/locSeq 없음, fromLoc={}", fromLoc);
            return;
        }

        ctx.setLastFloor1OutboundLocSeq(loc.getLocSeq());
        logger.info("[KMat2026TspgCallbackHandler] floor1 outbound cursor 갱신 - fromLoc={}, locSeq={}",
                fromLoc, loc.getLocSeq());
    }

    private void updateFloor2MoveCursor(KMat2026ScenarioContext ctx) {
        if (ctx.getCycleMode() != CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            return;
        }

        String orderKey = ctx.getMove2OrderKey();
        if (ValueUtil.isEmpty(orderKey)) {
            logger.warn("[KMat2026TspgCallbackHandler] floor2 move cursor 갱신 스킵 - move2OrderKey 없음");
            return;
        }

        String fromLoc = ctx.getOrderFromLoc(orderKey);
        if (ValueUtil.isEmpty(fromLoc)) {
            logger.warn("[KMat2026TspgCallbackHandler] floor2 move cursor 갱신 스킵 - fromLoc 없음, key={}", orderKey);
            return;
        }

        TbWcsLocMst loc = tbWcsLocMstService.findByEqGroupIdAndLocCode(KMat2026LocationMapping.EQ_GROUP_ID, fromLoc);
        if (loc == null || loc.getLocSeq() == null) {
            logger.warn("[KMat2026TspgCallbackHandler] floor2 move cursor 갱신 스킵 - loc/locSeq 없음, fromLoc={}", fromLoc);
            return;
        }

        ctx.setLastFloor2MoveLocSeq(loc.getLocSeq());
        logger.info("[KMat2026TspgCallbackHandler] floor2 move cursor 갱신 - fromLoc={}, locSeq={}",
                fromLoc, loc.getLocSeq());
    }

    private void updateFloor2InboundCursor(KMat2026ScenarioContext ctx) {
        if (ctx.getCycleMode() != CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            return;
        }

        String orderKey = ctx.getInbound2OrderKey();
        if (ValueUtil.isEmpty(orderKey)) {
            logger.warn("[KMat2026TspgCallbackHandler] floor2 inbound cursor 갱신 스킵 - inbound2OrderKey 없음");
            return;
        }

        String toLoc = ctx.getOrderToLoc(orderKey);
        if (ValueUtil.isEmpty(toLoc)) {
            logger.warn("[KMat2026TspgCallbackHandler] floor2 inbound cursor 갱신 스킵 - toLoc 없음, key={}", orderKey);
            return;
        }

        TbWcsLocMst loc = tbWcsLocMstService.findByEqGroupIdAndLocCode(KMat2026LocationMapping.EQ_GROUP_ID, toLoc);
        if (loc == null || loc.getLocSeq() == null) {
            logger.warn("[KMat2026TspgCallbackHandler] floor2 inbound cursor 갱신 스킵 - loc/locSeq 없음, toLoc={}", toLoc);
            return;
        }

        ctx.setLastFloor2InboundLocSeq(loc.getLocSeq());
        logger.info("[KMat2026TspgCallbackHandler] floor2 inbound cursor 갱신 - toLoc={}, locSeq={}",
                toLoc, loc.getLocSeq());
    }

    private boolean isKMat2026Order(TbWcsShuttleOrder order) {
        return KMat2026LocationMapping.EQ_GROUP_ID.equals(order.getEqGroupId());
    }
}