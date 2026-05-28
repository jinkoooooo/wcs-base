package operato.logis.ecs.tspg4way.dashboard.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ECS 작업 상태 DTO
 * WebSocket으로 브로드캐스트되는 실시간 작업 데이터
 *
 * [작업 계층 구조]
 * 1. WCS Level: tb_wcs_shuttle_order (상위 오더)
 * 2. ECS_RACK Level: tb_ecs_rack_order (셔틀 PICK/DROP 단위)
 * 3. ECS_ROUTE Level: tb_ecs_route_order (리프터/CV 이동)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusDto {

    /** 작업 키 */
    private String jobKey;

    /** 상위 WCS 오더 키 (ECS 레벨인 경우) */
    private String parentJobKey;

    /**
     * 작업 레벨
     * - WCS
     * - ECS_RACK
     * - ECS_ROUTE
     */
    private String jobLevel;

    /**
     * 작업 타입
     * 11: INBOUND
     * 12: OUTBOUND
     * 13: MOVE
     * 21: CHARGE
     */
    private Integer jobType;

    /**
     * 프론트 표시용 작업 상태 문자열
     * - PENDING
     * - ASSIGNED
     * - RUNNING
     * - PAUSED
     * - COMPLETED
     * - CANCELLED
     * - FAILED
     * - UNKNOWN
     */
    private String status;

    /** 출발지 로케이션 코드 */
    private String fromLoc;

    /** 목적지 로케이션 코드 */
    private String toLoc;

    /** 우선순위 (낮을수록 높음) */
    private Integer priority;

    /** 바코드 */
    private String barcode;

    /** 할당된 설비 ID (셔틀/리프터/CV) */
    private String assignedEquipmentId;

    /** 할당된 설비 타입 (SHUTTLE/LIFTER/CONVEYOR/RACK/UNKNOWN) */
    private String assignedEquipmentType;

    /** 할당된 셔틀 ID (deprecated, use assignedEquipmentId) */
    @Deprecated
    private String assignedShuttleId;

    /** 설비 그룹 ID */
    private String eqGroupId;

    /** PLC 명령 ID */
    private String plcCmdId;

    /** PLC 명령 상태 */
    private Integer cmdStatus;

    /** 생성 시간 */
    private Long createdAt;

    /** 시작 시간 */
    private Long startedAt;

    /** 완료 시간 */
    private Long completedAt;

    /** 에러 코드 */
    private String errorCode;

    /** 에러 메시지 */
    private String errorMessage;

    /** 브로드캐스트 시각 */
    private Long ts;

    /**
     * 작업 타입 상수
     */
    public static class JobType {
        public static final int INBOUND = 11;
        public static final int OUTBOUND = 12;
        public static final int MOVE = 13;
        public static final int CHARGE = 21;

        private JobType() {
        }
    }

    /**
     * 프론트 표시용 작업 상태 상수
     */
    public static class JobStatus {
        public static final String CREATED = "CREATED";
        public static final String PENDING = "PENDING";
        public static final String ASSIGNED = "ASSIGNED";
        public static final String RUNNING = "RUNNING";
        public static final String AWAITING_SCAN = "AWAITING_SCAN";
        public static final String PAUSED = "PAUSED";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";
        public static final String FAILED = "FAILED";
        public static final String UNKNOWN = "UNKNOWN";

        private JobStatus() {
        }
    }

    /**
     * 작업 레벨 상수
     */
    public static class JobLevel {
        public static final String WCS = "WCS";
        public static final String ECS_RACK = "ECS_RACK";
        public static final String ECS_ROUTE = "ECS_ROUTE";

        private JobLevel() {
        }
    }

    /**
     * 진행 중인 작업인지
     */
    public boolean isActive() {
        return JobStatus.CREATED.equals(status)
                || JobStatus.PENDING.equals(status)
                || JobStatus.ASSIGNED.equals(status)
                || JobStatus.RUNNING.equals(status)
                || JobStatus.AWAITING_SCAN.equals(status)
                || JobStatus.PAUSED.equals(status);
    }

    /**
     * 완료된 작업인지
     */
    public boolean isCompleted() {
        return JobStatus.COMPLETED.equals(status);
    }

    /**
     * 에러 상태인지
     */
    public boolean isError() {
        return JobStatus.FAILED.equals(status);
    }

    /**
     * 취소된 작업인지
     */
    public boolean isCancelled() {
        return JobStatus.CANCELLED.equals(status);
    }

    /**
     * 작업 타입 문자열 반환
     */
    public String getJobTypeName() {
        if (jobType == null) return "UNKNOWN";
        switch (jobType) {
            case JobType.INBOUND:
                return "INBOUND";
            case JobType.OUTBOUND:
                return "OUTBOUND";
            case JobType.MOVE:
                return "MOVE";
            case JobType.CHARGE:
                return "CHARGE";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 상태 문자열 반환
     */
    public String getStatusName() {
        return status == null ? JobStatus.UNKNOWN : status;
    }
}