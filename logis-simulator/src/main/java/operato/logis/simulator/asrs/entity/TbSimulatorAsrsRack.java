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
@Table(name = "tb_simulator_rack", idStrategy = GenerationRule.UUID)
public class TbSimulatorAsrsRack extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "loc_id", nullable = false)
    private String locId;

    @Column(name = "row", nullable = false)
    private Integer row;

    @Column(name = "bay", nullable = false)
    private Integer bay;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "is_cargo", nullable = false)
    private Boolean isCargo;

    @Column(name = "cargo_id")
    private String cargoId;
}