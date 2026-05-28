package operato.logis.samsung.entity.mw;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "tb_mw_chutes", idStrategy = GenerationRule.UUID)
public class TbMwChute extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "start_point_cd", nullable = false, length = 20)
    private String startPointCd;

    @Column(name = "end_point_cd", nullable = false, length = 20)
    private String endPointCd;

    @Column(name = "pallet_sequence", length = 20)
    private String palletSequence;

    @Column(name = "plt_type", length = 20)
    private String pltType;

    @Column(name = "order_id", length = 40)
    private String orderId;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "box_qty")
    private Integer boxQty;

    @Column(name = "is_use")
    private boolean isUse;
}