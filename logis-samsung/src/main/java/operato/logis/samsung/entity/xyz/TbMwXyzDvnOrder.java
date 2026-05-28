package operato.logis.samsung.entity.xyz;


import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_mw_xyz_dvn_order", idStrategy = GenerationRule.UUID)
public class TbMwXyzDvnOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "order_key", nullable = false, length = 30)
    private String orderKey;

    @Column(name = "result_code", length = 30)
    private String resultCode;

    @Column(name = "result_msg", length = 30)
    private String resultMsg;

    @Column(name = "barcode", length = 50)
    private String barcode;


}
