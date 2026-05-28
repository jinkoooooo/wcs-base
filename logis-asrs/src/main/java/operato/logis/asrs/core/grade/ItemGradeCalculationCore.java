package operato.logis.asrs.core.grade;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import operato.logis.asrs.query.grade.ItemActivityQueryService;
import operato.logis.asrs.query.grade.ItemGradeQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.response.ItemGradeCalculationResult;
import operato.logis.asrs.entity.TbAcGradePolicy;
import operato.logis.asrs.entity.TbAcItemGrade;
import operato.logis.asrs.entity.TbAcItemGradeHist;
import operato.logis.asrs.query.grade.model.ItemActivityDailyView;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 상품 등급 계산 코어.
 *
 * <p>
 * 입력:
 * - tb_ac_item_activity_daily
 * - tb_ac_grade_policy
 * - tb_ac_item_master.storage_grade_seed
 * </p>
 *
 * <p>
 * 출력:
 * - tb_ac_item_grade upsert
 * - tb_ac_item_grade_hist insert
 * </p>
 *
 * <p>
 * 1차 규칙:
 * </p>
 * <ul>
 *   <li>REALTIME/BATCH = finalScore = learnedScore</li>
 *   <li>HYBRID = finalScore = max(learnedScore, manualSeedScore)</li>
 *   <li>manualSeedScore 없고 manualSeedGrade 있으면 scheme threshold 점수로 환산</li>
 *   <li>score_formula_json 이 없으면 learnedScore = scoreRaw</li>
 *   <li>score_formula_json 에 threshold가 있으면 그걸 우선 사용</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemGradeCalculationCore extends AbstractQueryService {

    private final ItemActivityQueryService itemActivityQueryService;
    private final ItemGradeQueryService itemGradeQueryService;
    private final LocationQueryService locationQueryService;
    private final ItemQueryService itemQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 영역 기준 등급 계산 실행.
     *
     * @param areaCode 영역 코드
     * @param activityDate 기준 일자
     * @param policyCode 정책 코드(선택)
     * @return 계산 결과
     */
    @Transactional
    public ItemGradeCalculationResult calculateArea(String areaCode, LocalDate activityDate, String policyCode) {
        validateRequest(areaCode, activityDate);

        TbAcGradePolicy policy = itemGradeQueryService.findApplicablePolicy(policyCode, activityDate);
        List<ItemActivityDailyView> activities = itemActivityQueryService.findDailyActivities(areaCode, activityDate);

        int calculatedCount = 0;
        int historyCount = 0;

        for (ItemActivityDailyView activity : activities) {
            boolean historyCreated = calculateOne(activity, policy, activityDate);
            calculatedCount++;
            if (historyCreated) {
                historyCount++;
            }
        }

        log.info("[AisleCore][GRADE][AREA] areaCode={}, activityDate={}, policyCode={}, calculatedCount={}, historyCount={}",
                areaCode, activityDate, policy.getPolicyCode(), calculatedCount, historyCount);

        return ItemGradeCalculationResult.builder()
                .areaCode(areaCode)
                .itemCode(null)
                .activityDate(activityDate.toString())
                .policyCode(policy.getPolicyCode())
                .targetItemCount(activities == null ? 0 : activities.size())
                .calculatedCount(calculatedCount)
                .historyCount(historyCount)
                .message("Item grade calculation completed by area.")
                .build();
    }

    /**
     * 영역 + 품목 기준 등급 계산 실행.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @param activityDate 기준 일자
     * @param policyCode 정책 코드(선택)
     * @return 계산 결과
     */
    @Transactional
    public ItemGradeCalculationResult calculateItem(String areaCode, String itemCode, LocalDate activityDate, String policyCode) {
        validateRequest(areaCode, activityDate);

        if (ValueUtil.isEmpty(itemCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "itemCode is empty.");
        }

        TbAcGradePolicy policy = itemGradeQueryService.findApplicablePolicy(policyCode, activityDate);
        ItemActivityDailyView activity = itemActivityQueryService.findDailyActivity(areaCode, itemCode, activityDate);

        boolean historyCreated = calculateOne(activity, policy, activityDate);

        log.info("[AisleCore][GRADE][ITEM] areaCode={}, itemCode={}, activityDate={}, policyCode={}",
                areaCode, itemCode, activityDate, policy.getPolicyCode());

        return ItemGradeCalculationResult.builder()
                .areaCode(areaCode)
                .itemCode(itemCode)
                .activityDate(activityDate.toString())
                .policyCode(policy.getPolicyCode())
                .targetItemCount(1)
                .calculatedCount(1)
                .historyCount(historyCreated ? 1 : 0)
                .message("Item grade calculation completed by item.")
                .build();
    }

    /**
     * 개별 품목 등급 계산/저장.
     *
     * @param activity 일 집계 row
     * @param policy 적용 정책
     * @param activityDate 기준 일자
     * @return history 생성 여부
     */
    private boolean calculateOne(ItemActivityDailyView activity, TbAcGradePolicy policy, LocalDate activityDate) {
        TbAcItemGrade existing = itemGradeQueryService.findCurrentItemGradeOrNull(activity.getAreaId(), activity.getItemId());

        String manualSeedGrade = resolveManualSeedGrade(existing, activity.getItemCode());
        Integer manualSeedScore = resolveManualSeedScore(existing, manualSeedGrade, policy);

        int learnedScore = calculateLearnedScore(activity, policy);
        int finalScore = resolveFinalScore(policy.getApplyMode(), learnedScore, manualSeedScore);
        String currentGrade = resolveGrade(policy, finalScore);

        Integer previousScore = existing == null ? null : existing.getFinalScore();
        String previousGrade = existing == null ? null : existing.getCurrentGrade();

        TbAcItemGrade grade = existing;
        if (grade == null) {
            grade = new TbAcItemGrade();
            grade.setAreaId(activity.getAreaId());
            grade.setItemId(activity.getItemId());
        }

        grade.setGradePolicyId(policy.getId());
        grade.setManualSeedGrade(manualSeedGrade);
        grade.setManualSeedScore(manualSeedScore);
        grade.setLearnedScore(learnedScore);
        grade.setFinalScore(finalScore);
        grade.setCurrentGrade(currentGrade);
        grade.setLastCalculatedAt(new Date());

        if (existing == null) {
            this.queryManager.insert(grade);
        } else {
            this.queryManager.update(
                    grade,
                    "gradePolicyId",
                    "manualSeedGrade",
                    "manualSeedScore",
                    "learnedScore",
                    "finalScore",
                    "currentGrade",
                    "lastCalculatedAt"
            );
        }

        boolean changed = isChanged(previousGrade, currentGrade, previousScore, finalScore);
        if (changed) {
            insertHistory(grade, policy, previousGrade, currentGrade, previousScore, finalScore, activity, activityDate, learnedScore, manualSeedGrade, manualSeedScore);
        }

        return changed;
    }

    /**
     * 수동 seed grade 결정.
     *
     * <p>
     * 우선순위:
     * 1) tb_ac_item_grade.manual_seed_grade
     * 2) tb_ac_item_master.storage_grade_seed
     * </p>
     */
    private String resolveManualSeedGrade(TbAcItemGrade existing, String itemCode) {
        if (existing != null && ValueUtil.isNotEmpty(existing.getManualSeedGrade())) {
            return existing.getManualSeedGrade();
        }

        return itemQueryService.findItemByCode(itemCode).getStorageGradeSeed();
    }

    /**
     * 수동 seed score 결정.
     *
     * <p>
     * 우선순위:
     * 1) 기존 item_grade.manual_seed_score
     * 2) manualSeedGrade 기반 기본 threshold 점수
     * </p>
     */
    private Integer resolveManualSeedScore(TbAcItemGrade existing, String manualSeedGrade, TbAcGradePolicy policy) {
        if (existing != null && existing.getManualSeedScore() != null) {
            return existing.getManualSeedScore();
        }

        if (ValueUtil.isEmpty(manualSeedGrade)) {
            return null;
        }

        Map<String, Integer> thresholdMap = buildThresholdMap(policy);
        Integer score = thresholdMap.get(manualSeedGrade);
        return score == null ? null : score;
    }

    /**
     * learnedScore 계산.
     *
     * <p>
     * 기본은 scoreRaw 그대로 사용하고,
     * score_formula_json 에 weights 가 있으면 반영한다.
     * </p>
     */
    private int calculateLearnedScore(ItemActivityDailyView activity, TbAcGradePolicy policy) {
        if (ValueUtil.isEmpty(policy.getScoreFormulaJson())) {
            return safeInt(activity.getScoreRaw());
        }

        try {
            Map<String, Object> root = objectMapper.readValue(
                    policy.getScoreFormulaJson(),
                    new TypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> weights = getMap(root, "weights");
            if (weights.isEmpty()) {
                return safeInt(activity.getScoreRaw());
            }

            double score = 0.0d;
            score += safeInt(activity.getScoreRaw()) * getDouble(weights, "scoreRaw", 1.0d);
            score += safeInt(activity.getDemandTomorrowQty()) * getDouble(weights, "demandTomorrowQty", 0.0d);
            score += safeInt(activity.getOutboundQty()) * getDouble(weights, "outboundQty", 0.0d);
            score += safeInt(activity.getOutboundCount()) * getDouble(weights, "outboundCount", 0.0d);
            score += safeInt(activity.getPartialOutCount()) * getDouble(weights, "partialOutCount", 0.0d);
            score += safeInt(activity.getReturnInCount()) * getDouble(weights, "returnInCount", 0.0d);
            score += safeInt(activity.getMoveCount()) * getDouble(weights, "moveCount", 0.0d);
            score += safeInt(activity.getInboundCount()) * getDouble(weights, "inboundCount", 0.0d);
            score += safeInt(activity.getAvgDwellDays()) * getDouble(weights, "avgDwellDays", 0.0d);

            return (int) Math.round(score);
        } catch (Exception e) {
            log.warn("[AisleCore][GRADE] score_formula_json parse failed. policyCode={}, fallback scoreRaw used. cause={}",
                    policy.getPolicyCode(), e.getMessage());
            return safeInt(activity.getScoreRaw());
        }
    }

    /**
     * finalScore 계산.
     */
    private int resolveFinalScore(String applyMode, int learnedScore, Integer manualSeedScore) {
        String mode = ValueUtil.isEmpty(applyMode) ? "REALTIME" : applyMode.toUpperCase();

        if ("HYBRID".equals(mode)) {
            return Math.max(learnedScore, safeInt(manualSeedScore));
        }

        // REALTIME / BATCH / 기타 1차 기본값
        return learnedScore;
    }

    /**
     * score → grade 변환.
     */
    private String resolveGrade(TbAcGradePolicy policy, int finalScore) {
        List<GradeThreshold> thresholds = buildThresholds(policy);

        for (GradeThreshold threshold : thresholds) {
            if (finalScore >= threshold.getMinScore()) {
                return threshold.getGrade();
            }
        }

        if (!thresholds.isEmpty()) {
            return thresholds.get(thresholds.size() - 1).getGrade();
        }

        return "C";
    }

    /**
     * threshold map 생성.
     */
    private Map<String, Integer> buildThresholdMap(TbAcGradePolicy policy) {
        List<GradeThreshold> thresholds = buildThresholds(policy);
        Map<String, Integer> map = new LinkedHashMap<>();
        for (GradeThreshold threshold : thresholds) {
            map.put(threshold.getGrade(), threshold.getMinScore());
        }
        return map;
    }

    /**
     * threshold list 생성.
     *
     * <p>
     * 우선순위:
     * 1) score_formula_json.thresholds
     * 2) score_formula_json.gradeThresholds
     * 3) grade_scheme 기본 threshold
     * </p>
     */
    private List<GradeThreshold> buildThresholds(TbAcGradePolicy policy) {
        List<GradeThreshold> thresholdsFromJson = parseThresholdsFromJson(policy.getScoreFormulaJson());
        if (!thresholdsFromJson.isEmpty()) {
            thresholdsFromJson.sort((a, b) -> Integer.compare(b.getMinScore(), a.getMinScore()));
            return thresholdsFromJson;
        }

        return buildDefaultThresholds(policy.getGradeScheme());
    }

    /**
     * score_formula_json 에서 threshold 파싱.
     */
    private List<GradeThreshold> parseThresholdsFromJson(String json) {
        if (ValueUtil.isEmpty(json)) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> root = objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {}
            );

            Object raw = root.get("thresholds");
            if (raw == null) {
                raw = root.get("gradeThresholds");
            }
            if (raw == null) {
                raw = root.get("grade_thresholds");
            }
            if (!(raw instanceof List<?>)) {
                return Collections.emptyList();
            }

            List<GradeThreshold> result = new ArrayList<>();
            for (Object obj : (List<?>) raw) {
                if (!(obj instanceof Map<?, ?>)) {
                    continue;
                }
                Map<?, ?> row = (Map<?, ?>) obj;

                String grade = toString(row.get("grade"));
                if (ValueUtil.isEmpty(grade)) {
                    grade = toString(row.get("code"));
                }

                Integer minScore = toInteger(row.get("minScore"));
                if (minScore == null) {
                    minScore = toInteger(row.get("min_score"));
                }

                if (ValueUtil.isNotEmpty(grade) && minScore != null) {
                    result.add(new GradeThreshold(grade.toUpperCase(), minScore));
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("[AisleCore][GRADE] threshold parse failed. cause={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * grade_scheme 기반 기본 threshold 생성.
     *
     * <p>
     * 예:
     * ABC   -> A/B/C
     * ABCD  -> A/B/C/D
     * SABCD -> S/A/B/C/D
     * </p>
     */
    private List<GradeThreshold> buildDefaultThresholds(String gradeScheme) {
        String scheme = ValueUtil.isEmpty(gradeScheme) ? "ABC" : gradeScheme.trim().toUpperCase();

        List<String> grades = new ArrayList<>();
        for (int i = 0; i < scheme.length(); i++) {
            grades.add(String.valueOf(scheme.charAt(i)));
        }

        if (grades.isEmpty()) {
            grades.add("A");
            grades.add("B");
            grades.add("C");
        }

        int levelCount = grades.size();
        int step = (int) Math.floor(1000.0d / levelCount);

        List<GradeThreshold> result = new ArrayList<>();
        for (int i = 0; i < grades.size(); i++) {
            int minScore = Math.max(0, (levelCount - 1 - i) * step);
            result.add(new GradeThreshold(grades.get(i), minScore));
        }

        return result;
    }

    /**
     * 변경 이력 저장.
     */
    private void insertHistory(TbAcItemGrade grade,
                               TbAcGradePolicy policy,
                               String previousGrade,
                               String newGrade,
                               Integer previousScore,
                               Integer newScore,
                               ItemActivityDailyView activity,
                               LocalDate activityDate,
                               Integer learnedScore,
                               String manualSeedGrade,
                               Integer manualSeedScore) {

        TbAcItemGradeHist hist = new TbAcItemGradeHist();
        hist.setItemGradeId(grade.getId());
        hist.setGradePolicyId(policy.getId());
        hist.setPreviousGrade(previousGrade);
        hist.setNewGrade(newGrade);
        hist.setPreviousScore(previousScore);
        hist.setNewScore(newScore);
        hist.setReasonJson(buildReasonJson(policy, activity, activityDate, learnedScore, manualSeedGrade, manualSeedScore));
        hist.setCalculatedAt(new Date());

        this.queryManager.insert(hist);
    }

    /**
     * 이력 생성 필요 여부.
     */
    private boolean isChanged(String previousGrade, String currentGrade, Integer previousScore, Integer finalScore) {
        if (!equalsNullable(previousGrade, currentGrade)) {
            return true;
        }
        return !equalsNullable(previousScore, finalScore);
    }

    /**
     * reason_json 생성.
     */
    private String buildReasonJson(TbAcGradePolicy policy,
                                   ItemActivityDailyView activity,
                                   LocalDate activityDate,
                                   Integer learnedScore,
                                   String manualSeedGrade,
                                   Integer manualSeedScore) {
        try {
            Map<String, Object> reason = new LinkedHashMap<>();
            reason.put("activityDate", activityDate.toString());
            reason.put("policyCode", policy.getPolicyCode());
            reason.put("applyMode", policy.getApplyMode());
            reason.put("gradeScheme", policy.getGradeScheme());
            reason.put("itemCode", activity.getItemCode());
            reason.put("areaCode", activity.getAreaCode());
            reason.put("scoreRaw", activity.getScoreRaw());
            reason.put("learnedScore", learnedScore);
            reason.put("manualSeedGrade", manualSeedGrade);
            reason.put("manualSeedScore", manualSeedScore);
            reason.put("inboundCount", activity.getInboundCount());
            reason.put("outboundCount", activity.getOutboundCount());
            reason.put("outboundQty", activity.getOutboundQty());
            reason.put("partialOutCount", activity.getPartialOutCount());
            reason.put("returnInCount", activity.getReturnInCount());
            reason.put("moveCount", activity.getMoveCount());
            reason.put("avgDwellDays", activity.getAvgDwellDays());
            reason.put("demandTomorrowQty", activity.getDemandTomorrowQty());

            return objectMapper.writeValueAsString(reason);
        } catch (Exception e) {
            return "{\"error\":\"reason_json_build_failed\"}";
        }
    }

    /**
     * 공통 요청 검증.
     */
    private void validateRequest(String areaCode, LocalDate activityDate) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (activityDate == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "activityDate is null.");
        }
    }

    private boolean equalsNullable(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Map<String, Object> getMap(Map<String, Object> root, String key) {
        if (root == null) {
            return Collections.emptyMap();
        }
        Object value = root.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Collections.emptyMap();
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GradeThreshold {
        private String grade;
        private Integer minScore;
    }
}