package operato.logis.asrs.query.grade.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 현재 상품 등급 스냅샷 조회 View DTO.
 */
@Getter
@Setter
@ToString
public class ItemGradeView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** item_grade row id */
    private String itemGradeId;

    /** 영역 row id */
    private String areaId;

    /** 영역 코드 */
    private String areaCode;

    /** 품목 row id */
    private String itemId;

    /** 품목 코드 */
    private String itemCode;

    /** 품목명 */
    private String itemName;

    /** 정책 row id */
    private String gradePolicyId;

    /** 정책 코드 */
    private String policyCode;

    /** 정책명 */
    private String policyName;

    /** 초기 수동 등급 */
    private String manualSeedGrade;

    /** 초기 수동 점수 */
    private Integer manualSeedScore;

    /** 학습 점수 */
    private Integer learnedScore;

    /** 최종 점수 */
    private Integer finalScore;

    /** 현재 등급 */
    private String currentGrade;

    /** 최종 계산 일시 */
    private Date lastCalculatedAt;
}