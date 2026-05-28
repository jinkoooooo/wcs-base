package operato.logis.samsung.dto.wms;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Legacy-WCS 입고 컨테이너 요청 수신 상세 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InboundDeliveryItemRequest {

    private String lcId; // 물류센터 코드
    private String lcNm; // (필수) 물류센터 명
    private String sourceSystem = "legacy"; // 발신 시스템
    private String regId; // 지시 ID
    private LocalDateTime regTime; // 지시 생성 일시

    private String blNo;       // 입고 주문 번호
    private String cntrNo;     // 컨테이너 번호
    private String itemType;   // 제품 유형 (SBS, BMF 등)
    private String itemDesc; // 입고품 설명
    private String itemCode; // 입고품 코드
    private Integer itemQty; // 입고 예정 수량
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date inboundDate;  // 입고 예정일
}