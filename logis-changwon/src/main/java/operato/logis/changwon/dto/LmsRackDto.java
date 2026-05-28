package operato.logis.changwon.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LmsRackDto {

    private String locCd;

    private String taskId;

    private String stockId;

    private Integer rackDisabled;

    private Integer rackLocked;
}
