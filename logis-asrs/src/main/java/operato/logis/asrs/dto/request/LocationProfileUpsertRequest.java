package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 로케이션 프로필 등록/수정 요청 DTO.
 *
 * <p>
 * 외부 입력은 areaCode + profileCode 기준.
 * 내부 저장 시 area_id 는 code 로 resolve 한다.
 * </p>
 */
@Data
public class LocationProfileUpsertRequest {

    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    @JsonAlias({"profileCode", "profile_code"})
    private String profileCode;

    @JsonAlias({"profileName", "profile_name"})
    private String profileName;

    @JsonAlias({"aisleStart", "aisle_start"})
    private Integer aisleStart;

    @JsonAlias({"aisleEnd", "aisle_end"})
    private Integer aisleEnd;

    @JsonAlias({"sideCodes", "side_codes"})
    private String sideCodes;

    @JsonAlias({"bayStart", "bay_start"})
    private Integer bayStart;

    @JsonAlias({"bayEnd", "bay_end"})
    private Integer bayEnd;

    @JsonAlias({"levelStart", "level_start"})
    private Integer levelStart;

    @JsonAlias({"levelEnd", "level_end"})
    private Integer levelEnd;

    @JsonAlias({"depthStart", "depth_start"})
    private Integer depthStart;

    @JsonAlias({"depthEnd", "depth_end"})
    private Integer depthEnd;

    @JsonAlias({"locationType", "location_type"})
    private String locationType;

    @JsonAlias({"codePattern", "code_pattern"})
    private String codePattern;

    @JsonAlias({"mixedLoadYn", "mixed_load_yn"})
    private String mixedLoadYn;

    @JsonAlias({"inboundAllowedYn", "inbound_allowed_yn"})
    private String inboundAllowedYn;

    @JsonAlias({"outboundAllowedYn", "outbound_allowed_yn"})
    private String outboundAllowedYn;

    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}