package operato.logis.wcs.service.impl.scheduling;

import java.util.LinkedHashMap;
import java.util.Map;

import xyz.elidom.util.ValueUtil;

/**
 * 설명 가능한 점수 (priority/aging/efficiency/ga 구성 요소 보존).
 * 정렬에 직접 쓰는 값은 getTotal().
 */
public final class ScoreBreakdown {

    private final double total;
    private final double priorityValue;
    private final double agingValue;
    private final double efficiencyValue;
    private final Double gaScore;
    private final String formula;

    public ScoreBreakdown(double total,
                          double priorityValue,
                          double agingValue,
                          double efficiencyValue,
                          Double gaScore,
                          String formula) {
        this.total = total;
        this.priorityValue = priorityValue;
        this.agingValue = agingValue;
        this.efficiencyValue = efficiencyValue;
        this.gaScore = gaScore;
        this.formula = formula;
    }

    public double getTotal()           { return total; }
    public double getPriorityValue()   { return priorityValue; }
    public double getAgingValue()      { return agingValue; }
    public double getEfficiencyValue() { return efficiencyValue; }
    public Double getGaScore()         { return gaScore; }
    public String getFormula()         { return formula; }

    /**
     * JSON 응답에 직접 노출되는 Map 표현. 모든 값은 소수 둘째 자리 반올림.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", round2(total));
        m.put("priorityValue", round2(priorityValue));
        m.put("agingValue", round2(agingValue));
        m.put("efficiencyValue", round2(efficiencyValue));
        m.put("gaScore", gaScore == null ? null : round2(gaScore));
        m.put("formula", formula);
        return m;
    }

    @Override
    public String toString() {
        return String.format("[total=%.2f pri=%.1f age=%.1f eff=%.1f ga=%s formula=%s]",
                total, priorityValue, agingValue, efficiencyValue,
                ValueUtil.isEmpty(gaScore) ? "-" : String.format("%.1f", gaScore),
                formula);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
