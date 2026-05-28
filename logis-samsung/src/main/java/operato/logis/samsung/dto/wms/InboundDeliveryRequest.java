package operato.logis.samsung.dto.wms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Legacy-WCS 입고 컨테이너 요청 수신 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InboundDeliveryRequest {

    private String lcId; // 물류센터 코드
    private String lcNm; // (필수) 물류센터 명
    private String sourceSystem = "legacy"; // 발신 시스템
    private String regId; // 지시ID
    private LocalDateTime regTime; // 지시 생성 일시
    private String remark; // 비고
    private List<InboundDeliveryItemRequest> items; // 입고 품 목록
}
