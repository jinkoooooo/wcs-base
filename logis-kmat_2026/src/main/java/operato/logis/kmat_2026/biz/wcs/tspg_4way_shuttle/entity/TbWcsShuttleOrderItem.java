package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_wcs_shuttle_order_item", idStrategy = GenerationRule.UUID)
public class TbWcsShuttleOrderItem extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_key", length = 64)
    private String orderKey;

    @Column(name = "line_no")
    private int lineNo;

    @Column(name = "sku_code", length = 64)
    private String skuCode;

    @Column(name = "lot_no", length = 40)
    private String lotNo;

    @Column(name = "qty")
    private int qty;

    @Column(name = "uom", length = 10)
    private String uom;

    @Column(name = "line_status")
    private int lineStatus;
}