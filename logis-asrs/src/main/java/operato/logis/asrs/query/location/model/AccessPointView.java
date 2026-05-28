package operato.logis.asrs.query.location.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 조회용 View DTO.
 */
@Getter
@Setter
@ToString
public class AccessPointView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** access point row id */
    private String accessPointId;

    /** 영역 id */
    private String areaId;

    /** 접근점 코드 */
    private String pointCode;

    /** 접근점 명 */
    private String pointName;

    /** 접근점 유형 */
    private String pointType;

    /** aisle 번호 */
    private Integer aisleNo;

    /** side 코드 */
    private String sideCode;

    /** bay 번호 */
    private Integer bayNo;

    /** level 번호 */
    private Integer levelNo;

    /** depth 번호 */
    private Integer depthNo;

    /** 목적 코드 */
    private String purposeCode;

    /** 목적 우선순위 */
    private Integer priorityNo;
}