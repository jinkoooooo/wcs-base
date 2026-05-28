package operato.logis.asrs.dto.response;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 로케이션 접근성 미리보기 결과 DTO.
 */
@Getter
@Setter
@ToString
public class LocationAccessPreviewResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    private String areaCode;

    /** 접근 목적 코드 */
    private String purposeCode;

    /** 전체 대상 건수 */
    private Integer totalCount;

    /** 미리보기 반환 건수 */
    private Integer previewCount;

    /** 미리보기 행 목록 */
    private List<LocationAccessPreviewRow> rows;
}