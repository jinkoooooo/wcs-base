package operato.logis.samsung.cognex.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "tb_mw_bcr_data", idStrategy = GenerationRule.UUID)
public class TbMwBcrData extends ElidomStampHook {

    /**
     * PK: id (varchar(40), Not Null)
     */
    @PrimaryKey
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "seqno", length = 10)
    private String seqno;

    @Column(name = "length")
    private Integer length;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "barcodedata", length = 40)
    private String barcodedata;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    @Column(name = "reg_no", length = 15)
    private String regNo;

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    @Column(name = "upd_no", length = 15)
    private String updNo;

    @Column(name = "item_code", length = 64)
    private String itemCode;

    @Column(name = "angle")
    private Integer angle;

    @Column(name = "device_name")
    private Integer deviceName;
}