package operato.logis.asrs.query.stock.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 재고 할당 조회 전용 View DTO.
 *
 * <p>
 * allocation, stock_unit, item, location 정보를 조인한 결과를 담는다.
 * 운영 화면, Postman 확인, 출고 전 할당 상태 검증 용도로 사용한다.
 * </p>
 */
@Getter
@Setter
@ToString
public class StockAllocationView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** allocation row id */
    private String allocationId;

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

    /** 현재 로케이션 row id */
    private String currentLocationId;

    /** 영역 코드 */
    private String areaCode;

    /** 현재 로케이션 코드 */
    private String locationCode;

    /** 할당 수량 */
    private Integer allocatedQty;

    /** 할당 상태 코드 */
    private String allocStatusCode;

    /** 참조 문서 유형 */
    private String refDocType;

    /** 참조 문서 번호 */
    private String refDocNo;

    /** 참조 문서 라인 번호 */
    private String refLineNo;

    /** 납기일 */
    private Date dueDate;

    /** 할당 시각 */
    private Date allocatedAt;
}