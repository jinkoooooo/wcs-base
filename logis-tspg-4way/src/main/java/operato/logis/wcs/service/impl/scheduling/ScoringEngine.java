package operato.logis.wcs.service.impl.scheduling;

import xyz.elidom.util.ValueUtil;

/**
 * 우선순위 점수 계산 엔진 — 정적 유틸.
 *
 * throughput 우선: efficiency 50% + priority 30% + aging 20%.
 * 가혹한 efficiency 곡선으로 안쪽 우선 현상을 차단한다. aging 은 보조이며
 * 진짜 끼임은 release 측의 force_release(>120) 안전망이 처리한다.
 * gaScore 가 주어지면 30% 블렌드.
 */
public final class ScoringEngine {

    private static final double PRIORITY_WEIGHT_NO_GA   = 0.30;
    private static final double AGING_WEIGHT_NO_GA      = 0.20;
    private static final double EFFICIENCY_WEIGHT_NO_GA = 0.50;

    private static final double PRIORITY_WEIGHT_WITH_GA   = 0.20;
    private static final double AGING_WEIGHT_WITH_GA      = 0.10;
    private static final double EFFICIENCY_WEIGHT_WITH_GA = 0.40;
    private static final double GA_BLEND_WEIGHT           = 0.30;

    private static final double EFFICIENCY_NUMERATOR_AFTER_FIRST = 20.0;

    private ScoringEngine() {}

    /**
     * 번들(parent + child) 점수 — childMoveCount 가 클수록 efficiency 점수가 가파르게 감소.
     * gaScore 가 있으면 30% 블렌드된 결과를 반환한다.
     */
    public static ScoreBreakdown computeBundleScore(int priority, int agingCount, int agingCap,
                                                    int childMoveCount, Double gaScore) {
        double priorityValue = 100.0 / Math.max(priority, 1);
        double agingValue = Math.min((double) agingCount / Math.max(agingCap, 1), 1.0) * 100.0;
        double efficiencyValue = childMoveCount == 0
                ? 100.0
                : EFFICIENCY_NUMERATOR_AFTER_FIRST / childMoveCount;

        double total;
        String formula;
        if (ValueUtil.isEmpty(gaScore)) {
            total = PRIORITY_WEIGHT_NO_GA * priorityValue
                    + AGING_WEIGHT_NO_GA * agingValue
                    + EFFICIENCY_WEIGHT_NO_GA * efficiencyValue;
            formula = "v3-bundle-noGa(30/20/50,harsh-eff)";
        } else {
            total = PRIORITY_WEIGHT_WITH_GA * priorityValue
                    + AGING_WEIGHT_WITH_GA * agingValue
                    + EFFICIENCY_WEIGHT_WITH_GA * efficiencyValue
                    + GA_BLEND_WEIGHT * gaScore;
            formula = "v3-bundle-withGa(20/10/40+30,harsh-eff)";
        }

        return new ScoreBreakdown(total, priorityValue, agingValue,
                efficiencyValue, gaScore, formula);
    }

    /**
     * 단일 MOVE 점수 — priority 단독, gaScore 가 있으면 30% 블렌드.
     */
    public static ScoreBreakdown computeMoveScore(int priority, Double gaScore) {
        double priorityValue = 100.0 / Math.max(priority, 1);
        if (ValueUtil.isEmpty(gaScore)) {
            return new ScoreBreakdown(priorityValue, priorityValue, 0.0, 100.0, null, "v3-move-noGa");
        }
        double total = (1.0 - GA_BLEND_WEIGHT) * priorityValue + GA_BLEND_WEIGHT * gaScore;
        return new ScoreBreakdown(total, priorityValue, 0.0, 100.0, gaScore, "v3-move-withGa(70+30)");
    }
}
