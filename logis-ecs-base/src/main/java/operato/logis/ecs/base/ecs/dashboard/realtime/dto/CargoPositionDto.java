package operato.logis.ecs.base.ecs.dashboard.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 화물 위치 DTO
 * WebSocket으로 브로드캐스트되는 실시간 화물 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoPositionDto {

    /** 화물 고유 ID */
    private String cargoId;

    /** 바코드 */
    private String barcode;

    /** 레이아웃 X 좌표 (픽셀) */
    private Double posX;

    /** 레이아웃 Y 좌표 (픽셀) */
    private Double posY;

    /**
     * 화물 상태
     *
     * @see CargoStatus
     */
    private Integer status;

    /** 운반 중인 셔틀 ID (셔틀 위에 있을 때) */
    private String carriedByShuttleId;

    /** 현재 위치한 셀 ID (랙에 저장된 경우) */
    private String cellId;

    /** 현재 위치한 컨베이어 ID (컨베이어 위에 있을 때) */
    private String conveyorId;

    /** SKU 코드 */
    private String skuCode;

    /** 품목명 */
    private String itemName;

    /** 수량 */
    private Integer qty;

    /** 층 */
    private Integer floor;

    /** 타임스탬프 */
    private Long ts;

    /**
     * 화물 상태 상수
     */
    public static class CargoStatus {
        /** 셔틀이 운반 중 */
        public static final int MOVING = 1;
        /** 랙 셀에 저장됨 */
        public static final int STORED = 2;
        /** 컨베이어 위에 있음 */
        public static final int ON_CONVEYOR = 3;
        /** 대기 중 */
        public static final int WAITING = 0;
    }

    /**
     * 셔틀에 의해 운반 중인지
     */
    public boolean isCarried() {
        return carriedByShuttleId != null && !carriedByShuttleId.isEmpty();
    }

    /**
     * 랙에 저장되어 있는지
     */
    public boolean isStored() {
        return status != null && status == CargoStatus.STORED;
    }
}
