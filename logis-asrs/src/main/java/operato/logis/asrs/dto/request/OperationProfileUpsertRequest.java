package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 오퍼레이션 프로필 등록/수정 요청 DTO.
 *
 * <p>
 * snake_case / camelCase 모두 수용한다.
 * </p>
 */
@Data
public class OperationProfileUpsertRequest {

    @JsonAlias({"profileCode", "profile_code"})
    private String profileCode;

    @JsonAlias({"profileName", "profile_name"})
    private String profileName;

    @JsonAlias({"industryType", "industry_type"})
    private String industryType;

    @JsonAlias({"description"})
    private String description;

    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}