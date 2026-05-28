package operato.logis.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 기본 설비 상세 응답 DTO
 * - TbEqMst + TbEqPlcMst 정보를 합쳐서 반환
 */
@Getter
@Setter
public class EqMstDetailResponse {

    // TbEqMst 필드
    @JsonProperty("id")
    private String id;

    @JsonProperty("eqGroupId")
    private String eqGroupId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private int type;

    @JsonProperty("plcId")
    private String plcId;

    // TbEqPlcMst 필드 (plcId 있는 경우만)
    @JsonProperty("plcName")
    private String plcName;

    @JsonProperty("plcIp")
    private String plcIp;

    @JsonProperty("plcPort")
    private int plcPort;

    @JsonProperty("plcIfType")
    private String plcIfType;

    @JsonProperty("plcEqType")
    private int plcEqType;

    @JsonProperty("connectYn")
    private boolean connectYn;

    @JsonProperty("useYn")
    private boolean useYn;
}
