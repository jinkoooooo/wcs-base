package operato.logis.samsung.entity.mw;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_mw_box_conveyor_info", idStrategy = GenerationRule.UUID)
public class TbMwBoxConveyorInfo extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "pallet_sequence", length = 20)
    private String palletSequence;

    @Column(name = "task_id", nullable = false, length = 30)
    private String taskId;

    @Column(name = "index", nullable = false)
    private Integer index;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "serial_no", nullable = false, length = 100)
    private String serialNo;

    @Column(name = "pid", nullable = false, length = 100)
    private String pid;

    @Column(name = "is_picked", nullable = false)
    private boolean isPicked;
}