package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import jakarta.annotation.PostConstruct;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CycleMode;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class KMat2026WcsFacade {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026WcsFacade.class);

    @Autowired
    private KMat2026ScenarioService scenarioService;

    @Autowired
    private KMat2026CyclePlanService cyclePlanService;

    @Autowired
    private KMat2026LocationService locationService;

    @Autowired
    private KMat2026ScenarioPersistenceService persistenceService;

    /**
     * 현재 메모리 context
     * - 재기동 시 DB에서 복원
     * - 실제 판단은 가능하면 DB 기준으로 하고, 여기 값은 런타임 반영/응답용으로 사용
     */
    private final AtomicReference<KMat2026ScenarioContext> currentContext = new AtomicReference<>();

    @PostConstruct
    public void init() {
        loadFromDb();
    }

    private void loadFromDb() {
        try {
            KMat2026ScenarioContext restored = persistenceService.load();
            if (restored != null) {
                currentContext.set(restored);
                logger.info("[KMat2026WcsFacade] DB 복원 완료 - scenarioId={}, step={}, cycle={}, mode={}",
                        restored.getScenarioId(),
                        restored.getCurrentStep(),
                        restored.getCycleNumber(),
                        restored.getCycleMode());
            } else {
                logger.info("[KMat2026WcsFacade] 저장된 시나리오 없음");
            }
        } catch (Exception e) {
            logger.error("[KMat2026WcsFacade] DB 복원 실패", e);
        }
    }

    public void saveToDb() {
        KMat2026ScenarioContext ctx = currentContext.get();
        if (ctx != null) {
            persistenceService.save(ctx);
        }
    }

    @Transactional
    public String startScenario() {
        logger.info("[KMat2026WcsFacade] startScenario");

        KMat2026ScenarioContext existing = currentContext.get();
        if (existing != null
                && existing.isRunning()
                && existing.getCurrentStep() != KMat2026ScenarioContext.ScenarioStep.INITIALIZED) {
            logger.warn("[KMat2026WcsFacade] 이미 실행 중 - scenarioId={}", existing.getScenarioId());
            return existing.getScenarioId();
        }

        KMat2026ScenarioContext ctx = KMat2026ScenarioContext.create();
        currentContext.set(ctx);

        startNextCycle(ctx);

        logger.info("[KMat2026WcsFacade] startScenario 완료 - scenarioId={}, cycle={}",
                ctx.getScenarioId(), ctx.getCycleNumber());

        return ctx.getScenarioId();
    }

    /**
     * 다음 cycle 시작
     * - 현재 cursor(lastFloor1OutboundLocSeq, lastFloor2MoveLocSeq)는 그대로 유지
     * - cycle mode만 결정
     * - 필요한 포인트들은 ScenarioService.createInitialOrders() 안에서 그때그때 DB 조회
     */
    @Transactional
    public void startNextCycle(KMat2026ScenarioContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("ScenarioContext is required");
        }

        logger.info("[KMat2026WcsFacade] startNextCycle - scenarioId={}, cycle={}, lastOutSeq={}, lastMoveSeq={}",
                ctx.getScenarioId(),
                ctx.getCycleNumber(),
                ctx.getLastFloor1OutboundLocSeq(),
                ctx.getLastFloor2MoveLocSeq());

        CycleMode mode = cyclePlanService.resolveCycleMode();
        ctx.startNewCycle(mode);

        logger.info("[KMat2026WcsFacade] cycle 시작 준비 완료 - cycle={}, mode={}",
                ctx.getCycleNumber(), ctx.getCycleMode());

        scenarioService.createInitialOrders(ctx);
        saveToDb();
    }

    public void stopScenario() {
        KMat2026ScenarioContext ctx = currentContext.get();
        if (ctx != null) {
            ctx.pause();
            saveToDb();
            logger.info("[KMat2026WcsFacade] 시나리오 정지 - scenarioId={}", ctx.getScenarioId());
        }
    }

    public void resumeScenario() {
        KMat2026ScenarioContext ctx = currentContext.get();
        if (ctx == null) {
            logger.warn("[KMat2026WcsFacade] 재개할 시나리오 없음");
            return;
        }

        if (ctx.getCurrentStep() != KMat2026ScenarioContext.ScenarioStep.PAUSED) {
            logger.warn("[KMat2026WcsFacade] 현재 PAUSED 아님 - step={}", ctx.getCurrentStep());
            return;
        }

        ctx.setCurrentStep(KMat2026ScenarioContext.ScenarioStep.INITIALIZED);
        saveToDb();

        logger.info("[KMat2026WcsFacade] 시나리오 재개 - scenarioId={}", ctx.getScenarioId());
    }

    public void resetScenario() {
        KMat2026ScenarioContext old = currentContext.getAndSet(null);
        if (old != null) {
            persistenceService.delete();
            logger.info("[KMat2026WcsFacade] 시나리오 리셋 - scenarioId={}", old.getScenarioId());
        }
    }

    public KMat2026ScenarioContext getContext() {
        return currentContext.get();
    }

    public ScenarioStatus getStatus() {
        KMat2026ScenarioContext ctx = currentContext.get();
        if (ctx == null) {
            return ScenarioStatus.notStarted();
        }

        return new ScenarioStatus(
                ctx.getScenarioId(),
                ctx.getCurrentStep() != null ? ctx.getCurrentStep().name() : null,
                ctx.getCycleNumber(),
                ctx.getCycleMode() != null ? ctx.getCycleMode().name() : null,
                ctx.getLastFloor1OutboundLocSeq(),
                ctx.getLastFloor2MoveLocSeq(),
                ctx.getActiveOrderMap().size(),
                ctx.getCreatedOrderCount(),
                ctx.getCompletedOutboundCount(),
                ctx.getCompletedMoveCount(),
                ctx.getCompletedInboundCount(),
                ctx.isConveyorArrivedWaiting(),
                ctx.isHasError(),
                ctx.getErrorMessage()
        );
    }

    public LocationStatus getLocationStatus() {
        return new LocationStatus(
                locationService.getFloor1LocStatuses(),
                locationService.getFloor2LocStatuses(),
                locationService.findAvailableOutboundPorts(),
                locationService.hasCargoAtInboundPort()
        );
    }

    @Transactional
    public void onAgfOutboundComplete(String orderKey) {
        logger.info("[KMat2026WcsFacade] onAgfOutboundComplete - orderKey={}", orderKey);
    }

    @Transactional
    public void onAgfInboundComplete(String orderKey) {
        logger.info("[KMat2026WcsFacade] onAgfInboundComplete - orderKey={}", orderKey);
    }

    public static class ScenarioStatus {
        private final String scenarioId;
        private final String step;
        private final int cycleNumber;
        private final String cycleMode;
        private final Integer lastFloor1OutboundLocSeq;
        private final Integer lastFloor2MoveLocSeq;
        private final int activeOrderCount;
        private final int createdOrderCount;
        private final int completedOutboundCount;
        private final int completedMoveCount;
        private final int completedInboundCount;
        private final boolean conveyorArrivedWaiting;
        private final boolean hasError;
        private final String errorMessage;

        public ScenarioStatus(String scenarioId,
                              String step,
                              int cycleNumber,
                              String cycleMode,
                              Integer lastFloor1OutboundLocSeq,
                              Integer lastFloor2MoveLocSeq,
                              int activeOrderCount,
                              int createdOrderCount,
                              int completedOutboundCount,
                              int completedMoveCount,
                              int completedInboundCount,
                              boolean conveyorArrivedWaiting,
                              boolean hasError,
                              String errorMessage) {
            this.scenarioId = scenarioId;
            this.step = step;
            this.cycleNumber = cycleNumber;
            this.cycleMode = cycleMode;
            this.lastFloor1OutboundLocSeq = lastFloor1OutboundLocSeq;
            this.lastFloor2MoveLocSeq = lastFloor2MoveLocSeq;
            this.activeOrderCount = activeOrderCount;
            this.createdOrderCount = createdOrderCount;
            this.completedOutboundCount = completedOutboundCount;
            this.completedMoveCount = completedMoveCount;
            this.completedInboundCount = completedInboundCount;
            this.conveyorArrivedWaiting = conveyorArrivedWaiting;
            this.hasError = hasError;
            this.errorMessage = errorMessage;
        }

        public static ScenarioStatus notStarted() {
            return new ScenarioStatus(
                    null,
                    "NOT_STARTED",
                    0,
                    null,
                    null,
                    null,
                    0,
                    0,
                    0,
                    0,
                    0,
                    false,
                    false,
                    null
            );
        }

        public String getScenarioId() { return scenarioId; }
        public String getStep() { return step; }
        public int getCycleNumber() { return cycleNumber; }
        public String getCycleMode() { return cycleMode; }
        public Integer getLastFloor1OutboundLocSeq() { return lastFloor1OutboundLocSeq; }
        public Integer getLastFloor2MoveLocSeq() { return lastFloor2MoveLocSeq; }
        public int getActiveOrderCount() { return activeOrderCount; }
        public int getCreatedOrderCount() { return createdOrderCount; }
        public int getCompletedOutboundCount() { return completedOutboundCount; }
        public int getCompletedMoveCount() { return completedMoveCount; }
        public int getCompletedInboundCount() { return completedInboundCount; }
        public boolean isConveyorArrivedWaiting() { return conveyorArrivedWaiting; }
        public boolean isHasError() { return hasError; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class LocationStatus {
        private final List<KMat2026LocationService.LocStatusInfo> floor1;
        private final List<KMat2026LocationService.LocStatusInfo> floor2;
        private final List<String> availableOutboundPorts;
        private final boolean hasCargoAtInboundPort;

        public LocationStatus(List<KMat2026LocationService.LocStatusInfo> floor1,
                              List<KMat2026LocationService.LocStatusInfo> floor2,
                              List<String> availableOutboundPorts,
                              boolean hasCargoAtInboundPort) {
            this.floor1 = floor1;
            this.floor2 = floor2;
            this.availableOutboundPorts = availableOutboundPorts;
            this.hasCargoAtInboundPort = hasCargoAtInboundPort;
        }

        public List<KMat2026LocationService.LocStatusInfo> getFloor1() { return floor1; }
        public List<KMat2026LocationService.LocStatusInfo> getFloor2() { return floor2; }
        public List<String> getAvailableOutboundPorts() { return availableOutboundPorts; }
        public boolean isHasCargoAtInboundPort() { return hasCargoAtInboundPort; }
    }
}