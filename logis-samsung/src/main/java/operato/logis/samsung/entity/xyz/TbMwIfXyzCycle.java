package operato.logis.samsung.entity.xyz;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "tb_mw_if_xyz_cycle", idStrategy = GenerationRule.UUID)
public class TbMwIfXyzCycle extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "cycle_id", nullable = false, length = 30)
    private String cycleId;

    @Column(name = "pick_num", nullable = false)
    private Integer pickNum;

    @Column(name = "pallet_id", nullable = false, length = 10)
    private String palletId;
}