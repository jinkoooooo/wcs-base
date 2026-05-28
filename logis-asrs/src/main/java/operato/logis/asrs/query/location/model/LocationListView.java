package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 로케이션 목록/상세 조회 모델.
 */
@Data
public class LocationListView {

    private String id;

    private String areaId;
    private String areaCode;
    private String areaName;

    private String locationCode;
    private Integer aisleNo;
    private String sideCode;
    private Integer bayNo;
    private Integer levelNo;
    private Integer depthNo;

    private String locationType;
    private String usageStatusCode;

    private String inboundAllowedYn;
    private String outboundAllowedYn;
    private String mixedLoadYn;
    private String frontPriorityYn;

    private String dedicatedItemCategoryId;
    private String dedicatedItemCategoryCode;
    private String dedicatedItemCategoryName;

    private Integer maxWeightG;
    private Integer maxVolumeMm3;

    private Integer sortSeq;
    private String activeYn;

    private String locationGrade;
    private Integer accessScore;

    private String primaryAccessPointId;
    private String primaryAccessPointCode;
    private String primaryAccessPointName;

    private String createdAt;
    private String updatedAt;
}