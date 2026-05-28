package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.ProcessStatus;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.InternalEcsCallbackService;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.context.DomainContext;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class KMat2026ScenarioTestService {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026ScenarioTestService.class);

    @Value("${wcs.api.base-url:http://localhost:9500}")
    private String wcsBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Lazy
    private KMat2026WcsFacade wcsFacade;

    @Autowired
    private InternalEcsCallbackService internalEcsCallbackService;

    @Autowired
    private TbWcsOrderService tbWcsOrderService;

    @Autowired
    private TbWcsShuttleOrderService tbWcsShuttleOrderService;

    private static final String AGF_CALLBACK = "/rest/tbecsamhstask/7/robot/callback";

    private long callbackDelayMs = 3000L;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile AutoTestMode currentMode = AutoTestMode.BASIC;

    private volatile String currentVariantName = "NONE";

    private volatile int lastExecutedCycleNo = 0;

    private enum AutoTestMode {
        BASIC,
        VARIED,
        REVERSE
    }

    private enum CycleVariant {
        OUTBOUND2_FIRST_STANDARD,
        OUTBOUND1_FIRST_STANDARD,
        EARLY_CONVEYOR_BEFORE_OUTBOUND2,
        LATE_CONVEYOR_AFTER_MOVE_COMPLETE
    }

    public void runAutoCallbacks(long delayMs) {
        startAsync(delayMs, AutoTestMode.BASIC);
    }

    public void runVariedCallbacks(long delayMs) {
        startAsync(delayMs, AutoTestMode.VARIED);
    }

    public void runReverseOrderTest(long delayMs) {
        startAsync(delayMs, AutoTestMode.REVERSE);
    }

    public void sendStep1LastComplete(String orderKey) {
        executeWithDomain(() -> {
            KMat2026ScenarioContext before = wcsFacade.getContext();
            if (before == null) {
                logger.warn("[ScenarioTest] step1 마지막 완료 테스트 실패 - context 없음");
                return;
            }

            logger.info("[ScenarioTest] step1 마지막 완료 테스트 시작 - orderKey={}", orderKey);
            logger.info("[ScenarioTest] 완료 전 카운트 - outboundCompleted={}, moveCompleted={}, inboundCompleted={}",
                    before.getCompletedOutboundCount(),
                    before.getCompletedMoveCount(),
                    before.getCompletedInboundCount());

            internalEcsCallbackService.complete(orderKey);

            KMat2026ScenarioContext after = wcsFacade.getContext();
            if (after != null) {
                logger.info("[ScenarioTest] 완료 후 카운트 - outboundCompleted={}, moveCompleted={}, inboundCompleted={}",
                        after.getCompletedOutboundCount(),
                        after.getCompletedMoveCount(),
                        after.getCompletedInboundCount());
            }
        });
    }

    private void startAsync(long delayMs, AutoTestMode mode) {
        if (isRunning.getAndSet(true)) {
            logger.warn("[ScenarioTest] 이미 테스트가 실행 중입니다");
            return;
        }

        this.callbackDelayMs = delayMs;
        this.currentMode = mode;
        this.currentVariantName = mode.name();
        this.lastExecutedCycleNo = 0;

        final Domain capturedDomain = resolveExecutionDomain();

        executor.submit(() -> {
            try {
                Domain.setCurrentDomain(capturedDomain);

                logger.info("[ScenarioTest] ===== 자동 테스트 시작 =====");
                logger.info("[ScenarioTest] mode={}, delay={}ms", currentMode, callbackDelayMs);
                logger.info("[ScenarioTest] 실행 도메인: id={}, name={}",
                        capturedDomain != null ? capturedDomain.getId() : null,
                        capturedDomain != null ? capturedDomain.getName() : null);

                sleep(callbackDelayMs);

                int cycleNo = 1;

                while (isRunning.get()) {
                    lastExecutedCycleNo = cycleNo;
                    logger.info("[ScenarioTest] ===== {}사이클 시작 ===== mode={}", cycleNo, currentMode);

                    switch (currentMode) {
                        case BASIC -> {
                            currentVariantName = "BASIC_DEFAULT";
                            runOneCycleCallbacks();
                        }
                        case VARIED -> {
                            CycleVariant variant = selectCycleVariant(cycleNo);
                            currentVariantName = variant.name();
                            logger.info("[ScenarioTest] ===== {}사이클 패턴 ===== {}", cycleNo, currentVariantName);
                            runOneCycleVaried(variant);
                        }
                        case REVERSE -> {
                            currentVariantName = "REVERSE_ORDER";
                            runOneCycleReverseOrder();
                        }
                    }

                    logger.info("[ScenarioTest] ===== {}사이클 종료 ===== mode={}, variant={}",
                            cycleNo, currentMode, currentVariantName);

                    cycleNo++;
                    sleep(callbackDelayMs);
                }

                logger.info("[ScenarioTest] ===== 자동 테스트 완료 =====");
            } catch (Exception e) {
                logger.error("[ScenarioTest] 자동 테스트 실행 중 오류", e);
            } finally {
                DomainContext.unsetAll();
                isRunning.set(false);
                currentVariantName = "NONE";
            }
        });
    }

    private CycleVariant selectCycleVariant(int cycleNo) {
        CycleVariant[] variants = CycleVariant.values();
        return variants[(cycleNo - 1) % variants.length];
    }

    private void runOneCycleCallbacks() {
        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            logger.error("[ScenarioTest] Context 없음");
            return;
        }

        logger.info("[ScenarioTest] ========== Step1 TSPG 콜백 시작 ==========");

        completeOrder("outbound2", ctx.getOutbound2OrderKey());
        completeOrder("outbound1", ctx.getOutbound1OrderKey());

        logger.info("[ScenarioTest] ========== AGF 출고 콜백 시작 ==========");

        sendAgfFlow("Step2 출고단→입고단", "K_MAT_TSPG_CONVEYOR_INBOUND");
        sendAgfFlow("Step3 출고단→버퍼", "K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND");

        ctx = refreshContextOrReturn("move 처리 전 최신 Context 없음");
        if (ctx == null) return;

        String move2Key = ctx.getMove2OrderKey();
        String move1Key = ctx.getMove1OrderKey();

        sendConveyorArrivedInternal("move2", move2Key);
        sendConveyorArrivedInternal("move1", move1Key);

        completeOrder("move1", move1Key);
        completeOrder("move2", move2Key);

        // step1 마지막 완료 후 생성된 hold move가 있으면 입고 전에 먼저 완료
        completeGeneratedHoldMovesBeforeInbound();

        // AGF 버퍼→입고단 없음
        // 두 번째 입고도 출고단→입고단으로만 테스트
        sendAgfFlow("AGF 출고단→입고단(두번째)", "K_MAT_TSPG_CONVEYOR_INBOUND");

        completeInboundOrders();
        logger.info("[ScenarioTest] ========== 1사이클 완료 ==========");
    }

    private void runOneCycleVaried(CycleVariant variant) {
        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            logger.error("[ScenarioTest] Context 없음");
            return;
        }

        logger.info("[ScenarioTest] ========== VARIED Step1 TSPG 콜백 시작 ==========");
        logger.info("[ScenarioTest] variant={}", variant);

        switch (variant) {
            case OUTBOUND2_FIRST_STANDARD -> runVariantOutbound2FirstStandard(ctx);
            case OUTBOUND1_FIRST_STANDARD -> runVariantOutbound1FirstStandard(ctx);
            case EARLY_CONVEYOR_BEFORE_OUTBOUND2 -> runVariantEarlyConveyorBeforeOutbound2(ctx);
            case LATE_CONVEYOR_AFTER_MOVE_COMPLETE -> runVariantLateConveyorAfterMoveComplete(ctx);
        }

        logger.info("[ScenarioTest] ========== VARIED 1사이클 완료 - variant={} ==========", variant);
    }

    private void runVariantOutbound2FirstStandard(KMat2026ScenarioContext ctx) {
        completeOrder("outbound2", ctx.getOutbound2OrderKey());
        completeOrder("outbound1", ctx.getOutbound1OrderKey());

        sendAgfFlow("Step2/3 AGF 출고단→입고단", "K_MAT_TSPG_CONVEYOR_INBOUND");
        sendAgfFlow("Step3 AGF 출고단→버퍼", "K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND");

        ctx = refreshContextOrReturn("variant1 move 처리 전 Context 없음");
        if (ctx == null) return;

        sendConveyorArrivedInternal("move2", ctx.getMove2OrderKey());
        sendConveyorArrivedInternal("move1", ctx.getMove1OrderKey());

        completeOrder("move1", ctx.getMove1OrderKey());
        completeOrder("move2", ctx.getMove2OrderKey());

        completeGeneratedHoldMovesBeforeInbound();

        // AGF 버퍼→입고단 없음
        sendAgfFlow("AGF 출고단→입고단(두번째)", "K_MAT_TSPG_CONVEYOR_INBOUND");

        completeInboundOrders();
    }

    private void runVariantOutbound1FirstStandard(KMat2026ScenarioContext ctx) {
        completeOrder("outbound1", ctx.getOutbound1OrderKey());
        completeOrder("outbound2", ctx.getOutbound2OrderKey());

        sendAgfFlow("Step2/3 AGF 출고단→입고단", "K_MAT_TSPG_CONVEYOR_INBOUND");
        sendAgfFlow("Step3 AGF 출고단→버퍼", "K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND");

        ctx = refreshContextOrReturn("variant2 move 처리 전 Context 없음");
        if (ctx == null) return;

        sendConveyorArrivedInternal("move1", ctx.getMove1OrderKey());
        sendConveyorArrivedInternal("move2", ctx.getMove2OrderKey());

        completeOrder("move1", ctx.getMove1OrderKey());
        completeOrder("move2", ctx.getMove2OrderKey());

        completeGeneratedHoldMovesBeforeInbound();

        // AGF 버퍼→입고단 없음
        sendAgfFlow("AGF 출고단→입고단(두번째)", "K_MAT_TSPG_CONVEYOR_INBOUND");

        completeInboundOrders();
    }

    private void runVariantEarlyConveyorBeforeOutbound2(KMat2026ScenarioContext ctx) {
        completeOrder("outbound1", ctx.getOutbound1OrderKey());

        ctx = refreshContextOrReturn("variant3 early conveyor 전 Context 없음");
        if (ctx == null) return;

        sendConveyorArrivedInternal("move2(early)", ctx.getMove2OrderKey());
        sendConveyorArrivedInternal("move1(early)", ctx.getMove1OrderKey());

        sendAgfFlow("AGF 출고단→입고단(첫번째, early)", "K_MAT_TSPG_CONVEYOR_INBOUND");

        ctx = refreshContextOrReturn("variant3 outbound2 처리 전 Context 없음");
        if (ctx == null) return;
        completeOrder("outbound2", ctx.getOutbound2OrderKey());

        sendAgfFlow("AGF 출고단→버퍼", "K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND");

        ctx = refreshContextOrReturn("variant3 move complete 전 Context 없음");
        if (ctx == null) return;

        completeOrder("move1", ctx.getMove1OrderKey());
        completeOrder("move2", ctx.getMove2OrderKey());

        completeGeneratedHoldMovesBeforeInbound();

        // AGF 버퍼→입고단 없음
        sendAgfFlow("AGF 출고단→입고단(두번째)", "K_MAT_TSPG_CONVEYOR_INBOUND");

        completeInboundOrders();
    }

    private void runVariantLateConveyorAfterMoveComplete(KMat2026ScenarioContext ctx) {
        completeOrder("outbound2", ctx.getOutbound2OrderKey());
        completeOrder("outbound1", ctx.getOutbound1OrderKey());

        sendAgfFlow("AGF 출고단→입고단(첫번째)", "K_MAT_TSPG_CONVEYOR_INBOUND");
        sendAgfFlow("AGF 출고단→버퍼", "K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND");

        ctx = refreshContextOrReturn("variant4 move 처리 전 Context 없음");
        if (ctx == null) return;

        completeOrder("move1", ctx.getMove1OrderKey());
        completeOrder("move2", ctx.getMove2OrderKey());

        sendConveyorArrivedInternal("move1(late)", ctx.getMove1OrderKey());
        sendConveyorArrivedInternal("move2(late)", ctx.getMove2OrderKey());

        completeGeneratedHoldMovesBeforeInbound();

        // AGF 버퍼→입고단 없음
        sendAgfFlow("AGF 출고단→입고단(두번째)", "K_MAT_TSPG_CONVEYOR_INBOUND");

        completeInboundOrders();
    }

    private void runOneCycleReverseOrder() {
        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            logger.error("[ScenarioTest] Context 없음");
            return;
        }

        logger.info("[ScenarioTest] ========== [역순] Step1 TSPG 콜백 시작 ==========");

        completeOrderWithPrefix("[역순] outbound1", ctx.getOutbound1OrderKey());

        ctx = wcsFacade.getContext();
        String move1Key = ctx != null ? ctx.getMove1OrderKey() : null;
        completeOrderWithPrefix("[역순] move1", move1Key);

        ctx = wcsFacade.getContext();
        String move2Key = ctx != null ? ctx.getMove2OrderKey() : null;
        completeOrderWithPrefix("[역순] move2", move2Key);

        if (move2Key != null && isRunning.get()) {
            sleep(callbackDelayMs);
            logger.info("[ScenarioTest] [역순] ★ move2 렉단 컨베이어 도착 (AGF 입고단 도착 전!) ★");
            logger.info("[ScenarioTest] [역순] → 예상: conveyorArrivedWaiting = true");
            internalEcsCallbackService.conveyorArrived(move2Key);

            ctx = wcsFacade.getContext();
            if (ctx != null) {
                logger.info("[ScenarioTest] [역순] → 실제: conveyorArrivedWaiting = {}", ctx.isConveyorArrivedWaiting());
            }
        }

        if (isRunning.get()) {
            sleep(callbackDelayMs);
            logger.info("[ScenarioTest] [역순] ★ AGF 10601→입고단 완료 (outbound2 완료 전!) ★");
            logger.info("[ScenarioTest] [역순] → 예상: inbound1 생성 + conveyorArrivedWaiting 처리");
            sendAgfCallbackByCommandType("K_MAT_TSPG_CONVEYOR_INBOUND");

            ctx = wcsFacade.getContext();
            if (ctx != null) {
                logger.info("[ScenarioTest] [역순] → 실제: conveyorArrivedWaiting = {}",
                        ctx.isConveyorArrivedWaiting());
                logger.info("[ScenarioTest] [역순] → inbound1OrderKey = {}", ctx.getInbound1OrderKey());
            }
        }

        ctx = wcsFacade.getContext();
        String outbound2Key = ctx != null ? ctx.getOutbound2OrderKey() : null;
        if (outbound2Key != null && isRunning.get()) {
            sleep(callbackDelayMs);
            logger.info("[ScenarioTest] [역순] outbound2 완료 콜백: {}", outbound2Key);
            internalEcsCallbackService.complete(outbound2Key);
        }

        if (isRunning.get()) {
            sleep(callbackDelayMs);
            logger.info("[ScenarioTest] [역순] AGF 10602→버퍼 완료");
            sendAgfCallbackByCommandType("K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND");
        }

        // 역순 테스트에서도 hold move가 생겼으면 먼저 완료
        completeGeneratedHoldMovesBeforeInbound();

        ctx = wcsFacade.getContext();
        String inbound1Key = ctx != null ? ctx.getInbound1OrderKey() : null;
        completeOrderWithPrefix("[역순] inbound1", inbound1Key);

        if (isRunning.get()) {
            sleep(callbackDelayMs);
            logger.info("[ScenarioTest] [역순] AGF 출고단→입고단 완료 (두번째)");
            sendAgfCallbackByCommandType("K_MAT_TSPG_CONVEYOR_INBOUND");
        }

        ctx = wcsFacade.getContext();
        String inbound2Key = ctx != null ? ctx.getInbound2OrderKey() : null;
        completeOrderWithPrefix("[역순] inbound2", inbound2Key);

        logger.info("[ScenarioTest] ========== [역순] 1사이클 완료 ==========");
    }

    private KMat2026ScenarioContext refreshContextOrReturn(String errorMessage) {
        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            logger.error("[ScenarioTest] {}", errorMessage);
            return null;
        }
        return ctx;
    }

    private void completeOrder(String label, String orderKey) {
        completeOrderWithPrefix(label, orderKey);
    }

    private void completeOrderWithPrefix(String label, String orderKey) {
        if (orderKey == null || !isRunning.get()) {
            logger.warn("[ScenarioTest] {} 완료 스킵 - orderKey={}", label, orderKey);
            return;
        }

        sleep(callbackDelayMs);
        logger.info("[ScenarioTest] {} 완료 콜백: {}", label, orderKey);
        internalEcsCallbackService.complete(orderKey);
    }

    private void sendConveyorArrivedInternal(String label, String orderKey) {
        if (orderKey == null || !isRunning.get()) {
            logger.warn("[ScenarioTest] {} 렉단 컨베이어 도착 스킵 - orderKey={}", label, orderKey);
            return;
        }

        sleep(callbackDelayMs);
        logger.info("[ScenarioTest] {} 렉단 컨베이어 도착 콜백: {}", label, orderKey);
        internalEcsCallbackService.conveyorArrived(orderKey);
    }

    private void sendAgfFlow(String label, String commandType) {
        if (!isRunning.get()) {
            return;
        }

        sleep(callbackDelayMs);
        logger.info("[ScenarioTest] {} 콜백", label);
        sendAgfCallbackByCommandType(commandType);
    }

    private void completeInboundOrders() {
        KMat2026ScenarioContext ctx = refreshContextOrReturn("inbound 처리 전 Context 없음");
        if (ctx == null) return;

        String inbound1Key = ctx.getInbound1OrderKey();
        completeOrder("inbound1", inbound1Key);

        ctx = refreshContextOrReturn("inbound2 처리 전 Context 없음");
        if (ctx == null) return;

        String inbound2Key = ctx.getInbound2OrderKey();
        completeOrder("inbound2", inbound2Key);
    }

    /**
     * step1 마지막 완료 후 생성된 hold move가 있으면
     * 입고 완료 전에 먼저 처리한다.
     *
     * 순서:
     * 1) to=10603 주문 완료
     * 2) from=10603 주문 완료
     */
    private void completeGeneratedHoldMovesBeforeInbound() {
        if (!isRunning.get()) {
            return;
        }

        TbWcsShuttleOrder to10603 = waitForGeneratedMoveOrder(null, "10603", 10, 200);
        if (to10603 == null) {
            logger.info("[ScenarioTest] 생성된 hold move 없음 - to=10603");
            return;
        }

        logger.info("[ScenarioTest] 생성된 hold move 발견(to=10603) - orderKey={}, from={}, to={}",
                to10603.getOrderKey(), to10603.getFromLocCode(), to10603.getToLocCode());

        completeOrder("holdMove-to-10603", to10603.getOrderKey());

        TbWcsShuttleOrder from10603 = waitForGeneratedMoveOrder("10603", null, 10, 200);
        if (from10603 == null) {
            logger.warn("[ScenarioTest] 생성된 복귀 hold move 없음 - from=10603");
            return;
        }

        logger.info("[ScenarioTest] 생성된 hold move 발견(from=10603) - orderKey={}, from={}, to={}",
                from10603.getOrderKey(), from10603.getFromLocCode(), from10603.getToLocCode());

        completeOrder("holdMove-from-10603", from10603.getOrderKey());
    }

    private TbWcsShuttleOrder waitForGeneratedMoveOrder(String fromLocCode,
                                                        String toLocCode,
                                                        int maxRetry,
                                                        long sleepMs) {
        for (int i = 0; i < maxRetry && isRunning.get(); i++) {
            TbWcsShuttleOrder order = findGeneratedMoveOrder(fromLocCode, toLocCode);
            if (order != null) {
                return order;
            }
            sleep(sleepMs);
        }
        return null;
    }

    private TbWcsShuttleOrder findGeneratedMoveOrder(String fromLocCode, String toLocCode) {
        try {
            List<TbWcsShuttleOrder> candidates =
                    tbWcsShuttleOrderService.findByEqGroupIdAndOrderType("K_MAT_TSPG", "MOVE");

            if (candidates == null || candidates.isEmpty()) {
                return null;
            }

            for (TbWcsShuttleOrder order : candidates) {
                if (order == null) {
                    continue;
                }

                if (order.getOrderStatus() == 90) {
                    continue;
                }

                if (fromLocCode != null && !fromLocCode.equals(order.getFromLocCode())) {
                    continue;
                }

                if (toLocCode != null && !toLocCode.equals(order.getToLocCode())) {
                    continue;
                }

                if ("10603".equals(order.getFromLocCode()) || "10603".equals(order.getToLocCode())) {
                    return order;
                }
            }
        } catch (Exception e) {
            logger.error("[ScenarioTest] 생성된 hold move 조회 실패 - from={}, to={}", fromLocCode, toLocCode, e);
        }

        return null;
    }

    private void sendAgfCallbackByCommandType(String commandType) {
        try {
            List<TbWcsOrder> orders =
                    tbWcsOrderService.findByCommandTypeAndProcessStatus(commandType, ProcessStatus.READY);

            if (orders == null || orders.isEmpty()) {
                logger.warn("[ScenarioTest] AGF 작업 없음 - commandType={}", commandType);
                return;
            }

            TbWcsOrder order = orders.get(0);
            String taskId = order.getOrderId();
            String toPosition = order.getToPositionCod();

            logger.info("[ScenarioTest] AGF 콜백 전송 - taskId={}, to={}, commandType={}",
                    taskId, toPosition, commandType);

            sendAgfStart(taskId, toPosition);
            sleep(1000);

            sendAgfFromLoadingComplete(taskId, toPosition);
            sleep(1000);

            sendAgfComplete(taskId, toPosition != null ? toPosition : "UNKNOWN");
            sleep(1000);
        } catch (Exception e) {
            logger.error("[ScenarioTest] AGF 콜백 조회 실패 - commandType={}", commandType, e);
        }
    }

    public void sendTspgComplete(String orderKey) {
        executeWithDomain(() -> {
            logger.info("[ScenarioTest] TSPG 완료 콜백 전송 - orderKey={}", orderKey);
            internalEcsCallbackService.complete(orderKey);
        });
    }

    public void sendConveyorArrived(String orderKey) {
        executeWithDomain(() -> {
            logger.info("[ScenarioTest] 렉단 컨베이어 도착 콜백 전송 - orderKey={}", orderKey);
            internalEcsCallbackService.conveyorArrived(orderKey);
        });
    }

    public void sendAgfCallback(String taskId, String status, String currentPositionCode, String robotCode) {
        String url = wcsBaseUrl + AGF_CALLBACK;

        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("status", status);
        body.put("currentPositionCode", currentPositionCode);
        body.put("errorCode", "0");
        body.put("robotCode", robotCode);

        logger.info("[ScenarioTest] AGF 콜백 전송 - taskId={}, status={}, position={}",
                taskId, status, currentPositionCode);

        sendPostRequest(url, body);
    }

    public void sendAgfStart(String taskId, String currentPositionCode) {
        TbWcsOrder order = tbWcsOrderService.findOrder(taskId);
        if (ValueUtil.isEmpty(order)) {
            logger.info("[ScenarioTest] order is null - taskId={}", taskId);
            return;
        }
        sendAgfCallback(taskId, "1", order.getFromPositionCod(), "AGF1");
    }

    public void sendAgfFromLoadingComplete(String taskId, String currentPositionCode) {
        TbWcsOrder order = tbWcsOrderService.findOrder(taskId);
        if (ValueUtil.isEmpty(order)) {
            logger.info("[ScenarioTest] order is null - taskId={}", taskId);
            return;
        }
        sendAgfCallback(taskId, "2", order.getFromPositionCod(), "AGF1");
    }

    public void sendAgfComplete(String taskId, String currentPositionCode) {
        TbWcsOrder order = tbWcsOrderService.findOrder(taskId);
        if (ValueUtil.isEmpty(order)) {
            logger.info("[ScenarioTest] order is null - taskId={}", taskId);
            return;
        }
        sendAgfCallback(taskId, "4", order.getToPositionCod(), "AGF1");
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public String getCurrentMode() {
        return currentMode.name();
    }

    public String getCurrentVariantName() {
        return currentVariantName;
    }

    public int getLastExecutedCycleNo() {
        return lastExecutedCycleNo;
    }

    public void stop() {
        isRunning.set(false);
        logger.info("[ScenarioTest] 테스트 중지 요청");
    }

    private void executeWithDomain(Runnable runnable) {
        Domain domain = resolveExecutionDomain();
        Domain.setCurrentDomain(domain);
        try {
            runnable.run();
        } finally {
            DomainContext.unsetAll();
        }
    }

    private Domain resolveExecutionDomain() {
        try {
            return Domain.currentDomain();
        } catch (Exception e) {
            logger.warn("[ScenarioTest] currentDomain 없음. systemDomain 사용. reason={}", e.getMessage());
            Domain systemDomain = Domain.systemDomain();

            if (systemDomain == null) {
                throw new RuntimeException("System Domain Not Exist!");
            }

            return systemDomain;
        }
    }

    private void sendPostRequest(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, request, String.class);

            logger.debug("[ScenarioTest] 응답: {}", response);
        } catch (Exception e) {
            logger.error("[ScenarioTest] 요청 실패 - url={}", url, e);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[ScenarioTest] sleep interrupted");
        }
    }

    public void completeEcsTask(){}
}