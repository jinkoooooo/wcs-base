package operato.logis.asrs.dto.response;

import java.util.List;

import lombok.Data;

/**
 * 2D 맵 cell 응답 DTO.
 *
 * bayNo + levelNo 기준 1 cell.
 * depths 에 D1 / D2 정보 포함.
 */
@Data
public class OutboundLocation2DCellResponse {

    private Integer bayNo;
    private Integer levelNo;

    private List<OutboundLocation2DDepthResponse> depths;
}