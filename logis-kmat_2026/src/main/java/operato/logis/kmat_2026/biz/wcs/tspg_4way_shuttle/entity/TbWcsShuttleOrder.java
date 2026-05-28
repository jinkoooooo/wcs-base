package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_wcs_shuttle_order", idStrategy = GenerationRule.UUID,
        uniqueFields = "orderKey",
        indexes = {
                @Index(name = "ux_tb_wcs_shuttle_order_key", columnList = "order_key", unique = true),
        })
public class TbWcsShuttleOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_key", length = 100)
    private String orderKey;

    @Column(name = "order_type", length = 30)
    private String orderType;

    @Column(name = "order_status")
    private int orderStatus;

    @Column(name = "priority")
    private int priority;

    @Column(name = "from_loc_code", length = 50)
    private String fromLocCode;

    @Column(name = "to_loc_code", length = 50)
    private String toLocCode;

    @Column(name = "ecs_if_status")
    private int ecsIfStatus;

    @Column(name = "owner_code", length = 20)
    private String ownerCode;

    @Column(name = "eq_group_id", length = 20)
    private String eqGroupId;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "remark", length = 4000)
    private String remark;
}
