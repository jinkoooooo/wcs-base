package operato.logis.asrs.dto.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 조회 조건 DTO.
 */
@Getter
@Setter
@ToString
public class AccessPointSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 포인트 코드 */
    @JsonAlias({"pointCode", "point_code"})
    private String pointCode;

    /** 포인트 명 */
    @JsonAlias({"pointName", "point_name"})
    private String pointName;

    /** 포인트 타입 */
    @JsonAlias({"pointType", "point_type"})
    private String pointType;

    /** 목적 코드 */
    @JsonAlias({"purposeCode", "purpose_code"})
    private String purposeCode;

    /** 접근성 산정 사용 여부 */
    @JsonAlias({"useForSortYn", "use_for_sort_yn"})
    private String useForSortYn;

    /** 활성 여부 */
    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}