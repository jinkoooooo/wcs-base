package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import operato.logis.kmat_2026.biz.ecs.sineva.SinevaEcsFacade;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.LocationStatus;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CycleMode;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveResponse;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommandItem;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.WcsOrderService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.EcsCommandService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.List;

@Service
public class KMat2026ScenarioService {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026ScenarioService.class);

    private static final int PRIORITY_FIRST = 5;
    private static final int PRIORITY_SECOND = 10;

    @Autowired
    private WcsOrderService wcsOrderService;

    @Autowired
    private KMat2026LocationService locationService;

    @Autowired
    private KMat2026CyclePlanService cyclePlanService;

    @Autowired
    private SinevaEcsFacade sinevaEcsFacade;

    @Autowired
    private TbEcsLocMstService tbEcsLocMstService;

    @Autowired
    protected EcsCommandService ecsCommandService;

    /**
     * cycle 시작 시점에 현재 mode 기준으로 필요한 초기 오더만 생성한다.
     *
     * CAN_MOVE:
     * - 출고 2건
     * - move 2건
     *
     * CAN_NOT_MOVE:
     * - 출고 2건
     */
    @Transactional
    public void createInitialOrders(KMat2026ScenarioContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("ScenarioContext is required");
        }

        CycleMode mode = ctx.getCycleMode();
        if (mode == null) {
            ctx.onError("초기 오더 생성 실패: cycleMode 없음");
            return;
        }

        logger.info("[KMat2026ScenarioService] 초기 오더 생성 시작 - cycle={}, mode={}, lastOutSeq={}, lastMoveSeq={}",
                ctx.getCycleNumber(),
                mode,
                ctx.getLastFloor1OutboundLocSeq(),
                ctx.getLastFloor2MoveLocSeq());

        List<TbWcsLocMst> outboundSources =
                cyclePlanService.findNextFloor1OutboundSources(ctx.getLastFloor1OutboundLocSeq(), 2);

        validateSize("1층 출고 source", outboundSources, 2);

        String out1From = outboundSources.get(0).getLocCode();
        String out2From = outboundSources.get(1).getLocCode();

        String out1 = tspgMove(out1From, KMat2026LocationMapping.OUTBOUND_PORT_1, PRIORITY_FIRST);
        if (ValueUtil.isEmpty(out1)) {
            ctx.onError("초기 오더 생성 실패: outbound1 생성 실패 - from=" + out1From);
            return;
        }
        ctx.registerOutbound1Order(out1, out1From);

        String out2 = tspgMove(out2From, KMat2026LocationMapping.OUTBOUND_PORT_2, PRIORITY_SECOND);
        if (ValueUtil.isEmpty(out2)) {
            ctx.onError("초기 오더 생성 실패: outbound2 생성 실패 - from=" + out2From);
            return;
        }
        ctx.registerOutbound2Order(out2, out2From);

        logger.info("[KMat2026ScenarioService] 출고 2건 생성 완료 - out1={}, out2={}", out1, out2);

        if (mode == CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            List<TbWcsLocMst> moveSources =
                    cyclePlanService.findNextFloor2MoveSources(ctx.getLastFloor2MoveLocSeq(), 2);
            validateSize("2층 move source", moveSources, 2);

            List<TbWcsLocMst> moveTargets =
                    cyclePlanService.findSmallestEmptyFloor1(2);
            validateSize("1층 move target", moveTargets, 2);

            String mv1From = moveSources.get(0).getLocCode();
            String mv1To = moveTargets.get(0).getLocCode();

            String mv1 = tspgMove(mv1From, mv1To, PRIORITY_FIRST);
            if (ValueUtil.isEmpty(mv1)) {
                ctx.onError("초기 오더 생성 실패: move1 생성 실패 - from=" + mv1From + ", to=" + mv1To);
                return;
            }
            ctx.registerMove1Order(mv1, mv1From, mv1To);

            String mv2From = moveSources.get(1).getLocCode();
            String mv2To = moveTargets.get(1).getLocCode();

            String mv2 = tspgMove(mv2From, mv2To, PRIORITY_SECOND);
            if (ValueUtil.isEmpty(mv2)) {
                ctx.onError("초기 오더 생성 실패: move2 생성 실패 - from=" + mv2From + ", to=" + mv2To);
                return;
            }
            ctx.registerMove2Order(mv2, mv2From, mv2To);

            logger.info("[KMat2026ScenarioService] move 2건 생성 완료 - mv1={}, mv2={}", mv1, mv2);
        }

        logger.info("[KMat2026ScenarioService] 초기 오더 생성 완료 - cycle={}, mode={}",
                ctx.getCycleNumber(), mode);
    }

    /**
     * 출고 완료 시점 처리
     * - ECS 출고단 FULL 갱신
     * - 즉시 AGF 출고 호출
     *
     * 첫 번째 완료 / 두 번째 완료 구분 없이 동일 처리
     */
    @Transactional
    public void handleOutboundCompleted(KMat2026ScenarioContext ctx, String completedOutboundOrderKey) {
        if (ctx == null) {
            throw new IllegalArgumentException("ScenarioContext is required");
        }

        String outboundPort = ctx.getOrderToLoc(completedOutboundOrderKey);

        logger.info("[KMat2026ScenarioService] 출고 완료 처리 - key={}, outboundPort={}",
                completedOutboundOrderKey, outboundPort);

        if (ValueUtil.isEmpty(outboundPort)) {
            ctx.onError("출고 완료 처리 실패: outbound 목적지 조회 실패 - key=" + completedOutboundOrderKey);
            return;
        }

        locationService.updateEcsOutboundLocToFull(outboundPort);

        String ecsLoc = KMat2026LocationMapping.toEcsOutboundLoc(outboundPort);
        if (ValueUtil.isEmpty(ecsLoc)) {
            ctx.onError("출고 완료 처리 실패: ECS 출고단 매핑 없음 - port=" + outboundPort);
            return;
        }

        agfOutbound(ecsLoc);
    }

    /**
     * ECS 입고단 상태를 EMPTY로 갱신
     * 필요 시 move 완료 / inbound 완료 시점에서 호출
     */
    @Transactional
    public void markEcsInboundPortEmpty() {
        logger.info("[KMat2026ScenarioService] ECS 입고단 EMPTY 갱신");
        locationService.updateEcsInboundLocToEmpty(KMat2026LocationMapping.INBOUND_PORT);
    }

    /**
     * 입고 오더 목적지 조회
     *
     * CAN_MOVE:
     * - 2층 empty 1개 조회
     *
     * CAN_NOT_MOVE:
     * - 1층 empty 1개 조회
     */
    @Transactional(readOnly = true)
    public String findInboundTargetLoc(KMat2026ScenarioContext ctx) {

        if (ctx == null) {
            logger.info("findInboundTargetLoc KMat2026ScenarioContext is null");
            return null;
        }

        List<TbWcsLocMst> targets;

        CycleMode mode = ctx.getCycleMode();

        if(mode == null){
            logger.info("findInboundTargetLoc cycleMode is null");
            return null;
        }

        if (mode == CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE) {
            targets =
                    cyclePlanService.findNextFloor2InboundSources(ctx.getLastFloor2InboundLocSeq(), 1);
            validateSize("2층 inbound source", targets, 1);
        } else {
            targets = cyclePlanService.findSmallestEmptyFloor1(1);
        }

        if (targets == null || targets.isEmpty()) {
            logger.warn("[KMat2026ScenarioService] inbound target 없음 - mode={}", mode);
            return null;
        }

        String targetLoc = targets.get(0).getLocCode();
        logger.info("[KMat2026ScenarioService] inbound target 조회 - mode={}, targetLoc={}", mode, targetLoc);
        return targetLoc;
    }

    public void executeAgfInboundRefill() {
        String ecsInboundLoc = KMat2026LocationMapping.toEcsInboundLoc(KMat2026LocationMapping.INBOUND_PORT);
        if (ValueUtil.isEmpty(ecsInboundLoc)) {
            logger.warn("[KMat2026ScenarioService] AGF 입고 리필 스킵 - ECS 입고단 매핑 없음");
            return;
        }

        try {
            TbWcsOrder order = sinevaEcsFacade.handleTspgConveyorInboundRefillExecute(ecsInboundLoc);
            if (order != null) {
                logger.info("[KMat2026ScenarioService] AGF 입고 리필 지시 생성 - orderId={}", order.getOrderId());
            } else {
                logger.warn("[KMat2026ScenarioService] AGF 입고 리필 지시 결과 null");
            }
        } catch (Exception e) {
            logger.error("[KMat2026ScenarioService] AGF 입고 리필 실패 - ecsLoc={}", ecsInboundLoc, e);
        }
    }

    public String createInboundOrder(String fromLoc, String toLoc, int priority) {
        logger.info("[KMat2026ScenarioService] INBOUND 생성 - from={}, to={}, priority={}", fromLoc, toLoc, priority);
        return tspgInbound(fromLoc, toLoc, priority);
    }

    private void agfOutbound(String ecsLocCode) {
        try {
            TbWcsOrder order = sinevaEcsFacade.handleTspgConveyorOutboundExecute(ecsLocCode);

            if (order != null) {
                logger.info("[KMat2026ScenarioService] AGF 출고 호출 완료 - ecsLoc={}, orderId={}",
                        ecsLocCode, order.getOrderId());
            } else {
                logger.warn("[KMat2026ScenarioService] AGF 출고 호출 결과 null - ecsLoc={}", ecsLocCode);
            }
        } catch (Exception e) {
            logger.error("[KMat2026ScenarioService] AGF 출고 호출 실패 - ecsLoc={}", ecsLocCode, e);
        }
    }

    private String tspgMove(String fromLoc, String toLoc, int priority) {
        return executeWcsCommand("MOVE", fromLoc, toLoc, priority);
    }

    private String tspgInbound(String fromLoc, String toLoc, int priority) {
        return executeWcsCommand("INBOUND", fromLoc, toLoc, priority);
    }

    private String executeWcsCommand(String orderType, String fromLoc, String toLoc, Integer priority) {
        List<WcsOrderCommandItem> items = null;

        if ("INBOUND".equals(orderType)) {
            items = List.of(
                    WcsOrderCommandItem.builder()
                            .lineNo(1)
                            .skuCode("VIRTUAL_SKU")
                            .lotNo("VIRTUAL_LOT")
                            .qty(1)
                            .uom("EA")
                            .build()
            );
        }

        String orderKey = KMat2026LocationMapping.generateOrderKey(orderType, fromLoc);

        WcsOrderCommand command = WcsOrderCommand.builder()
                .sourceType(KMat2026LocationMapping.SOURCE_TYPE)
                .sourceSystemCode(KMat2026LocationMapping.SOURCE_SYSTEM_CODE)
                .sourceOrderKey(orderKey)
                .orderType(orderType)
                .eqGroupId(KMat2026LocationMapping.EQ_GROUP_ID)
                .fromLocCode(fromLoc)
                .toLocCode(toLoc)
                .persistHostOrder(false)
                .barCode(orderKey)
                .items(items)
                .priority(priority)
                .build();

        try {
            HostOrderReceiveResponse res = wcsOrderService.execute(command);
            if (res != null && res.isSuccess()) {
                logger.info("[KMat2026ScenarioService] WCS {} 성공 - {}→{}, priority={}, key={}",
                        orderType, fromLoc, toLoc, priority, res.getWcsOrderKey());
                return res.getWcsOrderKey();
            }

            logger.error("[KMat2026ScenarioService] WCS {} 실패 - {}→{}, error={}",
                    orderType, fromLoc, toLoc, res != null ? res.getErrorDesc() : "null");
        } catch (Exception e) {
            logger.error("[KMat2026ScenarioService] WCS {} 예외 - {}→{}", orderType, fromLoc, toLoc, e);
        }

        return null;
    }

    private void validateSize(String label, List<?> list, int required) {
        int actual = list == null ? 0 : list.size();
        if (actual < required) {
            throw new IllegalStateException(label + " 부족 - required=" + required + ", actual=" + actual);
        }
    }

    /**
     * agf 입고단 포인트 EMPTY 처리 및 입고 요청 진행
     */

    public void agfInboundPointEmptyAndSendInboundCommand(String orderKey){
        // agf 입고단 empty 처리
        tbEcsLocMstService.updatePodCdAndStatus("TSPG_CONV_IN_01", null, null, LocationStatus.EMPTY, null, null);

        // 입고 요청
        ecsCommandService.triggerInbound(orderKey);

        // agf 입고단 리필 요청
        executeAgfInboundRefill();
    }

    @Transactional
    public void createHoldMovesIfInboundNotCompletedInsertOnly(KMat2026ScenarioContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("ScenarioContext is required");
        }

        if (ctx.getCompletedInboundCount() >= 2) {
            logger.info("[KMat2026ScenarioService] 입고 2건 완료 상태이므로 hold move 생성 스킵");
            return;
        }

        TbWcsLocMst source = cyclePlanService.findLargestOccupiedFloor1();
        if (source == null) {
            logger.warn("[KMat2026ScenarioService] hold move 생성 스킵 - 1층 OCCUPIED source 없음");
            return;
        }

        String originalFrom = source.getLocCode();
        String waitPoint = "10603";

        logger.info("[KMat2026ScenarioService] hold move(insert-only) 생성 시작 - from={}, waitPoint={}",
                originalFrom, waitPoint);

        String moveTo10603 = executeInsertOnlyCommand("MOVE", originalFrom, waitPoint, PRIORITY_FIRST);
        if (ValueUtil.isEmpty(moveTo10603)) {
            logger.error("[KMat2026ScenarioService] hold move(insert-only) 실패 - {} -> {}", originalFrom, waitPoint);
            return;
        }

        String moveBack = executeInsertOnlyCommand("MOVE", waitPoint, originalFrom, PRIORITY_SECOND);
        if (ValueUtil.isEmpty(moveBack)) {
            logger.error("[KMat2026ScenarioService] hold move(insert-only) 실패 - {} -> {}", waitPoint, originalFrom);
            return;
        }

        logger.info("[KMat2026ScenarioService] hold move(insert-only) 생성 완료 - to10603={}, back={}, originalFrom={}",
                moveTo10603, moveBack, originalFrom);
    }

    private String executeInsertOnlyCommand(String orderType, String fromLoc, String toLoc, Integer priority) {
        List<WcsOrderCommandItem> items = null;

        if ("INBOUND".equals(orderType)) {
            items = List.of(
                    WcsOrderCommandItem.builder()
                            .lineNo(1)
                            .skuCode("VIRTUAL_SKU")
                            .lotNo("VIRTUAL_LOT")
                            .qty(1)
                            .uom("EA")
                            .build()
            );
        }

        String sourceOrderKey = KMat2026LocationMapping.generateOrderKey(orderType, fromLoc);

        WcsOrderCommand command = WcsOrderCommand.builder()
                .sourceType(KMat2026LocationMapping.SOURCE_TYPE)
                .sourceSystemCode(KMat2026LocationMapping.SOURCE_SYSTEM_CODE)
                .sourceOrderKey(sourceOrderKey)
                .orderType(orderType)
                .eqGroupId(KMat2026LocationMapping.EQ_GROUP_ID)
                .fromLocCode(fromLoc)
                .toLocCode(toLoc)
                .persistHostOrder(false)
                .barCode(sourceOrderKey)
                .items(items)
                .priority(priority)
                .build();


        try {
            HostOrderReceiveResponse res = wcsOrderService.executeInsertOnly(command);
            if (res != null && res.isSuccess()) {
                logger.info("[KMat2026ScenarioService] INSERT-ONLY WCS {} 성공 - {}→{}, priority={}, key={}",
                        orderType, fromLoc, toLoc, priority, res.getWcsOrderKey());
                return res.getWcsOrderKey();
            }

            logger.error("[KMat2026ScenarioService] INSERT-ONLY WCS {} 실패 - {}→{}, error={}",
                    orderType, fromLoc, toLoc, res != null ? res.getErrorDesc() : "null");
        } catch (Exception e) {
            logger.error("[KMat2026ScenarioService] INSERT-ONLY WCS {} 예외 - {}→{}", orderType, fromLoc, toLoc, e);
        }

        return null;
    }
}