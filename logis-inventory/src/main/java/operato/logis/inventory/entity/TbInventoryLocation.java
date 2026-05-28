package operato.logis.inventory.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_inventory_location", idStrategy = GenerationRule.UUID)
public class TbInventoryLocation extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "loc_group", nullable = false, length = 20)
    private String locGroup;

    @Column(name = "loc_type", nullable = false, length = 20)
    private String locType;

    @Column(name = "loc_id", nullable = false, length = 40)
    private String locId;

    @Column(name = "loc_code", nullable = false, length = 40)
    private String locCode;

    @Column(name = "loc_col", nullable = false)
    private Integer locCol;

    @Column(name = "loc_row", nullable = false)
    private Integer locRow;

    @Column(name = "loc_level", nullable = false)
    private Integer locLevel;

    @Column(name = "loc_deep", nullable = false)
    private Integer locDeep;

    @Column(name = "loc_side", nullable = false, length = 10)
    private String locSide;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "item_group", nullable = false, length = 20)
    private String itemGroup;

    @Column(name = "item_grade")
    private Integer itemGrade;

    @Column(name = "max_height", nullable = false)
    private Integer maxHeight;

    @Column(name = "max_weight", nullable = false)
    private Integer maxWeight;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "is_inbound_enabled", nullable = false)
    private Boolean isInboundEnabled;

    @Column(name = "is_outbound_enabled", nullable = false)
    private Boolean isOutboundEnabled;

    @Column(name = "equip_type", length = 40)
    private String equipType;

    @Column(name = "equip_code", length = 40)
    private String equipCode;

    @Column(name = "dest_node_code", nullable = false, length = 40)
    private String destNodeCode;

    @Column(name = "task_id", length = 100)
    private String taskId;

    @Column(name = "stock_id", length = 100)
    private String stockId;

    @Column(name = "is_path", nullable = false)
    private Boolean isPath;

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "position_z")
    private Double positionZ;

    @Column(name = "rotation_cw")
    private Integer rotationCw;
}