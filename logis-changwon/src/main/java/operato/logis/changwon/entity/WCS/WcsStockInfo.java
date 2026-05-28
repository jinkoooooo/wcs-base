package operato.logis.changwon.entity.WCS;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "wcs_stock_info", idStrategy = GenerationRule.UUID)
public class WcsStockInfo extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 10)
    private String lcId;

    @Column(name = "stock_id", nullable = false, length = 22)
    private String stockId;

    @Column(name = "sku_id", length = 80)
    private String skuId;

    @Column(name = "item_code", nullable = false, length = 30)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_owner", nullable = false, length = 30)
    private String itemOwner;

    @Column(name = "box_qty", nullable = false, length = 4)
    private Integer boxQty;

    @Column(name = "sub_standard", nullable = false, length = 4)
    private Integer subStandard;

    @Column(name = "suspended", nullable = false, length = 4)
    private Integer suspended;

    @Column(name = "stock_locked", nullable = false, length = 4)
    private Integer stockLocked;

    @Column(name = "stock_disabled", nullable = false, length = 4)
    private Integer stockDisabled;

    @Column(name = "inb_date")
    private Date inbDate;

    @Column(name = "lot_no", length = 255)
    private String lotNo;

    @Column(name = "inb_del_no", length = 50)
    private String inbDelNo;

    @Column(name = "stock_priority", nullable = false, length = 4)
    private Integer stockPriority;

    @Column(name = "stock_memo", length = 255)
    private String stockMemo;

    @Column(name = "attribute_a", length = 256)
    private String attributeA;

    @Column(name = "attribute_b", length = 256)
    private String attributeB;

    @Column(name = "attribute_c", length = 256)
    private String attributeC;

    @Column(name = "stock_type", length = 5)
    private String stockType;
}