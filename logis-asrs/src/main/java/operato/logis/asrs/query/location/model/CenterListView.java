package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 센터 목록/상세 조회 모델.
 */
@Data
public class CenterListView {

    private String id;
    private String centerCode;
    private String centerName;
    private String centerType;
    private String timezone;
    private String description;
    private String activeYn;
    private Long linkedAreaCount;
    private String createdAt;
    private String updatedAt;
}