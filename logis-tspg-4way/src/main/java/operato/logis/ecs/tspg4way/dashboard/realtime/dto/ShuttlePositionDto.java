package operato.logis.ecs.tspg4way.dashboard.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 셔틀 위치/상태 DTO
 * WebSocket으로 브로드캐스트되는 실시간 셔틀 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShuttlePositionDto {

    /** 셔틀 고유 ID (tb_eq_car_mst.id) */
    private String equipmentId;

    /** 설비 코드 (tb_eq_car_mst.eq_id) */
    private String equipmentCode;

    /** 현재 위치한 셀 ID */
    private String cellId;

    /** 레이아웃 X 좌표 (픽셀) */
    private Double posX;

    /** 레이아웃 Y 좌표 (픽셀) */
    private Double posY;

    /** 층 (floor/level) */
    private Integer floor;

    /**
     * 셔틀 상태 (EcsDBConsts.EqCarStatus)
     * 0: READY (대기)
     * 1: RESERVE (예약)
     * 2: RUN (이동중)
     * 5: EMR_STOP (비상정지)
     * 8: ERROR (에러)
     * 9: COMPLETE (완료)
     */
    private Integer status;

    /** 상태 설명 (한글) */
    private String statusDesc;

    /** 배터리 잔량 (0-100%) - batteryStatus 기반 추정값 */
    private Integer batteryLevel;

    /**
     * 배터리 상태 (EcsDBConsts.EqCarBatteryStatus)
     * 0: CAN_MOVE (작업가능)
     * 1: NEED_CHARGE (충전필요)
     * 2: CHARGING (충전중)
     * 9: COMPLETE_CHARGE (충전완료)
     */
    private Integer batteryStatus;

    /** 화물 적재 여부 */
    private Boolean hasCargo;

    /** 자동 모드 여부 (tb_eq_car_mst.auto_yn) — legend-spec 분기용 원시값 */
    private Boolean autoYn;

    /** 사용 여부 (tb_eq_car_mst.use_yn) — DISABLED 판정 원시값 */
    private Boolean useYn;

    /**
     * 정규화된 셔틀 상태 (EquipmentStateClassifier.ShuttleState.name()).
     * 값: DISABLED / ERROR / MANUAL / CHARGING / RUNNING / IDLE.
     * 프런트는 반드시 이 필드로만 색/라벨 분기할 것.
     */
    private String shuttleState;

    /** 에러 코드 (에러 시) */
    private String errorCode;

    /** 에러 메시지 */
    private String errorMessage;

    /** 현재 작업 키 (작업 중일 때) */
    private String currentJobKey;

    /** Row 위치 */
    private Integer row;

    /** Bay 위치 */
    private Integer bay;

    /** 이동 상태 (0: 정지, 1: 이동중) */
    private Integer movementStatus;

    /** 목적지 셀 ID (이동 중일 때) */
    private String targetCellId;

    // 현재 작업 정보 (tb_ecs_rack_order)

    /** 현재 진행 중인 작업 여부 */
    private Boolean hasActiveJob;

    /** 현재 작업 키 */
    private String currentOrderKey;

    /** 현재 작업 타입 (11:입고, 12:출고, 21:충전, 22:이동) */
    private Integer currentOrderType;

    /** 현재 작업 상태 (0:대기, 1:전송, 2:작업중, 9:완료) */
    private Integer currentOrderStatus;

    /** 현재 작업 바코드 */
    private String currentBarcode;

    /** 현재 작업 출발지 */
    private String currentFromLoc;

    /** 현재 작업 목적지 */
    private String currentToLoc;

    /** 타임스탬프 (멱등성 체크용) */
    private Long ts;
}
