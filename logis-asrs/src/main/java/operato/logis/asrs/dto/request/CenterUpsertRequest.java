package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 센터 등록/수정 요청 DTO.
 *
 * <p>
 * snake_case / camelCase 모두 수용한다.
 * </p>
 */
@Data
public class CenterUpsertRequest {

    @JsonAlias({"centerCode", "center_code"})
    private String centerCode;

    @JsonAlias({"centerName", "center_name"})
    private String centerName;

    @JsonAlias({"centerType", "center_type"})
    private String centerType;

    @JsonAlias({"timezone"})
    private String timezone;

    @JsonAlias({"description"})
    private String description;

    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}