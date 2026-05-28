package operato.logis.ecs.base.ecs.dashboard.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 알람/에러 DTO
 * WebSocket으로 브로드캐스트되는 실시간 알람 데이터
 *
 * [알람 타입]
 * - EQUIPMENT: 설비 에러 (셔틀, 컨베이어, 리프터)
 * - JOB_ERROR: 작업 에러 (ECS 작업 실패)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDto {

    /** 알람 고유 ID */
    private String alarmId;

    /**
     * 알람 타입
     * - EQUIPMENT: 설비 에러
     * - JOB_ERROR: 작업 에러
     */
    private String alarmType;

    /**
     * 장비 타입
     * SHUTTLE, CONVEYOR, LIFTER, RACK
     */
    private String equipmentType;

    /** 장비 ID */
    private String equipmentId;

    /** 장비 코드 */
    private String equipmentCode;

    // ============================================
    // 작업 관련 필드 (JOB_ERROR인 경우)
    // ============================================

    /** 작업 키 (JOB_ERROR인 경우) */
    private String orderKey;

    /** 작업 타입 (11:입고, 12:출고, 21:충전, 22:이동) */
    private Integer orderType;

    /** 작업 바코드 */
    private String barcode;

    // ============================================
    // 에러 정보
    // ============================================

    /** 에러 코드 */
    private String errorCode;

    /** 에러 메시지 */
    private String errorMessage;

    /**
     * 심각도
     *
     * @see AlarmSeverity
     */
    private Integer severity;

    /** 발생 시간 */
    private Long occurredAt;

    /** 확인 여부 */
    private Boolean acknowledged;

    /** 확인 시간 */
    private Long acknowledgedAt;

    /** 확인자 */
    private String acknowledgedBy;

    /** 타임스탬프 */
    private Long ts;

    /** 알람 심각도 상수 - 정보, 경고, 에러, 심각 */
    public static class AlarmSeverity {
        public static final int INFO = 0;
        public static final int WARNING = 1;
        public static final int ERROR = 2;
        public static final int CRITICAL = 3;
    }

    /** 알람 타입 상수 */
    public static class AlarmType {
        public static final String EQUIPMENT = "EQUIPMENT";
        public static final String JOB_ERROR = "JOB_ERROR";
    }

    /** 심각도 문자열 반환 */
    public String getSeverityName() {
        if (severity == null) return "UNKNOWN";
        switch (severity) {
            case AlarmSeverity.INFO:
                return "INFO";
            case AlarmSeverity.WARNING:
                return "WARNING";
            case AlarmSeverity.ERROR:
                return "ERROR";
            case AlarmSeverity.CRITICAL:
                return "CRITICAL";
            default:
                return "UNKNOWN";
        }
    }

    /** 에러 수준 이상인지 */
    public boolean isError() {
        return severity != null && severity >= AlarmSeverity.ERROR;
    }

    /** 작업 에러인지 */
    public boolean isJobError() {
        return AlarmType.JOB_ERROR.equals(alarmType);
    }

    /** 설비 에러인지 */
    public boolean isEquipmentError() {
        return AlarmType.EQUIPMENT.equals(alarmType);
    }
}
