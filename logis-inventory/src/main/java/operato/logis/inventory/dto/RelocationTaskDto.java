package operato.logis.inventory.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RelocationTaskDto {

    // 실행 순서 (1부터 시작, 낮을수록 먼저 실행)
    private Integer stepOrder;

    // 이동시킬 재고 ID
    private String stockId;

    // 출발 로케이션
    private String fromLocCode;

    // 도착 로케이션
    private String toLocCode;

    // 결과
    private Integer taskType;
}