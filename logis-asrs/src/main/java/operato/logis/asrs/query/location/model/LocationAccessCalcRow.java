package operato.logis.asrs.query.location.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 로케이션 접근성 계산용 내부 DTO.
 */
@Getter
@Setter
@ToString
public class LocationAccessCalcRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** location row id */
    private String locationId;

    /** 로케이션 코드 */
    private String locationCode;

    /** 영역 id */
    private String areaId;

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

    /** 활성 여부 */
    private String activeYn;

    /** 계산된 전면 여부 */
    private String frontPriorityYn;

    /** 계산된 접근 점수 */
    private Integer accessScore;

    /** 계산된 sort 순번 */
    private Integer newSortSeq;

    /** 계산된 로케이션 등급 */
    private String newLocationGrade;

    /** 대표 access point id */
    private String primaryAccessPointId;

    /** 대표 access point code */
    private String primaryAccessPointCode;
}