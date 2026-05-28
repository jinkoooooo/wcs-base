package operato.logis.samsung.dto.xyz;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팔레타이징 작업 화면 조회 DTO
 */
@Getter
@NoArgsConstructor
public class XyzOrderInfoRequest {

    private Integer limit;
    private String processStatus;
    private String itemCode;        // Material
    private String startPointCd;    // Box Conveyor
    private String endPointCd;      // Pallet Conveyor
    private String acceptDatetime;
}