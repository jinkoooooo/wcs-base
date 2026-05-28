package operato.logis.posco.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class McsMonitoringRequestDto {

    private String deviceId;

    private String equipType;

    private String equipCode;
}
