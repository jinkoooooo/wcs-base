package operato.logis.asrs.query.stock.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 현재고 조회 전용 View DTO.
 *
 * <p>
 * stock_unit, item, lot, location 정보를 조인한 결과를 담는다.
 * 운영 화면 / Postman 조회 / 디버깅 확인용으로 사용한다.
 * </p>
 */
@Getter
@Setter
@ToString
public class CurrentStockView implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** 현재 로케이션 row id */
    private String currentLocationId;

    /** 영역 코드 */
    private String areaCode;

    /** 현재 로케이션 코드 */
    private String locationCode;

    /** 재고 단위 유형 */
    private String stockUnitType;

    /** 총 수량 */
    private Integer qty;

    /** 예약 수량 */
    private Integer reservedQty;

    /** 재고 상태 코드 */
    private String stockStatusCode;

    /** 보류 여부 */
    private String holdYn;

    /** 최초 입고 시각 */
    private Date inboundAt;

    /** 최종 이동 시각 */
    private Date lastMovedAt;

    /** 활성 여부 */
    private String activeYn;
}