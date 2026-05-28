package operato.logis.samsung.entity.mw;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

@Getter
@Setter
@Table(name = "tb_mw_reject_box", idStrategy = GenerationRule.UUID)
public class TbMwRejectBox extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "inner_item_code", length = 100)
    private String innerItemCode;

    @Column(name = "serial_no", nullable = false, length = 100)
    private String serialNo;

    @Column(name = "cntr_no", nullable = false, length = 20)
    private String cntrNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "inbound_date", nullable = false)
    private Date inboundDate;

    @Column(name = "reject_datetime", nullable = false)
    private Date rejectDatetime;
}