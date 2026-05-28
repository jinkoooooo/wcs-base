package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CycleMode;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CyclePoint;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext.CycleOrderTrack;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext.ScenarioStep;
import operato.logis.kmat_2026.entity.TbKmat2026ScenarioState;
import operato.logis.kmat_2026.service.impl.TbKmat2026ScenarioStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KMat2026ScenarioPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026ScenarioPersistenceService.class);

    @Autowired
    private TbKmat2026ScenarioStateService stateService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================================================================
    // 저장
    // ========================================================================

    public void save(KMat2026ScenarioContext context) {
        if (context == null) {
            logger.warn("[Persistence] context is null, skip save");
            return;
        }

        try {
            TbKmat2026ScenarioState state = toEntity(context);
            stateService.save(state);

            logger.debug("[Persistence] save 완료 - scenarioId={}, step={}, cycle={}, mode={}",
                    context.getScenarioId(),
                    context.getCurrentStep(),
                    context.getCycleNumber(),
                    context.getCycleMode());
        } catch (Exception e) {
            logger.error("[Persistence] save 실패", e);
        }
    }

    private TbKmat2026ScenarioState toEntity(KMat2026ScenarioContext ctx) {
        TbKmat2026ScenarioState state = new TbKmat2026ScenarioState();

        state.setScenarioId(ctx.getScenarioId());
        state.setStartedAt(toOffsetDateTime(ctx.getStartedAt()));
        state.setCurrentStep(ctx.getCurrentStep() != null ? ctx.getCurrentStep().name() : null);

        state.setCycleNumber(ctx.getCycleNumber());
        state.setCycleMode(ctx.getCycleMode() != null ? ctx.getCycleMode().name() : null);

        state.setLastFloor1OutboundLocSeq(ctx.getLastFloor1OutboundLocSeq());
        state.setLastFloor2MoveLocSeq(ctx.getLastFloor2MoveLocSeq());
        state.setLastFloor2InboundLocSeq(ctx.getLastFloor2InboundLocSeq());

        state.setOutbound1OrderKey(ctx.getOutbound1OrderKey());
        state.setOutbound2OrderKey(ctx.getOutbound2OrderKey());
        state.setMove1OrderKey(ctx.getMove1OrderKey());
        state.setMove2OrderKey(ctx.getMove2OrderKey());
        state.setInbound1OrderKey(ctx.getInbound1OrderKey());
        state.setInbound2OrderKey(ctx.getInbound2OrderKey());

        state.setActiveOrderMap(mapToJson(ctx.getActiveOrderMap()));
        state.setCycleOrdersJson(cycleOrdersToJson(ctx.getCycleOrders()));

        state.setCreatedOrderCount(ctx.getCreatedOrderCount());
        state.setCompletedOutboundCount(ctx.getCompletedOutboundCount());
        state.setCompletedMoveCount(ctx.getCompletedMoveCount());
        state.setCompletedInboundCount(ctx.getCompletedInboundCount());
        state.setConveyorArrivedMoveCount(ctx.getConveyorArrivedMoveCount());

        state.setConveyorArrivedWaiting(ctx.isConveyorArrivedWaiting() ? 1 : 0);

        state.setHasError(ctx.isHasError() ? 1 : 0);
        state.setErrorMessage(ctx.getErrorMessage());

        return state;
    }

    // ========================================================================
    // 로드
    // ========================================================================

    public KMat2026ScenarioContext load() {
        TbKmat2026ScenarioState state = stateService.findCurrent();
        if (state == null) {
            logger.info("[Persistence] 저장된 상태 없음");
            return null;
        }

        try {
            KMat2026ScenarioContext ctx = fromEntity(state);

            logger.info("[Persistence] load 완료 - scenarioId={}, step={}, cycle={}, mode={}",
                    ctx.getScenarioId(),
                    ctx.getCurrentStep(),
                    ctx.getCycleNumber(),
                    ctx.getCycleMode());

            return ctx;
        } catch (Exception e) {
            logger.error("[Persistence] load 실패", e);
            return null;
        }
    }

    private KMat2026ScenarioContext fromEntity(TbKmat2026ScenarioState state) {
        KMat2026ScenarioContext ctx = new KMat2026ScenarioContext();

        ctx.setScenarioId(state.getScenarioId());
        ctx.setStartedAt(toLocalDateTime(state.getStartedAt()));
        ctx.setCurrentStep(parseStep(state.getCurrentStep()));

        ctx.setCycleNumber(defaultInt(state.getCycleNumber(), 1));
        ctx.setCycleMode(parseCycleMode(state.getCycleMode()));

        ctx.setLastFloor1OutboundLocSeq(state.getLastFloor1OutboundLocSeq());
        ctx.setLastFloor2MoveLocSeq(state.getLastFloor2MoveLocSeq());
        ctx.setLastFloor2InboundLocSeq(state.getLastFloor2InboundLocSeq());

        ctx.setOutbound1OrderKey(state.getOutbound1OrderKey());
        ctx.setOutbound2OrderKey(state.getOutbound2OrderKey());
        ctx.setMove1OrderKey(state.getMove1OrderKey());
        ctx.setMove2OrderKey(state.getMove2OrderKey());
        ctx.setInbound1OrderKey(state.getInbound1OrderKey());
        ctx.setInbound2OrderKey(state.getInbound2OrderKey());

        restoreMap(ctx.getActiveOrderMap(), state.getActiveOrderMap());
        restoreCycleOrders(ctx, state.getCycleOrdersJson());

        ctx.setCreatedOrderCount(defaultInt(state.getCreatedOrderCount(), 0));
        ctx.setCompletedOutboundCount(defaultInt(state.getCompletedOutboundCount(), 0));
        ctx.setCompletedMoveCount(defaultInt(state.getCompletedMoveCount(), 0));
        ctx.setCompletedInboundCount(defaultInt(state.getCompletedInboundCount(), 0));
        ctx.setConveyorArrivedMoveCount(defaultInt(state.getConveyorArrivedMoveCount(), 0));

        ctx.setConveyorArrivedWaiting(defaultInt(state.getConveyorArrivedWaiting(), 0) == 1);

        ctx.setHasError(defaultInt(state.getHasError(), 0) == 1);
        ctx.setErrorMessage(state.getErrorMessage());

        return ctx;
    }

    // ========================================================================
    // 존재 여부 / 삭제
    // ========================================================================

    public boolean hasSavedState() {
        return stateService.exists();
    }

    public void delete() {
        stateService.deleteCurrent();
        logger.info("[Persistence] 저장 상태 삭제 완료");
    }

    // ========================================================================
    // CyclePoint JSON
    // ========================================================================

    private String cyclePointToJson(CyclePoint cp) {
        if (cp == null) {
            return "{}";
        }

        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("outbound1Loc", cp.outbound1Loc);
            map.put("outbound1LocSeq", cp.outbound1LocSeq);

            map.put("outbound2Loc", cp.outbound2Loc);
            map.put("outbound2LocSeq", cp.outbound2LocSeq);

            map.put("move1From", cp.move1From);
            map.put("move1FromSeq", cp.move1FromSeq);
            map.put("move1To", cp.move1To);
            map.put("move1ToSeq", cp.move1ToSeq);

            map.put("move2From", cp.move2From);
            map.put("move2FromSeq", cp.move2FromSeq);
            map.put("move2To", cp.move2To);
            map.put("move2ToSeq", cp.move2ToSeq);

            map.put("inbound1ToLoc", cp.inbound1ToLoc);
            map.put("inbound1ToLocSeq", cp.inbound1ToLocSeq);

            map.put("inbound2ToLoc", cp.inbound2ToLoc);
            map.put("inbound2ToLocSeq", cp.inbound2ToLocSeq);

            map.put("mode", cp.mode != null ? cp.mode.name() : null);

            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            logger.warn("[Persistence] cyclePoint 직렬화 실패", e);
            return "{}";
        }
    }

    private CyclePoint jsonToCyclePoint(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return null;
        }

        try {
            Map<String, Object> map = objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {}
            );

            return new CyclePoint(
                    (String) map.get("outbound1Loc"), intVal(map.get("outbound1LocSeq")),
                    (String) map.get("outbound2Loc"), intVal(map.get("outbound2LocSeq")),
                    (String) map.get("move1From"), intVal(map.get("move1FromSeq")),
                    (String) map.get("move1To"), intVal(map.get("move1ToSeq")),
                    (String) map.get("move2From"), intVal(map.get("move2FromSeq")),
                    (String) map.get("move2To"), intVal(map.get("move2ToSeq")),
                    (String) map.get("inbound1ToLoc"), intVal(map.get("inbound1ToLocSeq")),
                    (String) map.get("inbound2ToLoc"), intVal(map.get("inbound2ToLocSeq")),
                    parseCycleMode((String) map.get("mode"))
            );
        } catch (Exception e) {
            logger.warn("[Persistence] cyclePoint 역직렬화 실패 - json={}", json, e);
            return null;
        }
    }

    // ========================================================================
    // activeOrderMap JSON
    // ========================================================================

    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.warn("[Persistence] map 직렬화 실패", e);
            return "{}";
        }
    }

    private void restoreMap(Map<String, String> map, String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return;
        }

        try {
            Map<String, String> loaded = objectMapper.readValue(
                    json, new TypeReference<Map<String, String>>() {}
            );
            map.putAll(loaded);
        } catch (Exception e) {
            logger.warn("[Persistence] map 역직렬화 실패 - json={}", json, e);
        }
    }

    // ========================================================================
    // cycleOrders JSON
    // ========================================================================

    private String cycleOrdersToJson(List<CycleOrderTrack> cycleOrders) {
        if (cycleOrders == null || cycleOrders.isEmpty()) {
            return "[]";
        }

        try {
            List<Map<String, Object>> list = cycleOrders.stream().map(track -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("orderKey", track.getOrderKey());
                map.put("role", track.getRole());
                map.put("orderType", track.getOrderType());
                map.put("fromLoc", track.getFromLoc());
                map.put("toLoc", track.getToLoc());
                map.put("createdSeq", track.getCreatedSeq());
                map.put("completedSeqInType", track.getCompletedSeqInType());
                map.put("completed", track.isCompleted());
                map.put("conveyorArrived", track.isConveyorArrived());
                map.put("conveyorArrivedSeqInType", track.getConveyorArrivedSeqInType());
                return map;
            }).toList();

            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            logger.warn("[Persistence] cycleOrders 직렬화 실패", e);
            return "[]";
        }
    }

    private void restoreCycleOrders(KMat2026ScenarioContext ctx, String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return;
        }

        try {
            List<Map<String, Object>> list = objectMapper.readValue(
                    json, new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> map : list) {
                CycleOrderTrack track = new CycleOrderTrack(
                        (String) map.get("orderKey"),
                        (String) map.get("role"),
                        (String) map.get("orderType"),
                        (String) map.get("fromLoc"),
                        (String) map.get("toLoc"),
                        defaultInt(intVal(map.get("createdSeq")), 0)
                );

                track.setCompletedSeqInType(defaultInt(intVal(map.get("completedSeqInType")), 0));
                track.setCompleted(boolVal(map.get("completed")));
                track.setConveyorArrived(boolVal(map.get("conveyorArrived")));
                track.setConveyorArrivedSeqInType(defaultInt(intVal(map.get("conveyorArrivedSeqInType")), 0));

                ctx.getCycleOrders().add(track);
                ctx.getCycleOrderMap().put(track.getOrderKey(), track);
            }
        } catch (Exception e) {
            logger.warn("[Persistence] cycleOrders 역직렬화 실패 - json={}", json, e);
        }
    }

    // ========================================================================
    // enum / time util
    // ========================================================================

    private ScenarioStep parseStep(String stepName) {
        if (stepName == null || stepName.isEmpty()) {
            return ScenarioStep.INITIALIZED;
        }

        try {
            return ScenarioStep.valueOf(stepName);
        } catch (Exception e) {
            logger.warn("[Persistence] 알 수 없는 step={}, INITIALIZED 처리", stepName);
            return ScenarioStep.INITIALIZED;
        }
    }

    private CycleMode parseCycleMode(String modeName) {
        if (modeName == null || modeName.isEmpty()) {
            return null;
        }

        try {
            return CycleMode.valueOf(modeName);
        } catch (Exception e) {
            logger.warn("[Persistence] 알 수 없는 cycleMode={}, null 처리", modeName);
            return null;
        }
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.toLocalDateTime();
    }

    // ========================================================================
    // primitive util
    // ========================================================================

    private Integer intVal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private boolean boolVal(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean b) return b;
        if (obj instanceof Number n) return n.intValue() == 1;
        if (obj instanceof String s) return "true".equalsIgnoreCase(s) || "1".equals(s);
        return false;
    }
}