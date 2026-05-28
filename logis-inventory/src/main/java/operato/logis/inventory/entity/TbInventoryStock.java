package operato.logis.inventory.entity;

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
@Table(name = "tb_inventory_stock", idStrategy = GenerationRule.UUID)
public class TbInventoryStock extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "stock_id", nullable = false, length = 100)
    private String stockId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "item_owner", nullable = false, length = 100)
    private String itemOwner;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "item_qty")
    private Integer itemQty;

    @Column(name = "lot_no", length = 100)
    private String lotNo;

    @Column(name = "stock_status", nullable = false)
    private Integer stockStatus;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "item_priority")
    private Integer itemPriority;

    @Column(name = "inb_datetime")
    private Date inbDatetime;

    @Column(name = "expired_datetime")
    private Date expiredDatetime;

    @Column(name = "stock_height")
    private Integer stockHeight;

    @Column(name = "attribute_a", length = 40)
    private String attributeA;
}