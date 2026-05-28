package operato.logis.ecs.base.ecs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
//@Table(name = "tb_wcs_crane_order", idStrategy = GenerationRule.UUID)
public class TbWcsCraneOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_key", length = 100)
    private String orderKey;

    @Column(name = "order_type")
    private String orderType;

    @Column(name = "order_status")
    private int orderStatus;

    @Column(name = "priority")
    private int priority;

    @Column(name = "from_loc_code", length = 50)
    private String fromLocId;

    @Column(name = "to_loc_code", length = 50)
    private String toLocId;

    @Column(name = "ecs_if_status")
    private int ecsIfStatus;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "eq_group_id")
    private String eqGroupId;
}
