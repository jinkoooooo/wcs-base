package operato.logis.wcs.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.dto.RelocationTaskDto;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 출고/이동/입고 할당 결과 (내부 DTO).
 * 로케이션 단위 할당(LocationAllocation)과 SKU/LOT 라인(Item)을 함께 담는다.
 */
@Getter
@Builder
public class AllocationResult {

    private boolean success;

    @Setter
    private String fromLocId;

    @Setter
    private String toLocId;

    private String eqGroupId;

    @Setter
    private String stockId;

    /** 단일 로케이션 방해물 (하위호환) */
    @Setter
    private List<RelocationTaskDto> obstacleMoves;

    @Setter
    private List<String> candidateLocs;

    private String errorCode;
    private String errorDesc;

    /** 로케이션별 할당 결과 (다중 파렛트 출고 시) */
    @Setter
    private List<LocationAllocation> locationAllocations;

    /** 다중 오더 묶음 식별자 */
    @Setter
    private String groupOrderKey;

    /** single 모드 partial-pick (LocationAllocation 와 동일 의미) */
    @Setter
    private List<Item> remainingStocks;

    public boolean isPartialPicking() {
        return ValueUtil.isNotEmpty(remainingStocks);
    }

    public static AllocationResult success(String fromLocId, String toLocId, String eqGroupId) {
        return AllocationResult.builder()
                .success(true)
                .fromLocId(fromLocId)
                .toLocId(toLocId)
                .eqGroupId(eqGroupId)
                .build();
    }

    public static AllocationResult fail(String errorCode, String errorDesc) {
        return AllocationResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorDesc(errorDesc)
                .build();
    }

    /**
     * 다중 로케이션 할당 성공 팩토리. 하위호환: 첫 allocation 의 from/to 를 기존 필드에도 세팅.
     */
    public static AllocationResult successMulti(List<LocationAllocation> allocations, String eqGroupId) {
        AllocationResultBuilder b = AllocationResult.builder()
                .success(true)
                .locationAllocations(allocations)
                .eqGroupId(eqGroupId);
        if (ValueUtil.isNotEmpty(allocations)) {
            b.fromLocId(allocations.get(0).getFromLocId())
             .toLocId(allocations.get(0).getToLocId());
        }
        return b.build();
    }

    public boolean isMultiLocation() {
        return ValueUtil.isNotEmpty(locationAllocations) && locationAllocations.size() > 1;
    }

    public void addObstacleMove(RelocationTaskDto move) {
        if (ValueUtil.isEmpty(obstacleMoves)) obstacleMoves = new ArrayList<>();
        obstacleMoves.add(move);
    }

    /** 출고 할당 결과 — 로케이션 단위. 다중 파렛트 출고 시 로케이션별 1개. */
    @Getter
    @Setter
    public static class LocationAllocation {
        private String fromLocId;
        private String toLocId;
        private String eqGroupId;
        private List<Item> items = new ArrayList<>();
        private List<RelocationTaskDto> obstacleMoves = new ArrayList<>();
        private boolean hasObstacles;

        /** 출고 후 잔여 재고 존재 여부. false 면 전량 출고. */
        private boolean partialPicking;
        /** 잔여 수량 합계(EA). */
        private Integer remainingQty;
        /** 잔여 재고 스냅샷. */
        private List<Item> remainingStocks = new ArrayList<>();
    }

    /** 할당 단위 — 개별 SKU/LOT 라인. */
    @Getter
    @Setter
    public static class Item {
        private String itemCode;
        private String lotNo;
        private int qty;
    }
}
