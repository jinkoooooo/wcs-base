package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 센터 선택 옵션 조회 모델.
 */
@Data
public class CenterOptionView {

    private String id;
    private String centerCode;
    private String centerName;
    private String activeYn;
}