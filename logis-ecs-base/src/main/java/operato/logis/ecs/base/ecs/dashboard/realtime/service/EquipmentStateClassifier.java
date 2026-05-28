package operato.logis.ecs.base.ecs.dashboard.realtime.service;

import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import org.springframework.util.StringUtils;

/**
 * Dashboard2D 범례용 정규화 분류기.
 *
 * - Shuttle/Conveyor/Pallet 원시 컬럼 → legend-spec 코드값 단일 변환점.
 * - 프런트는 이 클래스가 내려보낸 문자열 코드만 보고 색을 결정한다.
 * - 분류 결과는 enum.name() 으로 DTO 에 담는다.
 */
public final class EquipmentStateClassifier {

    private EquipmentStateClassifier() {
    }

    public enum ShuttleState {DISABLED, ERROR, MANUAL, CHARGING, RUNNING, IDLE}

    public enum ConveyorState {DISABLED, ERROR, MANUAL, AUTO}

    public enum PalletState {DATA_AND_PHY, PHY_ONLY, DATA_ONLY}

    /**
     * classifyShuttleState 입력 — 원시 컬럼 뭉치를 이름 있는 단위로 묶는 값 객체.
     * Provider 가 raw Map(SQL 결과) 에서 조립해 전달한다.
     */
    public record ShuttleStateInput(
            boolean useYn,
            String errorId,
            int status,
            boolean autoYn,
            int batteryStatus) {
    }

    /** classifyConveyorState 입력. */
    public record ConveyorStateInput(
            boolean useYn,
            String errorId,
            boolean autoYn) {
    }

    /**
     * 우선순위: DISABLED > ERROR > MANUAL > CHARGING > RUNNING > IDLE
     *
     * ERROR 는 오직 error_id 로만 판정한다. status == EMR_STOP(5) / ERROR(8) 로
     * ERROR 를 판정하면 error_id 는 비었는데(또는 '0') status 만 5/8 인 케이스에서
     * AlarmDataProvider (error_id IS NOT NULL AND <> '0') 와 어긋나
     * "알람은 안 울리는데 화면만 빨강" 이 된다.
     */
    public static ShuttleState classifyShuttleState(ShuttleStateInput in) {
        if (!in.useYn()) {
            return ShuttleState.DISABLED;
        }
        if (hasError(in.errorId())) {
            return ShuttleState.ERROR;
        }
        if (!in.autoYn()) {
            return ShuttleState.MANUAL;
        }
        //if (in.batteryStatus() == EcsDBConsts.EqCarBatteryStatus.CHARGING.getValue()) {
        //    return ShuttleState.CHARGING;
        //}
        //int status = in.status();
        //if (status == EcsDBConsts.EqCarStatus.RUN.getValue()) {
        //    return ShuttleState.RUNNING;
        //}
        return ShuttleState.IDLE;
    }

    /** 우선순위: DISABLED > ERROR > MANUAL > AUTO */
    public static ConveyorState classifyConveyorState(ConveyorStateInput in) {
        if (!in.useYn()) {
            return ConveyorState.DISABLED;
        }
        if (hasError(in.errorId())) {
            return ConveyorState.ERROR;
        }
        if (!in.autoYn()) {
            return ConveyorState.MANUAL;
        }
        return ConveyorState.AUTO;
    }

    /** Conveyor pallet 3-way 판정. (false,false) 인 경우 null 반환 → pallet 미렌더. */
    public static PalletState classifyPalletState(boolean hasData, boolean hasPhy) {
        if (hasData && hasPhy) {
            return PalletState.DATA_AND_PHY;
        }
        if (!hasData && hasPhy) {
            return PalletState.PHY_ONLY;
        }
        if (hasData && !hasPhy) {
            return PalletState.DATA_ONLY;
        }
        return null;
    }

    /** 에러 판정 단일 소스. null / 빈문자열 / "0" 은 정상으로 간주. */
    private static boolean hasError(String errorId) {
        return StringUtils.hasText(errorId) && !"0".equals(errorId.trim());
    }
}
