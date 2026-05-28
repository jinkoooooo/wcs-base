package operato.logis.asrs.dto.response;

import java.util.List;

import lombok.Data;

/**
 * 2D 맵 응답 DTO.
 */
@Data
public class OutboundLocation2DMapResponse {

    private String areaCode;
    private Integer aisleNo;
    private String sideCode;

    private Integer maxBayNo;
    private Integer maxLevelNo;

    private List<OutboundLocation2DCellResponse> cells;
}