package operato.logis.simulator.asrs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_simulator_equip", idStrategy = GenerationRule.UUID)
public class TbSimulatorAsrsEquip extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "crane_id", nullable = false)
    private String craneId;

    @Column(name = "row", nullable = false)
    private Integer row;

    @Column(name = "bay", nullable = false)
    private Integer bay;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "crane_status", nullable = false)
    private Integer craneStatus;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "is_cargo", nullable = false)
    private Boolean isCargo;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_desc")
    private String errorDesc;
}