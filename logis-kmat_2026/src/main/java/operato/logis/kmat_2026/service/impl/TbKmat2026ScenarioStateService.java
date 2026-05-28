package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.entity.TbKmat2026ScenarioState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * ============================================================================
 * TbKmat2026ScenarioState Entity Service
 * ============================================================================
 *
 * [역할]
 * - tb_kmat2026_scenario_state 단일 레코드 조회/저장/삭제
 * - KMAT_2026 고정 scenario_key 1건만 관리
 * - API / 이벤트 핸들러가 현재 상태를 DB 조회로 판단할 수 있도록 헬퍼 제공
 */
@Service
public class TbKmat2026ScenarioStateService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbKmat2026ScenarioStateService.class);

    /**
     * 단일 시나리오 레코드 키
     */
    public static final String SCENARIO_KEY = "KMAT_2026";

    // ========================================================================
    // 조회
    // ========================================================================

    /**
     * PK(id) 조회
     */
    public TbKmat2026ScenarioState findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return this.queryManager.select(TbKmat2026ScenarioState.class, id);
    }

    /**
     * scenario_key 기준 단일 레코드 조회
     */
    public TbKmat2026ScenarioState findByScenarioKey(String scenarioKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("scenario_key", scenarioKey);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbKmat2026ScenarioState.class, condition);
    }

    /**
     * 현재 시나리오 상태 조회
     */
    public TbKmat2026ScenarioState findCurrent() {
        return findByScenarioKey(SCENARIO_KEY);
    }

    /**
     * 현재 시나리오 상태 존재 여부
     */
    public boolean exists() {
        return findCurrent() != null;
    }

    /**
     * 현재 상태를 매번 DB에서 다시 읽어서 반환
     * - 핸들러/API에서 "최신 상태" 기준으로 판단하고 싶을 때 사용
     */
    public TbKmat2026ScenarioState refreshCurrent() {
        return findCurrent();
    }

    // ========================================================================
    // 현재 상태 DB 기준 헬퍼 조회
    // ========================================================================

    public String getCurrentScenarioId() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getScenarioId() : null;
    }

    public String getCurrentStep() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getCurrentStep() : null;
    }

    public Integer getCurrentCycleNumber() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getCycleNumber() : null;
    }

    public String getCurrentCycleMode() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getCycleMode() : null;
    }

    public Integer getLastFloor1OutboundLocSeq() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getLastFloor1OutboundLocSeq() : null;
    }

    public Integer getLastFloor2MoveLocSeq() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getLastFloor2MoveLocSeq() : null;
    }

    public Integer getLastFloor2InboundLocSeq() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getLastFloor2InboundLocSeq() : null;
    }

    public String getOutbound1OrderKey() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getOutbound1OrderKey() : null;
    }

    public String getOutbound2OrderKey() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getOutbound2OrderKey() : null;
    }

    public String getMove1OrderKey() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getMove1OrderKey() : null;
    }

    public String getMove2OrderKey() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getMove2OrderKey() : null;
    }

    public String getInbound1OrderKey() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getInbound1OrderKey() : null;
    }

    public String getInbound2OrderKey() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getInbound2OrderKey() : null;
    }

    public boolean isInbound1Created() {
        return isNotBlank(getInbound1OrderKey());
    }

    public boolean isInbound2Created() {
        return isNotBlank(getInbound2OrderKey());
    }

    public int getCreatedOrderCount() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null && state.getCreatedOrderCount() != null ? state.getCreatedOrderCount() : 0;
    }

    public int getCompletedOutboundCount() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null && state.getCompletedOutboundCount() != null ? state.getCompletedOutboundCount() : 0;
    }

    public int getCompletedMoveCount() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null && state.getCompletedMoveCount() != null ? state.getCompletedMoveCount() : 0;
    }

    public int getCompletedInboundCount() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null && state.getCompletedInboundCount() != null ? state.getCompletedInboundCount() : 0;
    }

    public int getConveyorArrivedMoveCount() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null && state.getConveyorArrivedMoveCount() != null ? state.getConveyorArrivedMoveCount() : 0;
    }

    public boolean isConveyorArrivedWaiting() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null && state.getConveyorArrivedWaiting() != null && state.getConveyorArrivedWaiting() == 1;
    }

    public boolean hasError() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null && state.getHasError() != null && state.getHasError() == 1;
    }

    public String getErrorMessage() {
        TbKmat2026ScenarioState state = refreshCurrent();
        return state != null ? state.getErrorMessage() : null;
    }

    public boolean isRunning() {
        TbKmat2026ScenarioState state = refreshCurrent();
        if (state == null) {
            return false;
        }

        String step = state.getCurrentStep();
        if (step == null || step.isBlank()) {
            return false;
        }

        return !"PAUSED".equals(step) && !"ERROR".equals(step);
    }

    public boolean isPaused() {
        return Objects.equals(getCurrentStep(), "PAUSED");
    }

    public boolean isErrorState() {
        return Objects.equals(getCurrentStep(), "ERROR");
    }

    public boolean isCurrentCycleModeCanMove() {
        return Objects.equals(getCurrentCycleMode(), "FLOOR_2_SHUTTLE_CAN_MOVE");
    }

    public boolean isCurrentCycleModeCanNotMove() {
        return Objects.equals(getCurrentCycleMode(), "FLOOR_2_SHUTTLE_CAN_NOT_MOVE");
    }

    /**
     * 현재 cycle에서 inbound가 몇 건 생성되었는지 DB 기준으로 판단
     */
    public int getInboundCreatedCount() {
        int count = 0;
        if (isInbound1Created()) count++;
        if (isInbound2Created()) count++;
        return count;
    }

    /**
     * 현재 cycle에서 outbound가 몇 건 생성되었는지 DB 기준으로 판단
     */
    public int getOutboundCreatedCount() {
        int count = 0;
        if (isNotBlank(getOutbound1OrderKey())) count++;
        if (isNotBlank(getOutbound2OrderKey())) count++;
        return count;
    }

    /**
     * 현재 cycle에서 move가 몇 건 생성되었는지 DB 기준으로 판단
     */
    public int getMoveCreatedCount() {
        int count = 0;
        if (isNotBlank(getMove1OrderKey())) count++;
        if (isNotBlank(getMove2OrderKey())) count++;
        return count;
    }

    /**
     * inbound1을 이미 만들었고, inbound2는 아직 안 만들었는지
     */
    public boolean shouldCreateInbound2() {
        return isInbound1Created() && !isInbound2Created();
    }

    /**
     * inbound를 아직 하나도 안 만들었는지
     */
    public boolean shouldCreateInbound1() {
        return !isInbound1Created();
    }

    /**
     * second move conveyor 도착 완료를 DB 상태로 판단
     * - 현재 구조에서는 conveyorArrivedMoveCount >= 2 이면 true
     */
    public boolean isSecondMoveConveyorArrived() {
        return getConveyorArrivedMoveCount() >= 2;
    }

    /**
     * 현재 상태 요약 로그용
     */
    public String getCurrentStateSummary() {
        TbKmat2026ScenarioState state = refreshCurrent();
        if (state == null) {
            return "ScenarioState{not_found}";
        }

        return "ScenarioState{" +
                "scenarioId='" + state.getScenarioId() + '\'' +
                ", step='" + state.getCurrentStep() + '\'' +
                ", cycleNumber=" + state.getCycleNumber() +
                ", cycleMode='" + state.getCycleMode() + '\'' +
                ", outbound1OrderKey='" + state.getOutbound1OrderKey() + '\'' +
                ", outbound2OrderKey='" + state.getOutbound2OrderKey() + '\'' +
                ", move1OrderKey='" + state.getMove1OrderKey() + '\'' +
                ", move2OrderKey='" + state.getMove2OrderKey() + '\'' +
                ", inbound1OrderKey='" + state.getInbound1OrderKey() + '\'' +
                ", inbound2OrderKey='" + state.getInbound2OrderKey() + '\'' +
                ", completedOutboundCount=" + state.getCompletedOutboundCount() +
                ", completedMoveCount=" + state.getCompletedMoveCount() +
                ", completedInboundCount=" + state.getCompletedInboundCount() +
                ", conveyorArrivedMoveCount=" + state.getConveyorArrivedMoveCount() +
                ", conveyorArrivedWaiting=" + state.getConveyorArrivedWaiting() +
                ", hasError=" + state.getHasError() +
                ", errorMessage='" + state.getErrorMessage() + '\'' +
                '}';
    }

    // ========================================================================
    // 저장
    // ========================================================================

    /**
     * 신규 insert
     */
    public TbKmat2026ScenarioState insert(TbKmat2026ScenarioState entity) {
        if (entity == null) {
            throw new IllegalArgumentException("TbKmat2026ScenarioState entity is required");
        }

        entity.setScenarioKey(SCENARIO_KEY);
        entity.setLastUpdatedAt(OffsetDateTime.now());

        logger.info("[TbKmat2026ScenarioStateService] insert - scenarioId={}, step={}, cycle={}, mode={}",
                entity.getScenarioId(),
                entity.getCurrentStep(),
                entity.getCycleNumber(),
                entity.getCycleMode());

        this.queryManager.insert(entity);
        return entity;
    }

    /**
     * 기존 row update
     */
    public TbKmat2026ScenarioState update(TbKmat2026ScenarioState entity) {
        if (entity == null) {
            throw new IllegalArgumentException("TbKmat2026ScenarioState entity is required");
        }

        entity.setScenarioKey(SCENARIO_KEY);
        entity.setLastUpdatedAt(OffsetDateTime.now());

        logger.info("[TbKmat2026ScenarioStateService] update - id={}, scenarioId={}, step={}, cycle={}, mode={}",
                entity.getId(),
                entity.getScenarioId(),
                entity.getCurrentStep(),
                entity.getCycleNumber(),
                entity.getCycleMode());

        this.queryManager.update(entity);
        return entity;
    }

    /**
     * 단일 row upsert
     *
     * 정책:
     * - scenario_key=KMAT_2026 row 1건만 유지
     * - 기존 row가 있으면 PK(id) 유지해서 update
     * - 없으면 insert
     */
    public TbKmat2026ScenarioState save(TbKmat2026ScenarioState entity) {
        if (entity == null) {
            throw new IllegalArgumentException("TbKmat2026ScenarioState entity is required");
        }

        TbKmat2026ScenarioState existing = findCurrent();

        if (existing == null) {
            return insert(entity);
        }

        entity.setId(existing.getId());
        entity.setScenarioKey(SCENARIO_KEY);

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(existing.getCreatedAt());
        }
        if (entity.getCreatorId() == null) {
            entity.setCreatorId(existing.getCreatorId());
        }
        if (entity.getUpdaterId() == null) {
            entity.setUpdaterId(existing.getUpdaterId());
        }

        return update(entity);
    }

    // ========================================================================
    // 삭제
    // ========================================================================

    /**
     * PK 기준 삭제
     */
    public void delete(String id) {
        if (id == null || id.isBlank()) {
            return;
        }

        TbKmat2026ScenarioState entity = findById(id);
        if (entity == null) {
            logger.info("[TbKmat2026ScenarioStateService] delete skip - id={} not found", id);
            return;
        }

        logger.info("[TbKmat2026ScenarioStateService] delete - id={}, scenarioId={}",
                id, entity.getScenarioId());

        this.queryManager.delete(entity);
    }

    /**
     * 현재 단일 row 삭제
     */
    public void deleteCurrent() {
        TbKmat2026ScenarioState entity = findCurrent();
        if (entity == null) {
            logger.info("[TbKmat2026ScenarioStateService] deleteCurrent skip - no current state");
            return;
        }

        logger.info("[TbKmat2026ScenarioStateService] deleteCurrent - id={}, scenarioId={}",
                entity.getId(), entity.getScenarioId());

        this.queryManager.delete(entity);
    }

    // ========================================================================
    // util
    // ========================================================================

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}