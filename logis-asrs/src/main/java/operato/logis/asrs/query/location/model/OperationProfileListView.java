package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 오퍼레이션 프로필 목록/상세 조회 모델.
 */
@Data
public class OperationProfileListView {

    private String id;
    private String profileCode;
    private String profileName;
    private String industryType;
    private String description;
    private String activeYn;
    private Long linkedAreaCount;
    private String createdAt;
    private String updatedAt;
}