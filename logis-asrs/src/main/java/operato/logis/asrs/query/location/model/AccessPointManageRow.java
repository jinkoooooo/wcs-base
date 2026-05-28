package operato.logis.asrs.query.location.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 관리 조회 모델.
 */
@Getter
@Setter
@ToString
public class AccessPointManageRow {

    private String id;

    private String areaId;
    private String areaCode;
    private String areaName;

    private String pointCode;
    private String pointName;
    private String pointType;

    private Integer aisleNo;
    private String sideCode;
    private Integer bayNo;
    private Integer levelNo;
    private Integer depthNo;

    private String useForSortYn;
    private String activeYn;
    private String description;

    /** 활성 목적 코드 문자열 */
    private String purposeCodes;

    private String createdAt;
    private String updatedAt;
}