package operato.logis.ecs.tspg4way.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 대시보드 팝업 제어 정보 응답 DTO. 팝업 오픈 시 1회 호출로 설비의 모든 제어 상태를 반환한다.
 * locId/eqGroupId/locXxx 는 RACK 타입에서만, eqUseYn 은 CONVEYOR/SHUTTLE 에서만 유효.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardControlInfo {

    // WCS 로케이션 정보 (RACK 타입 전용)

    /** WCS 로케이션 코드 (tb_wcs_loc_mst.loc_id) */
    @JsonProperty("locId")
    private String locId;

    /** 설비 그룹 ID */
    @JsonProperty("eqGroupId")
    private String eqGroupId;

    /**
     * 로케이션 사용 여부 (tb_wcs_loc_mst.use_yn)
     * 0 = 사용 안 함(비가동), 1 = 사용
     */
    @JsonProperty("locUseYn")
    private Integer locUseYn;

    /**
     * 로케이션 수동 잠금 여부 (tb_wcs_loc_mst.lock_yn)
     * 0 = 잠금 해제, 1 = 잠금
     */
    @JsonProperty("locLockYn")
    private Integer locLockYn;

    /**
     * 잠금 주체 (tb_wcs_loc_mst.lock_by)
     * 예: "MANUAL" (수동 잠금), "ORDER-2024-001" (오더 선점)
     */
    @JsonProperty("locLockBy")
    private String locLockBy;

    /**
     * 로케이션 상태 (tb_wcs_loc_mst.status)
     * 0 = EMPTY, 10 = OCCUPIED, 20 = LOCKED(작업중), 90 = DISABLED(사용불가)
     */
    @JsonProperty("locStatus")
    private Integer locStatus;

    // 설비 마스터 정보 (CONVEYOR / SHUTTLE 전용)

    /**
     * 설비 마스터 사용 여부 (tb_eq_car_mst.use_yn 또는 tb_eq_cv_mst.use_yn)
     * true = 사용, false = 비가동
     * RACK 타입이면 null
     */
    @JsonProperty("eqUseYn")
    private Boolean eqUseYn;

    // 현재 활성 WCS 오더 (RACK 타입 전용)

    /**
     * 현재 이 로케이션을 점유 중인 WCS 오더 키
     * null이면 활성 오더 없음 — Resume/ForceComplete/Cancel 버튼 비표시
     */
    @JsonProperty("activeOrderKey")
    private String activeOrderKey;

    /**
     * 현재 활성 오더 상태 코드 (ShuttleOrderStatus)
     * UI에서 버튼 활성화 조건 판단용:
     * - SENT(10) 이상 COMPLETED(90) 미만 → ForceComplete/Cancel 가능
     * - ERROR(100+) → Resume 가능
     */
    @JsonProperty("activeOrderStatus")
    private Integer activeOrderStatus;

    /** 현재 활성 오더 타입 (INBOUND=입고, OUTBOUND=출고, MOVE=이동 등) */
    @JsonProperty("activeOrderType")
    private String activeOrderType;

    // 포트 락 holder 정보 (포트 류 로케이션 전용)

    /**
     * 포트 락 holder — tb_inventory_location.task_id raw 값.
     * "DISPATCH_LOCK" sentinel (parent host key 없는 dispatch lock) 또는 셔틀 오더 키.
     * null 이면 포트 락 없음. 강제 해제 요청 시 감사용으로 함께 보낸다(화면 미노출).
     */
    @JsonProperty("portLockTaskId")
    private String portLockTaskId;

    /**
     * 포트 락 holder 셔틀 오더의 barcode (tb_wcs_shuttle_order.barcode).
     * DISPATCH_LOCK sentinel 인 경우 null.
     */
    @JsonProperty("portLockBarcode")
    private String portLockBarcode;

    /**
     * 포트 락 holder 셔틀 오더의 order_status (ShuttleOrderStatus 코드).
     * DISPATCH_LOCK sentinel 인 경우 null.
     */
    @JsonProperty("portLockOrderStatus")
    private Integer portLockOrderStatus;

    // 재고 정보 (RACK 타입 전용)

    /**
     * 해당 로케이션의 현재 WCS 재고 목록 (tb_wcs_inventory)
     * Empty Pick / Double Entry 판단에 활용
     */
    @JsonProperty("inventory")
    private List<InventoryItem> inventory;

    // 내부 재고 아이템 DTO

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private String id;

        @JsonProperty("skuCode")
        private String skuCode;

        @JsonProperty("palletId")
        private String palletId;  // LPN

        private int qty;

        @JsonProperty("allocQty")
        private int allocQty;

        /** 재고 상태: 1=정상, 2=보류, 9=불량, 99=Mismatch */
        @JsonProperty("stockStatus")
        private int stockStatus;

        /** 재고 카테고리 (stock_type) — NORMAL/QC_PENDING/QC_FAIL/NIA_PENDING/RETURN/DISPOSAL */
        @JsonProperty("stockType")
        private String stockType;

        /** stock 행의 stock_id (Dashboard2D 셀 액션에서 키로 사용) */
        @JsonProperty("stockId")
        private String stockId;

        // Stock 상세 (tb_inventory_stock)

        /** 상품 코드 (tb_inventory_stock.item_code) */
        @JsonProperty("itemCode")
        private String itemCode;

        /** Lot 번호 */
        @JsonProperty("lotNo")
        private String lotNo;

        /** 화주 */
        @JsonProperty("itemOwner")
        private String itemOwner;

        /** 입고 일시 — "YYYY-MM-DD HH:MI:SS" */
        @JsonProperty("inbDatetime")
        private String inbDatetime;

        /** 유통기한 — "YYYY-MM-DD" */
        @JsonProperty("expiredDate")
        private String expiredDate;

        /** 제조일 — "YYYY-MM-DD" */
        @JsonProperty("produceDate")
        private String produceDate;

        /** 우선순위 */
        @JsonProperty("itemPriority")
        private Integer itemPriority;

        /** 재고 높이 */
        @JsonProperty("stockHeight")
        private Integer stockHeight;

        /** attribute_a (자유 속성 필드) */
        @JsonProperty("attributeA")
        private String attributeA;

        // Location 메타 (tb_inventory_location)

        /** 로케이션 타입 (RACK / INBOUND_PORT / OUTBOUND_PORT / IN_OUTBOUND_PORT 등) */
        @JsonProperty("locType")
        private String locType;

        /** 포트 운영 모드 (겸용 포트 전용) */
        @JsonProperty("portMode")
        private String portMode;

        /** BCR 스캔 바코드 */
        @JsonProperty("scannedBarcode")
        private String scannedBarcode;
    }
}
