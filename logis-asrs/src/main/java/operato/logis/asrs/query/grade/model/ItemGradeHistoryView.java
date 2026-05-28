package operato.logis.asrs.query.grade.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 상품 등급 변경 이력 조회 View DTO.
 */
@Getter
@Setter
@ToString
public class ItemGradeHistoryView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** item_grade_hist row id */
    private String itemGradeHistId;

    /** item_grade row id */
    private String itemGradeId;

    /** 영역 코드 */
    private String areaCode;

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

    /** 이전 등급 */
    private String previousGrade;

    /** 신규 등급 */
    private String newGrade;

    /** 이전 점수 */
    private Integer previousScore;

    /** 신규 점수 */
    private Integer newScore;

    /** 변경 근거 JSON */
    private String reasonJson;

    /** 계산 일시 */
    private Date calculatedAt;
}