package operato.logis.simulator.tspg.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_eq_rack_mst", idStrategy = GenerationRule.UUID)
public class TbEqRackMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "eq_id", nullable = false)
    private String eqId;

    @Column(name = "rack_id", nullable = false)
    private String rackId;

    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "row", nullable = false)
    private Integer row;

    @Column(name = "bay", nullable = false)
    private Integer bay;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "sku_id")
    private String skuId;

    @Column(name = "sku_qty")
    private Integer skuQty;

    @Column(name = "drive_only_yn", nullable = false)
    private Boolean driveOnlyYn;

    @Column(name = "cargo_yn", nullable = false)
    private Boolean cargoYn;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "reserve_car_eq_id")
    private String reserveCarEqId;

    @Column(name = "buffer_yn", nullable = false)
    private Boolean bufferYn;

    @Column(name = "use_yn", nullable = false)
    private Boolean useYn;

    @Column(name = "error_id")
    private String errorId;

    @Column(name = "error_desc")
    private String errorDesc;
}