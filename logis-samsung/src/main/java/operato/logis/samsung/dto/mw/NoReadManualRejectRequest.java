package operato.logis.samsung.dto.mw;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoReadManualRejectRequest {

    @JsonAlias({"id"})
    private String id;

    @JsonAlias({"serialNo", "serial_no", "boxId", "box_id"})
    private String serialNo;

    @JsonAlias({"blNo", "bl_no"})
    private String blNo;

    @JsonAlias({"cntrNo", "cntr_no"})
    private String cntrNo;

    @JsonAlias({"itemCode", "item_code"})
    private String itemCode;

    @JsonAlias({"finalRemark", "final_remark", "reason"})
    private String finalRemark;
}