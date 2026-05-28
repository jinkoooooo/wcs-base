package operato.logis.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

/**
 * tb_inventory_location 단건 update 요청 DTO
 *
 * 모든 필드는 optional. null 이면 변경하지 않음.
 * (locGroup, rackEqId, locId) 3중 키 식별 후 부분 갱신 용도.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class LocationUpdateRequest {

    private String itemType;
    private String itemGroup;
    private Integer itemGrade;
    private Integer maxHeight;
    private Integer maxWeight;
    private Integer locDeep;
    private String locSide;
    private Boolean isEnabled;
    private Boolean isInboundEnabled;
    private Boolean isOutboundEnabled;
    private Boolean isPath;
    private String equipType;
    private String equipCode;
    private String destNodeCode;
    /** ExtTbInventoryLocation.loc_type — RACK / INBOUND_PORT / OUTBOUND_PORT / CHARGE_PORT / PILLAR 등 */
    private String locType;
}
