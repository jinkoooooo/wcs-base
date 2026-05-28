package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 로케이션 접근성 재산출 실행 결과 DTO.
 */
@Getter
@Setter
@ToString
public class LocationAccessRecalculateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    private String areaCode;

    /** 접근 목적 코드 */
    private String purposeCode;

    /** 대상 로케이션 수 */
    private Integer targetLocationCount;

    /** 업데이트 완료 건수 */
    private Integer updatedCount;

    /** A 등급 건수 */
    private Integer gradeACount;

    /** B 등급 건수 */
    private Integer gradeBCount;

    /** C 등급 건수 */
    private Integer gradeCCount;

    /** D 등급 건수 */
    private Integer gradeDCount;

    /** 처리 메시지 */
    private String message;
}