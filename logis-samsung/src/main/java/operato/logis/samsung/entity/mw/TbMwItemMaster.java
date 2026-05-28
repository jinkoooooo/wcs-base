package operato.logis.samsung.entity.mw;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_mw_item_master", idStrategy = GenerationRule.UUID)
public class TbMwItemMaster extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 20)
    private String lcId; // Delivery Plant

    @Column(name = "cust_id", length = 20)
    private String custId;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode; // Material

    @Column(name = "inner_item_code", length = 100)
    private String innerItemCode;

    @Column(name = "item_type", length = 20)
    private String itemType; // 품목 구분

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "item_group", length = 100)
    private String itemGroup; // Material Group

    @Column(name = "division", length = 10)
    private String division; // Division

    @Column(name = "item_barcode_1", length = 100)
    private String itemBarcode1;

    @Column(name = "item_barcode_2", length = 100)
    private String itemBarcode2;

    @Column(name = "item_barcode_3", length = 100)
    private String itemBarcode3;

    @Column(name = "item_barcode_4", length = 100)
    private String itemBarcode4;

    @Column(name = "item_length", nullable = false)
    private Integer itemLength; // Length

    @Column(name = "item_width", nullable = false)
    private Integer itemWidth; // Width

    @Column(name = "item_height", nullable = false)
    private Integer itemHeight; // Height

    @Column(name = "unit_dimension", nullable = false, length = 10)
    private String unitDimension; // Unit of Dimension

    @Column(name = "item_weight", nullable = false)
    private Double itemWeight; // Gross Wgt

    @Column(name = "unit_weight", nullable = false, length = 10)
    private String unitWeight; // Weight Unit

    @Column(name = "item_cbm", nullable = false)
    private Double itemCbm; // Volume

    @Column(name = "unit_cbm", nullable = false, length = 10)
    private String unitCbm; // Volume Unit

    @Column(name = "pallet_capacity", length = 10)
    private String palletCapacity;

    @Column(name = "box_barcode", length = 100)
    private String boxBarcode;

    @Column(name = "inb_type", length = 20)
    private String inbType;

    @Column(name = "store_type", length = 20)
    private String storeType;

    @Column(name = "loc_type", length = 20)
    private String locType;

    @Column(name = "store_mix_yn", length = 1)
    private String storeMixYn;

    @Column(name = "inb_pack_yn", length = 1)
    private String inbPackYn;

    @Column(name = "inb_qc_yn", length = 1)
    private String inbQcYn;

    @Column(name = "inb_decant_yn", length = 1)
    private String inbDecantYn;

    @Column(name = "plt_type", length = 100)
    private String pltType;

    @Column(name = "plt_box", length = 100)
    private String pltBox;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "reg_id", length = 32)
    private String regId;

    @Column(name = "reg_time", length = 32)
    private String regTime;

}
