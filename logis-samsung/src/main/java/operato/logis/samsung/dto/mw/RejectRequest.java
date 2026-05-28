package operato.logis.samsung.dto.mw;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.mw.TbMwBox;

@Getter
@Setter
public class RejectRequest {

    private TbMwBox param;

    private String rejectDesc;

    private String rejectType;
}
