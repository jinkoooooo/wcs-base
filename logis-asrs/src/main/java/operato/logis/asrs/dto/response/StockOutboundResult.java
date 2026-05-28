package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재고 출고 처리 결과 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOutboundResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 재고 단위 id */
    private String stockUnitId;

    /** 재고 단위 번호 */
    private String stockUnitNo;

    /** 트랜잭션 번호 */
    private String txnNo;

    /** 트랜잭션 유형 */
    private String txnType;

    /** 출고 수량 */
    private Integer outboundQty;

    /** 출고 후 잔량 */
    private Integer remainingQty;

    /** 출고 후 예약수량 */
    private Integer reservedQty;

    /** 출고 후 재고 상태 */
    private String stockStatusCode;

    /** 참조 문서 번호 */
    private String refDocNo;

    /** 결과 메시지 */
    private String message;
}