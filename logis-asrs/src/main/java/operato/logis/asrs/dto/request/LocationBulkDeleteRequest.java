package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 로케이션 일괄삭제 요청 DTO.
 *
 * <p>
 * 현재 조회조건 기준으로 삭제 대상을 추출한다.
 * </p>
 */
@Data
public class LocationBulkDeleteRequest {

    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    @JsonAlias({"locationCode", "location_code"})
    private String locationCode;

    @JsonAlias({"locationType", "location_type"})
    private String locationType;

    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}