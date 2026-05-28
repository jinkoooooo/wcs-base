package operato.logis.samsung.dto.ai.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SerialProgressDto {
    private String serialNo;
    private String itemCode;
    private String itemName;

    private String currentTrackingType;
    private Integer currentTrackingStatus;
    private String trackingDesc;
    private LocalDateTime lastTrackingAt;

    private String currentLineId;
    private String currentEquipId;
    private String currentPid;
    private Boolean picked;

    private Integer finalStatus;
    private String rejectType;
}