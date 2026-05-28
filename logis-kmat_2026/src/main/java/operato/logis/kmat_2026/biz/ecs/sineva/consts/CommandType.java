package operato.logis.kmat_2026.biz.ecs.sineva.consts;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipTaskType;
import xyz.elidom.exception.server.ElidomRuntimeException;

/**
 * ====================================================================
 * CommandType
 * ====================================================================
 *
 * [설명]
 * - WCS/ECS 내부에서 사용하는 Command 유형 정의
 * - 각 CommandType은 자신이 어떤 EquipTaskType으로 매핑되는지 책임을 가짐
 *
 * [리팩토링 포인트]
 * - 기존 switch-case 제거
 * - CommandType → TaskType 매핑을 enum 내부로 이동
 *
 * [장점]
 * - 유지보수 용이 (CommandType 추가 시 enum만 수정)
 * - 책임 집중 (도메인 구조 명확)
 */
public enum CommandType implements EnumCode<String> {

    UNKNOWN("UNKNOWN", "알 수 없음", null),

    MANUAL_COMMAND("MANUAL", "매뉴얼 지시", EquipTaskType.FREIGHT_MOVE),

    /**
     * ================================
     * 2026 KOREA MAT 전시회 Command
     * ================================
     */
    K_MAT_WCS("k_mat_wcs", "K-MAT 전시회 WCS 전용 지시", EquipTaskType.FREIGHT_MOVE),

    K_MAT_TSPG_CONVEYOR_INBOUND(
            "K_MAT_TSPG_CONVEYOR_INBOUND",
            "TSPG 4Way Shuttle Conveyor 입고",
            EquipTaskType.FREIGHT_MOVE
    ),

    K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND(
            "K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND",
            "TSPG Buffer → Conveyor 입고",
            EquipTaskType.FREIGHT_MOVE
    ),

    K_MAT_TSPG_CONVEYOR_OUTBOUND(
            "K_MAT_TSPG_CONVEYOR_OUTBOUND",
            "TSPG Conveyor 출고",
            EquipTaskType.FREIGHT_MOVE
    ),

    K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND(
            "K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND",
            "TSPG Conveyor → Buffer 출고",
            EquipTaskType.FREIGHT_MOVE
    ),

    /**
     * ================================
     * 일반 Command
     * ================================
     */
    FREIGHT_MOVE("FREIGHT_MOVE", "단순 화물 이동", EquipTaskType.FREIGHT_MOVE),

    SIMPLE_MOVE("SIMPLE_MOVE", "단순 이동", EquipTaskType.SIMPLE_MOVE),

    CANCEL_EMPTY_POINT_AMR_MOVE(
            "CANCEL_EMPTY_POINT_AMR_MOVE",
            "빈 포인트 이동",
            EquipTaskType.ONLY_TO_SIDE_MOVE
    );

    private final String code;
    private final String desc;
    private final EquipTaskType taskType;

    CommandType(String code, String desc, EquipTaskType taskType) {
        this.code = code;
        this.desc = desc;
        this.taskType = taskType;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }

    public String getCode() {
        return code;
    }

    public EquipTaskType getTaskTypeEnum() {
        return taskType;
    }

    /**
     * CommandType → TaskType 코드 반환
     *
     * @return EquipTaskType.code()
     */
    public String getTaskTypeCode() {
        if (taskType == null) {
            return null;
        }
        return taskType.getCode();
    }

    /**
     * 안전하게 TaskType 코드 반환 (없으면 예외)
     */
    public String resolveTaskTypeCode() {
        if (taskType == null) {
            throw new ElidomRuntimeException(
                    "지원하지 않는 CommandType 입니다. commandType=" + this.code
            );
        }
        return taskType.getCode();
    }

    /**
     * code → CommandType 변환 (null 허용)
     */
    public static CommandType fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(CommandType.class, code, false, null);
    }

    /**
     * code → CommandType 변환 (UNKNOWN fallback)
     */
    public static CommandType fromCode(Object code) {
        CommandType result = fromCodeOrNull(code);
        return result != null ? result : UNKNOWN;
    }
}