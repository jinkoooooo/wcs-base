package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 아레아 목록/상세 조회 모델.
 */
@Data
public class StorageAreaListView {

    private String id;

    private String centerId;
    private String centerCode;
    private String centerName;

    private String areaCode;
    private String areaName;
    private String areaType;

    private String operationProfileId;
    private String operationProfileCode;
    private String operationProfileName;

    private String description;
    private String activeYn;

    private Long linkedLocationProfileCount;
    private Long linkedLocationCount;

    private String createdAt;
    private String updatedAt;
}