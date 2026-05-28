package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재고 변경계 커맨드 처리 결과 DTO.
 *
 * <p>
 * 입고 / 적치 / 이동 등 재고 상태 변경 처리 후
 * 프론트나 호출 시스템이 확인할 수 있는 핵심 결과값을 응답한다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCommandResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 처리된 재고 단위 ID */
    private String stockUnitId;

    /** 재고 단위 번호 */
    private String stockUnitNo;

    /** 생성된 트랜잭션 번호 */
    private String txnNo;

    /** 트랜잭션 유형 */
    private String txnType;

    /** 출발 로케이션 ID */
    private String fromLocationId;

    /** 도착 로케이션 ID */
    private String toLocationId;

    /** 처리 수량 */
    private Integer qty;

    /** 처리 후 재고 상태 코드 */
    private String stockStatusCode;

    /** 결과 메시지 */
    private String message;
}