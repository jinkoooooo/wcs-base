package operato.logis.asrs.entity;

import lombok.Data;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "tb_inventory_location", idStrategy = GenerationRule.UUID)
@Data
public class LocationGenerator extends ElidomStampHook {

    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "loc_group", length = 20)
    private String locGroup;

    @Column(name = "loc_code", length = 40)
    private String locCode;

    // 💡 숫자형(int4) 및 Not Null 조건 매핑
    @Column(name = "loc_col", nullable = false)
    private Integer locCol;

    @Column(name = "loc_row", nullable = false)
    private Integer locRow;

    @Column(name = "loc_level", nullable = false)
    private Integer locLevel;

    @Column(name = "loc_deep", nullable = false)
    private Integer locDeep;

    @Column(name = "loc_side", length = 10)
    private String locSide;

    @Column(name = "item_type", length = 20)
    private String itemType;

    @Column(name = "item_group", length = 20)
    private String itemGroup;

    @Column(name = "item_grade")
    private Integer itemGrade;

    @Column(name = "max_height", nullable = false)
    private Integer maxHeight;

    @Column(name = "max_weight", nullable = false)
    private Integer maxWeight;

    // 💡 boolean 매핑 (is_enabled, is_path)
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "equip_type", length = 40)
    private String equipType;

    @Column(name = "equip_code", length = 40)
    private String equipCode;

    @Column(name = "dest_node_code", length = 40)
    private String destNodeCode;

    @Column(name = "task_id", length = 100)
    private String taskId;

    @Column(name = "stock_id", length = 100)
    private String stockId;

    @Column(name = "is_path", nullable = false)
    private Boolean isPath;

    // 💡 실수형(float4) 매핑
    @Column(name = "position_x")
    private Float positionX;

    @Column(name = "position_y")
    private Float positionY;

    @Column(name = "position_z")
    private Float positionZ;

    @Column(name = "rotation_cw")
    private Integer rotationCw;

}