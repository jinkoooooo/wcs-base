package operato.logis.changwon.entity.WMS;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

import java.util.Date;

@Setter
@Getter
@Table(name = "c_wms_result", idStrategy = GenerationRule.UUID)
public class WmsResult extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "order_id", nullable = false, length = 15)
    private String orderId;

    @Column(name = "ord_seq", nullable = false, length = 5)
    private String ordSeq;

    @Column(name = "order_kind", nullable = false, length = 2)
    private String orderKind;

    @Column(name = "cust_id", nullable = false, length = 20)
    private String custId;

    @Column(name = "lot_id", nullable = false, length = 40)
    private String lotId;

    @Column(name = "item_code", nullable = false, length = 30)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "loc_id", length = 20)
    private String locId;

    @Column(name = "menge", length = 4)
    private Integer menge;

    @Column(name = "result_type", nullable = false, length = 2)
    private String resultType;

    @Column(name = "error_machine", length = 6)
    private String errorMachine;

    @Column(name = "error_code", length = 4)
    private Integer errorCode;

    @Column(name = "flag", nullable = false, length = 4)
    private Integer flag;

    @Column(name = "wms_snd_yn", length = 2)
    private String wmsSndYn;

    @Column(name = "reg_id", length = 20)
    private String regId;

    @Column(name = "reg_dt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date regDt;
}
