package operato.logis.asrs.core.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.request.StrategyRunRequest;
import operato.logis.asrs.dto.response.RelocationTaskPreviewRow;
import operato.logis.asrs.dto.response.StrategyRunResult;
import operato.logis.asrs.entity.TbAcRelocationTask;
import operato.logis.asrs.entity.TbAcStrategyRule;
import operato.logis.asrs.entity.TbAcStrategyRun;
import operato.logis.asrs.entity.TbAcStrategySet;
import operato.logis.asrs.enums.GradeRankType;
import operato.logis.asrs.enums.StrategyTaskStatus;
import operato.logis.asrs.enums.StrategyTaskType;
import operato.logis.asrs.query.strategy.StrategyQueryService;
import operato.logis.asrs.query.strategy.model.RelocationStockCandidateRow;
import operato.logis.asrs.query.strategy.model.RelocationSwapCandidateRow;
import operato.logis.asrs.query.strategy.model.RelocationTargetLocationRow;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 재배치 전략 코어.
 *
 * <p>
 * 3차 고도화 버전:
 * - rule_type 분기 처리
 * - condition_json / action_json 실제 해석
 * - Demand 반영
 * - Swap 옵션 반영
 * - simulation_yn 반영
 * - run 상태코드 흐름 반영
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RelocationStrategyCore extends AbstractQueryService {

    private static final String RUN_TYPE_MANUAL = "MANUAL";
    private static final String RUN_STATUS_RUNNING = "RUNNING";
    private static final String RUN_STATUS_DONE = "DONE";
    private static final String RUN_STATUS_FAILED = "FAILED";

    private final StrategyQueryService strategyQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 상품등급 ↔ 로케이션등급 매칭 기반 재배치 전략 실행
     */
    @Transactional
    public StrategyRunResult runGradeMatchingStrategy(StrategyRunRequest request) {
        validateRequest(request);

        TbAcStrategySet strategySet =
                strategyQueryService.findStrategySet(request.getAreaCode(), request.getStrategyCode());

        List<TbAcStrategyRule> rules =
                strategyQueryService.findEnabledRules(strategySet.getId());

        if (rules == null || rules.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Enabled strategy rule not found. strategySetId=" + strategySet.getId()
            );
        }

        boolean previewOnly = request.getPreviewOnly() != null && request.getPreviewOnly().booleanValue();

        // simulation 전략은 강제로 previewOnly
        if ("Y".equalsIgnoreCase(strategySet.getSimulationYn())) {
            previewOnly = true;
        }

        int maxTaskCount = request.getMaxTaskCount() == null || request.getMaxTaskCount().intValue() <= 0
                ? 50 : request.getMaxTaskCount().intValue();

        List<RelocationStockCandidateRow> stocks =
                strategyQueryService.findRelocationSourceCandidates(request.getAreaCode());

        List<RelocationTaskPreviewRow> tasks = new ArrayList<RelocationTaskPreviewRow>();
        Set<String> assignedStockIds = new HashSet<String>();
        Set<String> assignedTargetLocationIds = new HashSet<String>();

        TbAcStrategyRun run = null;
        String runId = null;
        Date startedAt = new Date();

        try {
            if (!previewOnly) {
                run = new TbAcStrategyRun();
                run.setStrategySetId(strategySet.getId());
                run.setRunType(RUN_TYPE_MANUAL);
                run.setRunStatusCode(RUN_STATUS_RUNNING);
                run.setRunStartedAt(startedAt);
                run.setRunEndedAt(null);
                run.setTriggeredBy("admin");
                run.setResultSummaryJson(buildInitialRunSummaryJson(request, strategySet, rules, stocks.size()));

                this.queryManager.insert(run);
                runId = run.getId();
            }

            int taskPriority = 1;

            for (TbAcStrategyRule rule : rules) {
                if (tasks.size() >= maxTaskCount) {
                    break;
                }

                Map<String, Object> conditionMap = parseJson(rule.getConditionJson());
                Map<String, Object> actionMap = parseJson(rule.getActionJson());

                Integer maxTaskCountPerRule = getInteger(actionMap, "maxTaskCountPerRule");
                int createdCountByRule = 0;

                for (RelocationStockCandidateRow stock : stocks) {
                    if (tasks.size() >= maxTaskCount) {
                        break;
                    }

                    if (maxTaskCountPerRule != null && createdCountByRule >= maxTaskCountPerRule.intValue()) {
                        break;
                    }

                    if (ValueUtil.isEmpty(stock.getStockUnitId())) {
                        continue;
                    }
                    if (assignedStockIds.contains(stock.getStockUnitId())) {
                        continue;
                    }
                    if (ValueUtil.isEmpty(stock.getItemGrade()) || ValueUtil.isEmpty(stock.getCurrentLocationGrade())) {
                        continue;
                    }

                    // 1) condition_json 기반 룰 매칭
                    if (!matchesRule(rule, stock, conditionMap)) {
                        continue;
                    }

                    // 2) target location 선택 (빈 위치 + swap 반영)
                    RelocationTargetLocationRow bestTarget =
                            findBestTargetWithSwap(
                                    request.getAreaCode(),
                                    stock,
                                    rule,
                                    actionMap,
                                    assignedTargetLocationIds
                            );

                    if (bestTarget == null) {
                        continue;
                    }

                    // 3) taskType / reasonCode 결정
                    String taskType = resolveTaskType(rule, actionMap);
                    String reasonCode = resolveReasonCode(rule, actionMap);

                    RelocationTaskPreviewRow task = new RelocationTaskPreviewRow();
                    task.setStockUnitId(stock.getStockUnitId());
                    task.setStockUnitNo(stock.getStockUnitNo());
                    task.setItemId(stock.getItemId());
                    task.setItemCode(stock.getItemCode());
                    task.setItemName(stock.getItemName());
                    task.setFromLocationId(stock.getCurrentLocationId());
                    task.setFromLocationCode(stock.getCurrentLocationCode());
                    task.setFromLocationGrade(stock.getCurrentLocationGrade());
                    task.setToLocationId(bestTarget.getLocationId());
                    task.setToLocationCode(bestTarget.getLocationCode());
                    task.setToLocationGrade(bestTarget.getLocationGrade());
                    task.setItemGrade(stock.getItemGrade());
                    task.setTaskType(taskType);
                    task.setPriorityNo(Integer.valueOf(taskPriority++));
                    task.setReason(buildReasonText(rule, stock, bestTarget, reasonCode));

                    tasks.add(task);
                    assignedStockIds.add(stock.getStockUnitId());
                    assignedTargetLocationIds.add(bestTarget.getLocationId());
                    createdCountByRule++;
                }
            }

            if (!previewOnly && run != null) {
                for (RelocationTaskPreviewRow previewTask : tasks) {
                    TbAcRelocationTask task = new TbAcRelocationTask();
                    task.setStrategyRunId(run.getId());
                    task.setTaskType(previewTask.getTaskType());
                    task.setPriority(previewTask.getPriorityNo());
                    task.setItemId(previewTask.getItemId());
                    task.setStockUnitId(previewTask.getStockUnitId());
                    task.setSourceLocationId(previewTask.getFromLocationId());
                    task.setTargetLocationId(previewTask.getToLocationId());
                    task.setReasonCode(resolveReasonCodeFromPreview(previewTask));
                    task.setReasonDetailJson(
                            "{\"reason\":\"" + escapeJson(previewTask.getReason()) + "\"}"
                    );
                    task.setTaskStatusCode(StrategyTaskStatus.CREATED.getCode());
                    task.setConfirmedAt(null);

                    this.queryManager.insert(task);
                }

                run.setRunStatusCode(RUN_STATUS_DONE);
                run.setRunEndedAt(new Date());
                run.setResultSummaryJson(buildFinalRunSummaryJson(request, strategySet, rules, stocks.size(), tasks));
                this.queryManager.update(
                        run,
                        "runStatusCode",
                        "runEndedAt",
                        "resultSummaryJson"
                );
                runId = run.getId();
            }

            StrategyRunResult result = new StrategyRunResult();
            result.setAreaCode(request.getAreaCode());
            result.setStrategyCode(request.getStrategyCode());
            result.setPreviewOnly(Boolean.valueOf(previewOnly));
            result.setCandidateCount(Integer.valueOf(stocks.size()));
            result.setTaskCount(Integer.valueOf(tasks.size()));
            result.setStrategyRunId(runId);
            result.setTasks(tasks);

            return result;

        } catch (Exception e) {
            if (!previewOnly && run != null) {
                run.setRunStatusCode(RUN_STATUS_FAILED);
                run.setRunEndedAt(new Date());
                run.setResultSummaryJson(buildFailureRunSummaryJson(e));
                this.queryManager.update(
                        run,
                        "runStatusCode",
                        "runEndedAt",
                        "resultSummaryJson"
                );
            }

            if (e instanceof AisleCoreException) {
                throw (AisleCoreException) e;
            }

            throw new AisleCoreException(
                    AisleCoreErrorCode.INTERNAL_SERVER_ERROR,
                    "Relocation strategy execution failed. " + e.getMessage()
            );
        }
    }

    /**
     * 룰 조건 매칭
     */
    private boolean matchesRule(TbAcStrategyRule rule,
                                RelocationStockCandidateRow stock,
                                Map<String, Object> conditionMap) {

        if (!matchesCommonConditions(stock, conditionMap)) {
            return false;
        }

        String ruleType = nullToEmpty(rule.getRuleType()).toUpperCase();

        if ("GRADE_FORWARD".equals(ruleType)) {
            return matchesGradeForward(stock, conditionMap);
        }

        if ("GRADE_BACKWARD".equals(ruleType)) {
            return matchesGradeBackward(stock, conditionMap);
        }

        if ("DEMAND_PREPOS".equals(ruleType)) {
            return matchesDemandPrepos(stock, conditionMap);
        }

        return false;
    }

    /**
     * 공통 조건
     */
    private boolean matchesCommonConditions(RelocationStockCandidateRow stock, Map<String, Object> conditionMap) {
        List<String> itemGradeIn = getStringList(conditionMap, "itemGradeIn");
        if (!itemGradeIn.isEmpty() && !itemGradeIn.contains(stock.getItemGrade())) {
            return false;
        }

        List<String> itemGradeNotIn = getStringList(conditionMap, "itemGradeNotIn");
        if (!itemGradeNotIn.isEmpty() && itemGradeNotIn.contains(stock.getItemGrade())) {
            return false;
        }

        List<String> fromLocationGradeIn = getStringList(conditionMap, "fromLocationGradeIn");
        if (!fromLocationGradeIn.isEmpty() && !fromLocationGradeIn.contains(stock.getCurrentLocationGrade())) {
            return false;
        }

        List<String> fromLocationGradeNotIn = getStringList(conditionMap, "fromLocationGradeNotIn");
        if (!fromLocationGradeNotIn.isEmpty() && fromLocationGradeNotIn.contains(stock.getCurrentLocationGrade())) {
            return false;
        }

        Boolean frontPriorityRequired = getBoolean(conditionMap, "frontPriorityRequired");
        if (Boolean.TRUE.equals(frontPriorityRequired) && !"Y".equals(stock.getCurrentFrontPriorityYn())) {
            return false;
        }

        List<String> itemCodeIn = getStringList(conditionMap, "itemCodeIn");
        if (!itemCodeIn.isEmpty() && !itemCodeIn.contains(stock.getItemCode())) {
            return false;
        }

        List<String> itemCategoryIdIn = getStringList(conditionMap, "itemCategoryIdIn");
        if (!itemCategoryIdIn.isEmpty() && !itemCategoryIdIn.contains(stock.getItemCategoryId())) {
            return false;
        }

        Integer minQty = getInteger(conditionMap, "minQty");
        if (minQty != null) {
            if (stock.getQty() == null || stock.getQty().intValue() < minQty.intValue()) {
                return false;
            }
        }

        Boolean excludeReserved = getBoolean(conditionMap, "excludeReserved");
        if (Boolean.TRUE.equals(excludeReserved)) {
            if (stock.getReservedQty() != null && stock.getReservedQty().intValue() > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * GRADE_FORWARD 조건
     */
    private boolean matchesGradeForward(RelocationStockCandidateRow stock, Map<String, Object> conditionMap) {
        int itemRank = GradeRankType.rankOf(stock.getItemGrade());
        int locationRank = GradeRankType.rankOf(stock.getCurrentLocationGrade());

        Boolean itemBetterThanLocation = getBoolean(conditionMap, "itemGradeBetterThanLocationGrade");
        if (Boolean.TRUE.equals(itemBetterThanLocation) && !(itemRank < locationRank)) {
            return false;
        }

        return itemRank < locationRank;
    }

    /**
     * GRADE_BACKWARD 조건
     */
    private boolean matchesGradeBackward(RelocationStockCandidateRow stock, Map<String, Object> conditionMap) {
        int itemRank = GradeRankType.rankOf(stock.getItemGrade());
        int locationRank = GradeRankType.rankOf(stock.getCurrentLocationGrade());

        Boolean itemWorseThanLocation = getBoolean(conditionMap, "itemGradeWorseThanLocationGrade");
        if (Boolean.TRUE.equals(itemWorseThanLocation) && !(itemRank > locationRank)) {
            return false;
        }

        return itemRank > locationRank;
    }

    /**
     * DEMAND_PREPOS 조건
     */
    private boolean matchesDemandPrepos(RelocationStockCandidateRow stock, Map<String, Object> conditionMap) {
        Integer minDemandQty = getInteger(conditionMap, "minDemandQty");
        if (minDemandQty != null) {
            if (stock.getDemandTomorrowQty() == null || stock.getDemandTomorrowQty().intValue() < minDemandQty.intValue()) {
                return false;
            }
        }

        Integer maxDemandQty = getInteger(conditionMap, "maxDemandQty");
        if (maxDemandQty != null) {
            if (stock.getDemandTomorrowQty() != null && stock.getDemandTomorrowQty().intValue() > maxDemandQty.intValue()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 목적지 등급 결정
     */
    private String resolveTargetLocationGrade(TbAcStrategyRule rule,
                                              RelocationStockCandidateRow stock,
                                              Map<String, Object> actionMap) {

        Boolean sameAsItemGrade = getBoolean(actionMap, "sameAsItemGrade");
        if (Boolean.TRUE.equals(sameAsItemGrade)) {
            return stock.getItemGrade();
        }

        String directTarget = getString(actionMap, "targetLocationGrade");
        if (ValueUtil.isNotEmpty(directTarget) && !"SAME_AS_ITEM_GRADE".equalsIgnoreCase(directTarget)) {
            return directTarget;
        }

        Map<String, String> gradeMap = getStringMap(actionMap, "targetLocationGradeMap");
        if (!gradeMap.isEmpty()) {
            String mapped = gradeMap.get(stock.getItemGrade());
            if (ValueUtil.isNotEmpty(mapped)) {
                return mapped;
            }
        }

        String ruleType = nullToEmpty(rule.getRuleType()).toUpperCase();

        if ("GRADE_FORWARD".equals(ruleType)) {
            return stock.getItemGrade();
        }
        if ("GRADE_BACKWARD".equals(ruleType)) {
            return stock.getItemGrade();
        }
        if ("DEMAND_PREPOS".equals(ruleType)) {
            if ("A".equals(stock.getItemGrade()) || "B".equals(stock.getItemGrade())) {
                return "A";
            }
            return "B";
        }

        return null;
    }

    /**
     * primary + fallback target grade 후보 목록
     */
    private List<String> resolveTargetLocationGradeCandidates(TbAcStrategyRule rule,
                                                              RelocationStockCandidateRow stock,
                                                              Map<String, Object> actionMap) {
        List<String> candidates = new ArrayList<String>();

        String primary = resolveTargetLocationGrade(rule, stock, actionMap);
        if (ValueUtil.isNotEmpty(primary)) {
            candidates.add(primary);
        }

        List<String> fallbackGrades = getStringList(actionMap, "fallbackLocationGrades");
        for (String grade : fallbackGrades) {
            if (ValueUtil.isNotEmpty(grade) && !candidates.contains(grade)) {
                candidates.add(grade);
            }
        }

        return candidates;
    }

    /**
     * task type 결정
     */
    private String resolveTaskType(TbAcStrategyRule rule, Map<String, Object> actionMap) {
        String taskType = getString(actionMap, "taskType");
        if (ValueUtil.isNotEmpty(taskType)) {
            return taskType;
        }

        String ruleType = nullToEmpty(rule.getRuleType()).toUpperCase();
        if ("GRADE_FORWARD".equals(ruleType)) {
            return StrategyTaskType.FORWARD_RELOCATION.getCode();
        }
        if ("GRADE_BACKWARD".equals(ruleType)) {
            return StrategyTaskType.BACKWARD_RELOCATION.getCode();
        }
        if ("DEMAND_PREPOS".equals(ruleType)) {
            return StrategyTaskType.PREPOSITION.getCode();
        }

        return StrategyTaskType.FORWARD_RELOCATION.getCode();
    }

    /**
     * reason code 결정
     */
    private String resolveReasonCode(TbAcStrategyRule rule, Map<String, Object> actionMap) {
        String reasonCode = getString(actionMap, "reasonCode");
        if (ValueUtil.isNotEmpty(reasonCode)) {
            return reasonCode;
        }

        String ruleType = nullToEmpty(rule.getRuleType()).toUpperCase();
        if ("GRADE_FORWARD".equals(ruleType)) {
            return "GRADE_MISMATCH";
        }
        if ("GRADE_BACKWARD".equals(ruleType)) {
            return "GRADE_REBALANCE";
        }
        if ("DEMAND_PREPOS".equals(ruleType)) {
            return "DEMAND_PREPOS";
        }

        return "STRATEGY_RULE";
    }

    /**
     * 목적지 후보 선택 (빈 위치 우선 + swap 옵션)
     */
    private RelocationTargetLocationRow findBestTargetWithSwap(String areaCode,
                                                               RelocationStockCandidateRow stock,
                                                               TbAcStrategyRule rule,
                                                               Map<String, Object> actionMap,
                                                               Set<String> assignedTargetLocationIds) {
        List<String> targetGrades = resolveTargetLocationGradeCandidates(rule, stock, actionMap);
        if (targetGrades.isEmpty()) {
            return null;
        }

        Boolean allowSwap = getBoolean(actionMap, "allowSwap");
        Boolean swapOnly = getBoolean(actionMap, "swapOnly");
        Boolean requireFrontPriority = getBoolean(actionMap, "requireFrontPriority");

        // 1) 빈 위치 우선
        if (!Boolean.TRUE.equals(swapOnly)) {
            for (String targetGrade : targetGrades) {
                List<RelocationTargetLocationRow> emptyTargets =
                        strategyQueryService.findEmptyTargetLocations(areaCode, targetGrade);

                for (RelocationTargetLocationRow target : emptyTargets) {
                    if (ValueUtil.isEmpty(target.getLocationId())) {
                        continue;
                    }
                    if (assignedTargetLocationIds.contains(target.getLocationId())) {
                        continue;
                    }
                    if (ValueUtil.isNotEmpty(stock.getCurrentLocationId())
                            && stock.getCurrentLocationId().equals(target.getLocationId())) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(requireFrontPriority) && !"Y".equals(target.getFrontPriorityYn())) {
                        continue;
                    }
                    return target;
                }
            }
        }

        // 2) swap 허용 시 점유 위치 검사
        if (Boolean.TRUE.equals(allowSwap)) {
            int currentItemRank = GradeRankType.rankOf(stock.getItemGrade());

            for (String targetGrade : targetGrades) {
                List<RelocationSwapCandidateRow> swapTargets =
                        strategyQueryService.findSwapTargetLocations(areaCode, targetGrade);

                for (RelocationSwapCandidateRow swap : swapTargets) {
                    if (ValueUtil.isEmpty(swap.getLocationId())) {
                        continue;
                    }
                    if (assignedTargetLocationIds.contains(swap.getLocationId())) {
                        continue;
                    }
                    if (ValueUtil.isNotEmpty(stock.getCurrentLocationId())
                            && stock.getCurrentLocationId().equals(swap.getLocationId())) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(requireFrontPriority) && !"Y".equals(swap.getFrontPriorityYn())) {
                        continue;
                    }

                    int targetItemRank = GradeRankType.rankOf(swap.getItemGrade());

                    // 현재 후보가 swap 대상 점유 재고보다 우수한 경우만 허용
                    if (currentItemRank < targetItemRank) {
                        RelocationTargetLocationRow target = new RelocationTargetLocationRow();
                        target.setLocationId(swap.getLocationId());
                        target.setLocationCode(swap.getLocationCode());
                        target.setLocationGrade(swap.getLocationGrade());
                        target.setSortSeq(swap.getSortSeq());
                        target.setFrontPriorityYn(swap.getFrontPriorityYn());
                        return target;
                    }
                }
            }
        }

        return null;
    }

    /**
     * reason text 조합
     */
    private String buildReasonText(TbAcStrategyRule rule,
                                   RelocationStockCandidateRow stock,
                                   RelocationTargetLocationRow target,
                                   String reasonCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("ruleType=").append(rule.getRuleType());
        sb.append(", reasonCode=").append(reasonCode);
        sb.append(", itemGrade=").append(stock.getItemGrade());
        sb.append(", fromLocationGrade=").append(stock.getCurrentLocationGrade());
        sb.append(", targetLocationGrade=").append(target == null ? "" : target.getLocationGrade());
        sb.append(", demandTomorrowQty=").append(stock.getDemandTomorrowQty() == null ? 0 : stock.getDemandTomorrowQty());
        return sb.toString();
    }

    private String resolveReasonCodeFromPreview(RelocationTaskPreviewRow previewTask) {
        String reason = nullToEmpty(previewTask.getReason());
        if (reason.contains("reasonCode=")) {
            int start = reason.indexOf("reasonCode=") + "reasonCode=".length();
            int end = reason.indexOf(",", start);
            if (end < 0) {
                return reason.substring(start).trim();
            }
            return reason.substring(start, end).trim();
        }
        return "STRATEGY_RULE";
    }

    private Map<String, Object> parseJson(String json) {
        if (ValueUtil.isEmpty(json)) {
            return new HashMap<String, Object>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "Invalid strategy rule json. " + e.getMessage()
            );
        }
    }

    private boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return Collections.emptyList();
        }

        List<String> results = new ArrayList<String>();

        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            for (Object obj : list) {
                if (obj != null) {
                    results.add(String.valueOf(obj));
                }
            }
        } else {
            results.add(String.valueOf(value));
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getStringMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || !(value instanceof Map)) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<String, String>();
        Map<Object, Object> raw = (Map<Object, Object>) value;

        Iterator<Map.Entry<Object, Object>> iterator = raw.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, Object> entry = iterator.next();
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        return result;
    }

    private String buildInitialRunSummaryJson(StrategyRunRequest request,
                                              TbAcStrategySet strategySet,
                                              List<TbAcStrategyRule> rules,
                                              int candidateCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"areaCode\":\"").append(request.getAreaCode()).append("\",");
        sb.append("\"strategySetId\":\"").append(strategySet.getId()).append("\",");
        sb.append("\"strategyCode\":\"").append(strategySet.getStrategyCode()).append("\",");
        sb.append("\"ruleCount\":").append(rules.size()).append(",");
        sb.append("\"candidateCount\":").append(candidateCount).append(",");
        sb.append("\"phase\":\"STARTED\"");
        sb.append("}");
        return sb.toString();
    }

    private String buildFinalRunSummaryJson(StrategyRunRequest request,
                                            TbAcStrategySet strategySet,
                                            List<TbAcStrategyRule> rules,
                                            int candidateCount,
                                            List<RelocationTaskPreviewRow> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"areaCode\":\"").append(request.getAreaCode()).append("\",");
        sb.append("\"strategySetId\":\"").append(strategySet.getId()).append("\",");
        sb.append("\"strategyCode\":\"").append(strategySet.getStrategyCode()).append("\",");
        sb.append("\"ruleCount\":").append(rules.size()).append(",");
        sb.append("\"candidateCount\":").append(candidateCount).append(",");
        sb.append("\"taskCount\":").append(tasks == null ? 0 : tasks.size()).append(",");
        sb.append("\"phase\":\"DONE\"");
        sb.append("}");
        return sb.toString();
    }

    private String buildFailureRunSummaryJson(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"phase\":\"FAILED\",");
        sb.append("\"message\":\"").append(escapeJson(e.getMessage())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\"", "\\\"");
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private void validateRequest(StrategyRunRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "request is null.");
        }
        if (ValueUtil.isEmpty(request.getAreaCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getStrategyCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "strategyCode is empty.");
        }
    }
}