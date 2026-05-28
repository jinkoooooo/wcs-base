package operato.logis.wcs.service.impl.scheduling;

import org.springframework.stereotype.Component;

/**
 * 운동학 계산기 — 사다리꼴 속도 프로파일 기반 셔틀/리프트 이동 시간 추정.
 *
 * 창고 제원은 코드 상수로 고정. GA 적합도 함수에서 자주 호출되므로 순수 함수로 유지.
 * 실측 보정이 필요하면 상단 상수만 조정.
 */
@Component
public class KinematicsCalculator {

    private static final double CELL_WIDTH_X    = 1.10;
    private static final double CELL_LENGTH_Y   = 1.35;
    private static final double LEVEL_HEIGHT_Z  = 1.50;
    private static final double SHUTTLE_VMAX    = 2.0;
    private static final double SHUTTLE_ACCEL   = 1.0;
    private static final double LIFT_VMAX       = 2.0;
    private static final double LIFT_ACCEL      = 1.0;
    private static final double TURNING_TIME    = 1.0;

    /**
     * 1차원 사다리꼴 프로파일 — 가속·정속·감속 합산 이동 시간(초).
     */
    public double travelTime(double distanceM, double vMax, double accel) {
        if (distanceM <= 0) return 0.0;
        double accelDistance = (vMax * vMax) / accel;
        if (distanceM >= accelDistance) {
            return (distanceM / vMax) + (vMax / accel);
        }
        return 2.0 * Math.sqrt(distanceM / accel);
    }

    /**
     * 셔틀 수평 이동 시간 (row + bay + 회전 보정).
     */
    public double shuttleHorizontalTime(int fromRow, int fromBay, int toRow, int toBay) {
        double dRow = Math.abs(toRow - fromRow) * CELL_LENGTH_Y;
        double dBay = Math.abs(toBay - fromBay) * CELL_WIDTH_X;
        double rowT = travelTime(dRow, SHUTTLE_VMAX, SHUTTLE_ACCEL);
        double bayT = travelTime(dBay, SHUTTLE_VMAX, SHUTTLE_ACCEL);
        int turns = (fromRow != toRow && fromBay != toBay) ? 1 : 0;
        return rowT + bayT + turns * TURNING_TIME;
    }

    /**
     * 리프트 수직 이동 시간.
     */
    public double liftTravelTime(int fromLevel, int toLevel) {
        double d = Math.abs(toLevel - fromLevel) * LEVEL_HEIGHT_Z;
        return travelTime(d, LIFT_VMAX, LIFT_ACCEL);
    }

    /**
     * 한 작업의 총 시간 추정 — 셔틀 시작 → from 적재 → to 하역.
     * 리프트는 셔틀 진입/이동 시 level 이 바뀌면 호출된다.
     */
    public double estimateTaskTime(int sRow, int sBay, int sLevel,
                                   int fRow, int fBay, int fLevel,
                                   int tRow, int tBay, int tLevel,
                                   int liftLevel) {
        double total = 0;
        if (sLevel != fLevel) {
            total += liftTravelTime(liftLevel, sLevel);
            total += liftTravelTime(sLevel, fLevel);
        }
        total += shuttleHorizontalTime(sRow, sBay, fRow, fBay);
        if (fLevel != tLevel) {
            total += liftTravelTime(fLevel, tLevel);
        }
        total += shuttleHorizontalTime(fRow, fBay, tRow, tBay);
        return total;
    }
}
