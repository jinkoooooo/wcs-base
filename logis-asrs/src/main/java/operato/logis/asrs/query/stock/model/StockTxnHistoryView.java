package operato.logis.asrs.query.stock.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 재고 이력 조회 전용 View DTO.
 *
 * <p>
 * stock_txn + stock_unit + item + location 정보를 조인한 결과를 담는다.
 * 이력 흐름 확인, 참조문서 추적, 운영 디버깅 용도로 사용한다.
 * </p>
 */
@Getter
@Setter
@ToString
public class StockTxnHistoryView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 재고 트랜잭션 row id */
    private String stockTxnId;

    /** 트랜잭션 번호 */
    private String txnNo;

    /** 트랜잭션 유형 */
    private String txnType;

    /** 재고 단위 row id */
    private String stockUnitId;

    /** 재고 단위 번호 */
    private String stockUnitNo;

    /** 품목 row id */
    private String itemId;

    /** 품목 코드 */
    private String itemCode;

    /** 품목명 */
    private String itemName;

    /** LOT row id */
    private String lotId;

    /** LOT 번호 */
    private String lotNo;

    /** 출발 로케이션 row id */
    private String fromLocationId;

    /** 출발 로케이션 코드 */
    private String fromLocationCode;

    /** 도착 로케이션 row id */
    private String toLocationId;

    /** 도착 로케이션 코드 */
    private String toLocationCode;

    /** 처리 수량 */
    private Integer qty;

    /** 참조 문서 유형 */
    private String refDocType;

    /** 참조 문서 번호 */
    private String refDocNo;

    /** 참조 문서 라인 번호 */
    private String refLineNo;

    /** 사유 코드 */
    private String reasonCode;

    /** 비고 */
    private String remark;

    /** 처리 시각 */
    private Date txnAt;
}