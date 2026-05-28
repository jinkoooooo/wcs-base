package operato.logis.ecs.base.ecs.dashboard.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 컨베이어 상태 DTO
 * WebSocket으로 브로드캐스트되는 실시간 컨베이어 데이터
 *
 * [ECS 연동]
 * - plcCmdId: 현재 PLC 명령 ID
 * - 현재 작업 정보: tb_ecs_route_order 기반
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConveyorStatusDto {

    /** 컨베이어 고유 ID (tb_eq_cv_mst.id) */
    private String equipmentId;

    /** 설비 코드 (tb_eq_cv_mst.eq_id) */
    private String eqId;

    /**
     * 컨베이어 타입 (EcsDBConsts.ConveyorType)
     * 1: GROUND (지상컨베이어)
     * 2: INBOUND (입고대컨베이어)
     * 3: OUTBOUND (출고대컨베이어)
     * 4: IN_OUTBOUND (입출고대컨베이어)
     * 11: LIFT (리프트컨베이어)
     * 12: RACK_IN (랙단 컨베이어)
     */
    private Integer type;

    /** 타입 설명 (한글) */
    private String typeDesc;

    /** 레이아웃 X 좌표 (픽셀) */
    private Double posX;

    /** 레이아웃 Y 좌표 (픽셀) */
    private Double posY;

    /**
     * 컨베이어 상태 (EcsDBConsts.EqConveyorStatus)
     * 0: READY (대기)
     * 1: MOVE_RESERVE (주행 예약)
     */
    private Integer status;

    /** 상태 설명 (한글) */
    private String statusDesc;

    /** 화물 감지 여부 (물리 센서 — tb_eq_cv_mst.cargo_yn) */
    private Boolean hasCargo;

    /** 구동 여부 */
    private Boolean moving;

    /** 자동 모드 여부 (tb_eq_cv_mst.auto_yn) — legend-spec 분기용 원시값 */
    private Boolean autoYn;

    /** 사용 여부 (tb_eq_cv_mst.use_yn) — DISABLED 판정 원시값 */
    private Boolean useYn;

    /**
     * 정규화된 컨베이어 상태 (EquipmentStateClassifier.ConveyorState.name()).
     * 값: DISABLED / ERROR / MANUAL / AUTO.
     * 프런트는 반드시 이 필드로만 색/라벨 분기할 것.
     */
    private String conveyorState;

    /**
     * Conveyor Pallet 3-way 상태 (EquipmentStateClassifier.PalletState.name()).
     * 값: DATA_AND_PHY / PHY_ONLY / DATA_ONLY / null(=pallet 미렌더).
     * has_data(= tb_ecs_route_order 진행중 EXISTS) 와 has_phy(= cargo_yn) 조합으로 결정.
     */
    private String palletState;

    /** PLC 명령 ID */
    private Integer plcCmdId;

    /** 에러 코드 */
    private String errorCode;

    /** 에러 메시지 */
    private String errorMessage;

    /** 층 (level) */
    private Integer level;

    // ============================================
    // 현재 작업 정보 (tb_ecs_route_order)
    // ============================================

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

    /** 타임스탬프 */
    private Long ts;
}

