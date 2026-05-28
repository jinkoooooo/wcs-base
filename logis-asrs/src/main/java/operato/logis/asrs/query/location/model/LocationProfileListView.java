package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 로케이션 프로필 목록/상세 조회 모델.
 */
@Data
public class LocationProfileListView {

    private String id;
    private String areaId;
    private String areaCode;
    private String areaName;

    private String profileCode;
    private String profileName;

    private Integer aisleStart;
    private Integer aisleEnd;
    private String sideCodes;
    private Integer bayStart;
    private Integer bayEnd;
    private Integer levelStart;
    private Integer levelEnd;
    private Integer depthStart;
    private Integer depthEnd;

    private String locationType;
    private String codePattern;

    private String mixedLoadYn;
    private String inboundAllowedYn;
    private String outboundAllowedYn;
    private String activeYn;

    private Long linkedLocationCount;
    private String createdAt;
    private String updatedAt;
}