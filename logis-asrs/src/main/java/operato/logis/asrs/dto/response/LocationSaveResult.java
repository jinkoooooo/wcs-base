package operato.logis.asrs.dto.response;

import lombok.Data;

/**
 * 로케이션 저장/삭제 결과 DTO.
 */
@Data
public class LocationSaveResult {

    private String id;
    private String areaCode;
    private String locationCode;
    private String action;
    private String message;

    /** 함께 삭제된 현재고 건수 */
    private Integer deletedStockCount;

    /** 함께 삭제된 로케이션 건수(일괄삭제용) */
    private Integer deletedLocationCount;
}