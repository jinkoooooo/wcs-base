package operato.logis.posco.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_mcs_equip", idStrategy = GenerationRule.UUID)
public class TbMcsEquip extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "equip_type")
    private String equipType;

    @Column(name = "equip_code")
    private String equipCode;

    @Column(name = "status")
    private String status;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "rotation")
    private Integer rotation;

    @Column(name = "battery")
    private Integer battery;

    @Column(name = "pod_code")
    private String podCode;

    @Column(name = "pod_type")
    private String podType;

    @Column(name = "task_id")
    private String taskId;
}