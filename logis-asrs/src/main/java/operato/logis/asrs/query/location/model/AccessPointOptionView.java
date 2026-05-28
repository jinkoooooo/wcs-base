package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * Access Point 선택 옵션 모델.
 */
@Data
public class AccessPointOptionView {

    private String id;
    private String areaCode;
    private String pointCode;
    private String pointName;
    private String activeYn;
}