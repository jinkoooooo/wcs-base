package operato.logis.asrs.dto.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 로케이션 접근성 미리보기 요청 DTO.
 */
@Getter
@Setter
@ToString
public class LocationAccessPreviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 접근 목적 코드 (INBOUND / OUTBOUND / PICK / RELOCATION) */
    @JsonAlias({"purposeCode", "purpose_code"})
    private String purposeCode;

    /** A 구간 비율(예: 0.15) */
    @JsonAlias({"gradeARatio", "grade_a_ratio"})
    private Double gradeARatio;

    /** B 구간 누적 비율(예: 0.50) */
    @JsonAlias({"gradeBRatio", "grade_b_ratio"})
    private Double gradeBRatio;

    /** C 구간 누적 비율(예: 0.80) */
    @JsonAlias({"gradeCRatio", "grade_c_ratio"})
    private Double gradeCRatio;

    /** 미리보기 최대 건수 */
    @JsonAlias({"limit", "limit"})
    private Integer limit;
}