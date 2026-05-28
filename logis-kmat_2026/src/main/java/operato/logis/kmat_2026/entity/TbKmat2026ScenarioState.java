package operato.logis.kmat_2026.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import xyz.elidom.dbist.annotation.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Table(name = "tb_kmat2026_scenario_state", idStrategy = GenerationRule.UUID)
public class TbKmat2026ScenarioState extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id")
    private String id;

    @Column(name = "scenario_id")
    private String scenarioId;

    @Column(name = "scenario_key")
    private String scenarioKey;

    @Column(name = "last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "cycle_number")
    private Integer cycleNumber;

    @Column(name = "cycle_mode")
    private String cycleMode;

    @Column(name = "last_floor1_outbound_loc_seq")
    private Integer lastFloor1OutboundLocSeq;

    @Column(name = "last_floor2_move_loc_seq")
    private Integer lastFloor2MoveLocSeq;

    @Column(name = "last_floor2_inbound_loc_seq")
    private Integer lastFloor2InboundLocSeq;

    @Column(name = "outbound1_order_key")
    private String outbound1OrderKey;

    @Column(name = "outbound2_order_key")
    private String outbound2OrderKey;

    @Column(name = "move1_order_key")
    private String move1OrderKey;

    @Column(name = "move2_order_key")
    private String move2OrderKey;

    @Column(name = "inbound1_order_key")
    private String inbound1OrderKey;

    @Column(name = "inbound2_order_key")
    private String inbound2OrderKey;

    /**
     * 현재 싸이클 포인트 전체 JSON
     */
    @Column(name = "current_cycle_point")
    private String currentCyclePoint;

    /**
     * activeOrderMap JSON
     */
    @Column(name = "active_order_map")
    private String activeOrderMap;

    /**
     * cycleOrders JSON
     */
    @Column(name = "cycle_orders_json")
    private String cycleOrdersJson;

    @Column(name = "created_order_count")
    private Integer createdOrderCount;

    @Column(name = "completed_outbound_count")
    private Integer completedOutboundCount;

    @Column(name = "completed_move_count")
    private Integer completedMoveCount;

    @Column(name = "completed_inbound_count")
    private Integer completedInboundCount;

    @Column(name = "conveyor_arrived_move_count")
    private Integer conveyorArrivedMoveCount;

    /**
     * boolean 대신 숫자(0/1)
     */
    @Column(name = "conveyor_arrived_waiting")
    private Integer conveyorArrivedWaiting;

    @Column(name = "agf_inbound_refill_waiting")
    private Integer agfInboundRefillWaiting;

    @Column(name = "has_error")
    private Integer hasError;

    @Column(name = "error_message")
    private String errorMessage;

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getCycleNumber() {
        return cycleNumber;
    }

    public void setCycleNumber(Integer cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public String getCycleMode() {
        return cycleMode;
    }

    public void setCycleMode(String cycleMode) {
        this.cycleMode = cycleMode;
    }

    public Integer getLastFloor1OutboundLocSeq() {
        return lastFloor1OutboundLocSeq;
    }

    public void setLastFloor1OutboundLocSeq(Integer lastFloor1OutboundLocSeq) {
        this.lastFloor1OutboundLocSeq = lastFloor1OutboundLocSeq;
    }

    public Integer getLastFloor2MoveLocSeq() {
        return lastFloor2MoveLocSeq;
    }

    public void setLastFloor2MoveLocSeq(Integer lastFloor2MoveLocSeq) {
        this.lastFloor2MoveLocSeq = lastFloor2MoveLocSeq;
    }

    public String getOutbound1OrderKey() {
        return outbound1OrderKey;
    }

    public void setOutbound1OrderKey(String outbound1OrderKey) {
        this.outbound1OrderKey = outbound1OrderKey;
    }

    public String getOutbound2OrderKey() {
        return outbound2OrderKey;
    }

    public void setOutbound2OrderKey(String outbound2OrderKey) {
        this.outbound2OrderKey = outbound2OrderKey;
    }

    public String getMove1OrderKey() {
        return move1OrderKey;
    }

    public void setMove1OrderKey(String move1OrderKey) {
        this.move1OrderKey = move1OrderKey;
    }

    public String getMove2OrderKey() {
        return move2OrderKey;
    }

    public void setMove2OrderKey(String move2OrderKey) {
        this.move2OrderKey = move2OrderKey;
    }

    public String getInbound1OrderKey() {
        return inbound1OrderKey;
    }

    public void setInbound1OrderKey(String inbound1OrderKey) {
        this.inbound1OrderKey = inbound1OrderKey;
    }

    public String getInbound2OrderKey() {
        return inbound2OrderKey;
    }

    public void setInbound2OrderKey(String inbound2OrderKey) {
        this.inbound2OrderKey = inbound2OrderKey;
    }

    public String getCurrentCyclePoint() {
        return currentCyclePoint;
    }

    public void setCurrentCyclePoint(String currentCyclePoint) {
        this.currentCyclePoint = currentCyclePoint;
    }

    public String getActiveOrderMap() {
        return activeOrderMap;
    }

    public void setActiveOrderMap(String activeOrderMap) {
        this.activeOrderMap = activeOrderMap;
    }

    public String getCycleOrdersJson() {
        return cycleOrdersJson;
    }

    public void setCycleOrdersJson(String cycleOrdersJson) {
        this.cycleOrdersJson = cycleOrdersJson;
    }

    public Integer getCreatedOrderCount() {
        return createdOrderCount;
    }

    public void setCreatedOrderCount(Integer createdOrderCount) {
        this.createdOrderCount = createdOrderCount;
    }

    public Integer getCompletedOutboundCount() {
        return completedOutboundCount;
    }

    public void setCompletedOutboundCount(Integer completedOutboundCount) {
        this.completedOutboundCount = completedOutboundCount;
    }

    public Integer getCompletedMoveCount() {
        return completedMoveCount;
    }

    public void setCompletedMoveCount(Integer completedMoveCount) {
        this.completedMoveCount = completedMoveCount;
    }

    public Integer getCompletedInboundCount() {
        return completedInboundCount;
    }

    public void setCompletedInboundCount(Integer completedInboundCount) {
        this.completedInboundCount = completedInboundCount;
    }

    public Integer getConveyorArrivedMoveCount() {
        return conveyorArrivedMoveCount;
    }

    public void setConveyorArrivedMoveCount(Integer conveyorArrivedMoveCount) {
        this.conveyorArrivedMoveCount = conveyorArrivedMoveCount;
    }

    public Integer getConveyorArrivedWaiting() {
        return conveyorArrivedWaiting;
    }

    public void setConveyorArrivedWaiting(Integer conveyorArrivedWaiting) {
        this.conveyorArrivedWaiting = conveyorArrivedWaiting;
    }

    public Integer getAgfInboundRefillWaiting() {
        return agfInboundRefillWaiting;
    }

    public void setAgfInboundRefillWaiting(Integer agfInboundRefillWaiting) {
        this.agfInboundRefillWaiting = agfInboundRefillWaiting;
    }

    public Integer getHasError() {
        return hasError;
    }

    public void setHasError(Integer hasError) {
        this.hasError = hasError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}