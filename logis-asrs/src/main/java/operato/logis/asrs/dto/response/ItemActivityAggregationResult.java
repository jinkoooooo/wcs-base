package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일별 활동 집계 실행 결과 DTO.
 *
 * <p>
 * 영역 단위 또는 품목 단위 집계 실행 결과를 응답한다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemActivityAggregationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    private String areaCode;

    /** 품목 코드 */
    private String itemCode;

    /** 집계 기준일자 (yyyy-MM-dd) */
    private String activityDate;

    /** 집계 대상 품목 수 */
    private Integer targetItemCount;

    /** 실제 upsert 건수 */
    private Integer upsertedCount;

    /** 결과 메시지 */
    private String message;
}