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
@Table(name = "tb_inventory_equipment", idStrategy = GenerationRule.UUID)
public class TbInventoryEquipment extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "equip_type", nullable = false, length = 40)
    private String equipType;

    @Column(name = "equip_code", nullable = false, length = 40)
    private String equipCode;

    @Column(name = "equip_status", nullable = false, length = 20)
    private String equipStatus;

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "position_z")
    private Double positionZ;

    @Column(name = "rotation_cw")
    private Integer rotationCw;
}