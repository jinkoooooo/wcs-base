package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 로케이션 접근성 미리보기 행 DTO.
 */
@Getter
@Setter
@ToString
public class LocationAccessPreviewRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** location row id */
    private String locationId;

    /** 로케이션 코드 */
    private String locationCode;

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

    /** 재계산된 전면 여부 */
    private String frontPriorityYn;

    /** 재계산된 접근성 점수 */
    private Integer accessScore;

    /** 재계산된 sort 순번 */
    private Integer newSortSeq;

    /** 재계산된 로케이션 등급 */
    private String newLocationGrade;

    /** 대표 access point id */
    private String primaryAccessPointId;

    /** 대표 access point code */
    private String primaryAccessPointCode;
}