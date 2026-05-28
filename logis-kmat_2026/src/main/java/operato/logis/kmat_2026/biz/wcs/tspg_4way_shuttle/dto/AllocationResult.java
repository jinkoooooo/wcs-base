package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Builder
public class AllocationResult {

    private boolean success;

    @Setter // Manager에서 실제 락 걸린 코드로 업데이트하기 위해 추가
    private String fromLocCode;

    @Setter // Manager에서 실제 락 걸린 코드로 업데이트하기 위해 추가
    private String toLocCode;

    private String eqGroupId;

    @Setter
    private String inventoryId;

    /** * 추천된 후보 로케이션 리스트
     * - Allocator가 순위를 매겨 전달하고, Manager가 이 리스트를 들고 DB 락을 시도함
     */
    @Setter
    private List<String> candidateLocs;

    private String errorCode;
    private String errorDesc;

    public static AllocationResult success(String fromLocCode, String toLocCode, String eqGroupId) {
        return AllocationResult.builder()
                .success(true)
                .fromLocCode(fromLocCode)
                .toLocCode(toLocCode)
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

    /** 실제 할당된 코드를 세팅하기 위한 편의 메서드 */
    public void setLocCode(String locCode) {
        // 입고 시에는 목적지(to), 출고 시에는 출발지(from)가 랙 로케이션임
        this.toLocCode = locCode;
    }
}