package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 품목 등급 계산 실행 결과 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemGradeCalculationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    private String areaCode;

    /** 품목 코드 */
    private String itemCode;

    /** 계산 기준 일자 */
    private String activityDate;

    /** 적용 정책 코드 */
    private String policyCode;

    /** 대상 품목 수 */
    private Integer targetItemCount;

    /** 계산 완료 건수 */
    private Integer calculatedCount;

    /** 이력 생성 건수 */
    private Integer historyCount;

    /** 결과 메시지 */
    private String message;
}