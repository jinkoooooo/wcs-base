package operato.logis.kmat_2026.biz.wcs.kmat_2026.dto;

import lombok.Getter;
import lombok.Setter;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CycleMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class KMat2026ScenarioContext {

    protected Logger logger = LoggerFactory.getLogger(KMat2026ScenarioContext.class);

    private String scenarioId;
    private LocalDateTime startedAt;
    private ScenarioStep currentStep;

    /**
     * 단순 cycle 번호
     */
    private int cycleNumber;

    /**
     * 현재 cycle mode
     */
    private CycleMode cycleMode;

    /**
     * 다음 cycle 선택용 cursor
     * - 1층 출고 occupied 마지막 사용 loc_seq
     * - 2층 이동 occupied 마지막 사용 loc_seq
     */
    private Integer lastFloor1OutboundLocSeq;
    private Integer lastFloor2MoveLocSeq;
    private Integer lastFloor2InboundLocSeq;

    private String outbound1OrderKey;
    private String outbound2OrderKey;
    private String move1OrderKey;
    private String move2OrderKey;
    private String inbound1OrderKey;
    private String inbound2OrderKey;

    /**
     * 화면/상태 조회용 active order map
     * key = orderKey
     * value = "from→to"
     */
    private final Map<String, String> activeOrderMap = new ConcurrentHashMap<>();

    /**
     * 현재 cycle에서 생성된 오더 추적
     */
    private final List<CycleOrderTrack> cycleOrders = new ArrayList<>();
    private final Map<String, CycleOrderTrack> cycleOrderMap = new ConcurrentHashMap<>();

    /**
     * 타입별 완료 카운트
     */
    private int createdOrderCount;
    private int completedOutboundCount;
    private int completedMoveCount;
    private int completedInboundCount;

    /**
     * move 렉단 컨베이어 도착 카운트
     */
    private int conveyorArrivedMoveCount;

    /**
     * CAN_MOVE 모드에서만 사용
     * - second move conveyor 도착이 먼저 왔는데 inbound1이 아직 없으면 true
     * - inbound1 생성 후 이 값이 true면 바로 ECS 입고 요청
     */
    private boolean conveyorArrivedWaiting;

    private boolean hasError;
    private String errorMessage;

    public enum ScenarioStep {
        INITIALIZED,
        STEP1_RUNNING,
        STEP1_DONE,
        STEP2_DONE,
        STEP3_DONE,
        STEP4_DONE,
        STEP5_DONE,
        STEP6_DONE,
        PAUSED,
        ERROR
    }

    @Getter
    @Setter
    public static class CycleOrderTrack {
        private String orderKey;
        private String role;
        private String orderType;
        private String fromLoc;
        private String toLoc;
        private int createdSeq;
        private int completedSeqInType;
        private boolean completed;
        private boolean conveyorArrived;
        private int conveyorArrivedSeqInType;

        public CycleOrderTrack(String orderKey,
                               String role,
                               String orderType,
                               String fromLoc,
                               String toLoc,
                               int createdSeq) {
            this.orderKey = orderKey;
            this.role = role;
            this.orderType = orderType;
            this.fromLoc = fromLoc;
            this.toLoc = toLoc;
            this.createdSeq = createdSeq;
        }
    }

    public static KMat2026ScenarioContext create() {
        KMat2026ScenarioContext ctx = new KMat2026ScenarioContext();
        ctx.scenarioId = "KMAT_" + System.currentTimeMillis();
        ctx.startedAt = LocalDateTime.now();
        ctx.currentStep = ScenarioStep.INITIALIZED;
        ctx.cycleNumber = 1;
        ctx.cycleMode = null;
        ctx.hasError = false;
        ctx.lastFloor1OutboundLocSeq = 0;
        ctx.lastFloor2MoveLocSeq = 0;
        ctx.lastFloor2InboundLocSeq = 6;
        return ctx;
    }

    public void startNewCycle(CycleMode cycleMode) {
        this.cycleMode = cycleMode;
        this.currentStep = ScenarioStep.INITIALIZED;
        clearCycleRuntime();
    }

    public void advanceToNextCycle() {
        this.cycleNumber++;
        this.currentStep = ScenarioStep.INITIALIZED;
        this.cycleMode = null;
        clearCycleRuntime();
    }

    private void clearCycleRuntime() {
        outbound1OrderKey = null;
        outbound2OrderKey = null;
        move1OrderKey = null;
        move2OrderKey = null;
        inbound1OrderKey = null;
        inbound2OrderKey = null;

        activeOrderMap.clear();
        cycleOrders.clear();
        cycleOrderMap.clear();

        createdOrderCount = 0;
        completedOutboundCount = 0;
        completedMoveCount = 0;
        completedInboundCount = 0;
        conveyorArrivedMoveCount = 0;

        conveyorArrivedWaiting = false;
        hasError = false;
        errorMessage = null;
    }

    public boolean isRunning() {
        return currentStep != ScenarioStep.PAUSED && currentStep != ScenarioStep.ERROR;
    }

    public void onError(String message) {
        this.hasError = true;
        this.errorMessage = message;
        this.currentStep = ScenarioStep.ERROR;
    }

    public void pause() {
        this.currentStep = ScenarioStep.PAUSED;
    }

    public void registerOutbound1Order(String orderKey, String fromLoc) {
        this.outbound1OrderKey = orderKey;
        registerTrackedOrder(
                orderKey,
                "outbound1",
                "OUTBOUND",
                fromLoc,
                KMat2026LocationMapping.OUTBOUND_PORT_1
        );
    }

    public void registerOutbound2Order(String orderKey, String fromLoc) {
        this.outbound2OrderKey = orderKey;
        registerTrackedOrder(
                orderKey,
                "outbound2",
                "OUTBOUND",
                fromLoc,
                KMat2026LocationMapping.OUTBOUND_PORT_2
        );
    }

    public void registerMove1Order(String orderKey, String fromLoc, String toLoc) {
        this.move1OrderKey = orderKey;
        registerTrackedOrder(orderKey, "move1", "MOVE", fromLoc, toLoc);
    }

    public void registerMove2Order(String orderKey, String fromLoc, String toLoc) {
        this.move2OrderKey = orderKey;
        registerTrackedOrder(orderKey, "move2", "MOVE", fromLoc, toLoc);
    }

    public void registerInbound1Order(String orderKey, String toLoc) {
        this.inbound1OrderKey = orderKey;
        registerTrackedOrder(
                orderKey,
                "inbound1",
                "INBOUND",
                KMat2026LocationMapping.INBOUND_PORT,
                toLoc
        );
    }

    public void registerInbound2Order(String orderKey, String toLoc) {
        this.inbound2OrderKey = orderKey;
        registerTrackedOrder(
                orderKey,
                "inbound2",
                "INBOUND",
                KMat2026LocationMapping.INBOUND_PORT,
                toLoc
        );
    }

    private void registerTrackedOrder(String orderKey,
                                      String role,
                                      String orderType,
                                      String fromLoc,
                                      String toLoc) {
        if (orderKey == null || cycleOrderMap.containsKey(orderKey)) {
            return;
        }

        createdOrderCount++;

        activeOrderMap.put(orderKey, fromLoc + "→" + toLoc);

        CycleOrderTrack track = new CycleOrderTrack(
                orderKey,
                role,
                orderType,
                fromLoc,
                toLoc,
                createdOrderCount
        );

        cycleOrders.add(track);
        cycleOrderMap.put(orderKey, track);

        logger.info("[Context] cycle order 등록 - seq={}, role={}, type={}, {}→{}, key={}",
                track.getCreatedSeq(), role, orderType, fromLoc, toLoc, orderKey);
    }

    public int markOrderCompleted(String orderKey) {
        if (orderKey == null) {
            return -1;
        }

        CycleOrderTrack track = cycleOrderMap.get(orderKey);
        if (track == null) {
            return -1;
        }
        if (track.isCompleted()) {
            return track.getCompletedSeqInType();
        }

        int seqInType;
        switch (track.getOrderType()) {
            case "OUTBOUND":
                completedOutboundCount++;
                seqInType = completedOutboundCount;
                break;
            case "MOVE":
                completedMoveCount++;
                seqInType = completedMoveCount;
                break;
            case "INBOUND":
                completedInboundCount++;
                seqInType = completedInboundCount;
                break;
            default:
                return -1;
        }

        track.setCompleted(true);
        track.setCompletedSeqInType(seqInType);
        activeOrderMap.remove(orderKey);

        logger.info("[Context] order 완료 - type={}, role={}, seqInType={}, key={}",
                track.getOrderType(), track.getRole(), seqInType, orderKey);

        return seqInType;
    }

    public int markMoveConveyorArrived(String orderKey) {
        if (orderKey == null) {
            return -1;
        }

        CycleOrderTrack track = cycleOrderMap.get(orderKey);
        if (track == null) {
            return -1;
        }
        if (!"MOVE".equals(track.getOrderType())) {
            return -1;
        }
        if (track.isConveyorArrived()) {
            return track.getConveyorArrivedSeqInType();
        }

        conveyorArrivedMoveCount++;
        track.setConveyorArrived(true);
        track.setConveyorArrivedSeqInType(conveyorArrivedMoveCount);

        logger.info("[Context] move conveyor 도착 - role={}, seqInType={}, key={}",
                track.getRole(), track.getConveyorArrivedSeqInType(), orderKey);

        return track.getConveyorArrivedSeqInType();
    }

    public String getOrderRole(String orderKey) {
        CycleOrderTrack track = cycleOrderMap.get(orderKey);
        return track != null ? track.getRole() : null;
    }

    public String getOrderType(String orderKey) {
        CycleOrderTrack track = cycleOrderMap.get(orderKey);
        return track != null ? track.getOrderType() : null;
    }

    public String getOrderFromLoc(String orderKey) {
        CycleOrderTrack track = cycleOrderMap.get(orderKey);
        return track != null ? track.getFromLoc() : null;
    }

    public String getOrderToLoc(String orderKey) {
        CycleOrderTrack track = cycleOrderMap.get(orderKey);
        return track != null ? track.getToLoc() : null;
    }

    public CycleOrderTrack getCycleOrder(String orderKey) {
        return cycleOrderMap.get(orderKey);
    }

    public boolean isTrackedOrder(String orderKey) {
        return orderKey != null && cycleOrderMap.containsKey(orderKey);
    }

    @Override
    public String toString() {
        return "ScenarioContext{" +
                "scenarioId='" + scenarioId + '\'' +
                ", currentStep=" + currentStep +
                ", cycleNumber=" + cycleNumber +
                ", cycleMode=" + cycleMode +
                ", lastFloor1OutboundLocSeq=" + lastFloor1OutboundLocSeq +
                ", lastFloor2MoveLocSeq=" + lastFloor2MoveLocSeq +
                ", lastFloor2InboundLocSeq=" + lastFloor2InboundLocSeq +
                ", createdOrderCount=" + createdOrderCount +
                ", completedOutboundCount=" + completedOutboundCount +
                ", completedMoveCount=" + completedMoveCount +
                ", completedInboundCount=" + completedInboundCount +
                ", conveyorArrivedMoveCount=" + conveyorArrivedMoveCount +
                ", conveyorArrivedWaiting=" + conveyorArrivedWaiting +
                ", hasError=" + hasError +
                '}';
    }
}