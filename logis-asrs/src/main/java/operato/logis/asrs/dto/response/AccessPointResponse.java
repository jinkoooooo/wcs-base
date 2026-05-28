package operato.logis.asrs.dto.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 응답 DTO.
 */
@Getter
@Setter
@ToString
public class AccessPointResponse implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private String purposeCodes;

    private String action;
    private String message;

    private List<AccessPointPurposeResponse> purposes = new ArrayList<AccessPointPurposeResponse>();
}