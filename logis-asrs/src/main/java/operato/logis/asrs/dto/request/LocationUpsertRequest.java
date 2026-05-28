package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 로케이션 등록/수정 요청 DTO.
 *
 * <p>
 * 외부 입력은 code 기준으로 받고,
 * 저장 시에는 내부에서 FK id 로 resolve 한다.
 * </p>
 */
@Data
public class LocationUpsertRequest {

    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    @JsonAlias({"locationCode", "location_code"})
    private String locationCode;

    @JsonAlias({"aisleNo", "aisle_no"})
    private Integer aisleNo;

    @JsonAlias({"sideCode", "side_code"})
    private String sideCode;

    @JsonAlias({"bayNo", "bay_no"})
    private Integer bayNo;

    @JsonAlias({"levelNo", "level_no"})
    private Integer levelNo;

    @JsonAlias({"depthNo", "depth_no"})
    private Integer depthNo;

    @JsonAlias({"locationType", "location_type"})
    private String locationType;

    @JsonAlias({"usageStatusCode", "usage_status_code"})
    private String usageStatusCode;

    @JsonAlias({"inboundAllowedYn", "inbound_allowed_yn"})
    private String inboundAllowedYn;

    @JsonAlias({"outboundAllowedYn", "outbound_allowed_yn"})
    private String outboundAllowedYn;

    @JsonAlias({"mixedLoadYn", "mixed_load_yn"})
    private String mixedLoadYn;

    @JsonAlias({"frontPriorityYn", "front_priority_yn"})
    private String frontPriorityYn;

    @JsonAlias({"dedicatedItemCategoryCode", "dedicated_item_category_code"})
    private String dedicatedItemCategoryCode;

    @JsonAlias({"maxWeightG", "max_weight_g"})
    private Integer maxWeightG;

    @JsonAlias({"maxVolumeMm3", "max_volume_mm3"})
    private Integer maxVolumeMm3;

    @JsonAlias({"sortSeq", "sort_seq"})
    private Integer sortSeq;

    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;

    @JsonAlias({"locationGrade", "location_grade"})
    private String locationGrade;

    @JsonAlias({"accessScore", "access_score"})
    private Integer accessScore;

    @JsonAlias({"primaryAccessPointCode", "primary_access_point_code"})
    private String primaryAccessPointCode;
}