package operato.logis.samsung.service.hokusho.core;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.DimCheckMode;
import operato.logis.samsung.consts.VisionJudgeResult;
import operato.logis.samsung.entity.mw.TbMwBcrItemDimensionAvg;
import operato.logis.samsung.entity.mw.TbMwBox;
import operato.logis.samsung.entity.mw.TbMwItemMaster;
import org.springframework.stereotype.Component;
import xyz.elidom.sys.util.SettingUtil;

@Component
@RequiredArgsConstructor
public class BoxDecisionService {


    /* =========================
     * 1) 비전(코그넥스/AI/메뉴얼) 종합 판정
     * ========================= */

    /**
     * VISION 디버트 판단:
     * - 세 값 중 하나라도 null  : WAIT (아직 판정 대기 → 지시 내리지 말 것)
     * - 세 값 모두 1            : ALL_OK (PATH)
     * - 그 외(0 또는 기타 값 포함) : HAS_NG (NG)
     */
    public VisionJudgeResult judgeForVisionDivert(TbMwBox box) {
        Integer c = box.getCognexResult();
        /*Integer s = box.getSdsAiResult();*/
        Integer m = box.getManualResult();

        // 하나라도 아직 안들어온 경우 → 대기
        if (c == null || m == null/* || s == null || m == null*/) {
            return VisionJudgeResult.WAIT;
        }

        boolean allOk = isOk(c) && isOk(m)/* && isOk(s) && isOk(m)*/;
        return allOk ? VisionJudgeResult.ALL_OK : VisionJudgeResult.HAS_NG;
    }



    private static boolean isOk(Integer v) { return v != null && v == 1; }
    private static boolean isNg(Integer v) { return v != null && v == 0; }



    /* =========================
     * 2) 체적/방향 판정 + 사유 메시지
     * ========================= */

    public DimCheckResult checkDimensionAndOrientation(TbMwBox box, TbMwItemMaster item) {
        if (box == null || item == null) {
            return DimCheckResult.noCheck();
        }


        // 측정값(BCR/VISION 누적값) - mm 단위라고 가정
        Integer measL = box.getBoxLength();
        Integer measW = box.getBoxWidth();
        Integer measH = box.getBoxHeight();
        Integer angle = box.getBoxAngle();

        // 기준값(상품마스터) - mm
        Integer stdL  = item.getItemLength();
        Integer stdW  = item.getItemWidth();
        Integer stdH  = item.getItemHeight();

        // 어떤 축을 검사할지 옵션 (예: "H", "LW", "LWH" 등)
        String opt = SettingUtil.getValue("mw.bcr.dim-validation_option", "H");
        DimCheckMode mode = DimCheckMode.from(opt);

        // 허용 오차 mm
        int tolMm = Integer.parseInt(
                SettingUtil.getValue("mw.bcr.dim-tolerance-mm", "10")
        );

        boolean outL = mode.checkL() && isOutOfTolerance(measL, stdL, tolMm);
        boolean outW = mode.checkW() && isOutOfTolerance(measW, stdW, tolMm);
        boolean outH = mode.checkH() && isOutOfTolerance(measH, stdH, tolMm);

        boolean reject = outL || outW || outH;

        BoxLongSideDirection dir =
                detectLongSideDirection(measL, measW, stdL, stdW, tolMm, angle);

        String reason = null;
        if (reject) {
            reason = buildDimRejectReason(
                    box.getBoxId(),
                    measL, measW, measH,
                    stdL,  stdW,  stdH,
                    mode, tolMm, outL, outW, outH
            );
        }

        return new DimCheckResult(reject, dir, reason);
    }

    /* =========================
     * 내부 공통 로직 (필요하면 다른 곳에서 재사용 가능)
     * ========================= */

    private boolean isOutOfTolerance(Integer measured, Integer standard, int tolMm) {
        if (measured == null || standard == null) return true; // 값 없는 것도 NG 취급
        return Math.abs(measured - standard) > tolMm;
    }

    /** 장변 방향 판정 (앞으로 / 옆으로 / 모름) */
    private BoxLongSideDirection detectLongSideDirection(
            Integer measL, Integer measW,
            Integer stdL,  Integer stdW,
            int tolMm, Integer angle
    ) {

        if (measL == null || measW == null || stdL == null || stdW == null) {
            return BoxLongSideDirection.UNKNOWN;
        }
        if (stdL.equals(stdW)) {
            // 정사각형이면 장변 개념 없음 턴 안함.
            return BoxLongSideDirection.LONG_SIDE_FORWARD;
        }

        int turningAngle = Integer.parseInt(SettingUtil.getValue("mw.bcr.dim-turning-angle", "70"));

        if(Math.abs(angle) < turningAngle){
            return BoxLongSideDirection.LONG_SIDE_SIDEWAYS;
        }else{
            return BoxLongSideDirection.LONG_SIDE_FORWARD;
        }
    }

    /** 축별 NG 정보 기반으로 한글/영문 사유 메시지 생성 */
    private String buildDimRejectReason(
            String boxId,
            Integer measL, Integer measW, Integer measH,
            Integer stdL,  Integer stdW,  Integer stdH,
            DimCheckMode mode,
            int tolMm,
            boolean outL, boolean outW, boolean outH
    ) {
        StringBuilder sb = new StringBuilder();

        if (outL) {
            appendAxisReason(sb, boxId, "LENGTH", "가로", measL, stdL, tolMm);
        }
        if (outW) {
            appendAxisReason(sb, boxId, "WIDTH", "세로", measW, stdW, tolMm);
        }
        if (outH) {
            appendAxisReason(sb, boxId, "HEIGHT", "높이", measH, stdH, tolMm);
        }

        if (sb.length() == 0) {
            return String.format(
                    "[%s] BOX DIMENSION NG: 측정값이 기준 대비 허용오차를 초과했습니다. (Dimension out of tolerance)",
                    boxId
            );
        }

        return sb.toString();
    }

    private void appendAxisReason(
            StringBuilder sb,
            String boxId,
            String axisEn, String axisKo,
            Integer meas, Integer std,
            int tolMm
    ) {
        if (sb.length() > 0) {
            sb.append(" / ");
        }

        if (meas == null || std == null) {
            sb.append(String.format(
                    "[%s] %s(%s) 측정값 또는 기준값이 없습니다. (Missing %s %s value)",
                    boxId, axisKo, axisEn, axisKo, axisEn
            ));
            return;
        }

        int diff = meas - std;
        String sign = diff >= 0 ? "+" : "";

        sb.append(String.format(
                "[%s] BOX %s NG: 측정(%dmm)이 기준(%dmm) 대비 차이(%s%dmm)가 허용오차(%dmm)를 초과했습니다. (%s out of tolerance)",
                boxId, axisKo, meas, std, sign, diff, tolMm, axisEn
        ));
    }

    /* =========================
     * 리턴용 DTO + 방향 enum
     * ========================= */

    public enum BoxLongSideDirection {
        UNKNOWN,
        LONG_SIDE_FORWARD,    // 장변이 진행방향과 수직 턴 안함.
        LONG_SIDE_SIDEWAYS    // 장변이 진행방향에 평행 턴함
    }

    public static class DimCheckResult {
        private final boolean reject;
        private final BoxLongSideDirection direction;
        private final String reason; // 체적 NG일 때 메시지

        public DimCheckResult(boolean reject,
                              BoxLongSideDirection direction,
                              String reason) {
            this.reject = reject;
            this.direction = direction;
            this.reason = reason;
        }

        public static DimCheckResult noCheck() {
            return new DimCheckResult(false, BoxLongSideDirection.UNKNOWN, null);
        }

        public boolean isReject() {
            return reject;
        }

        public BoxLongSideDirection getDirection() {
            return direction;
        }

        public String getReason() {
            return reason;
        }
    }
}
