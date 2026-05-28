package operato.logis.ecs.tspg4way.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_eq_cv_mst", idStrategy = GenerationRule.UUID, uniqueFields = "eqId,id", indexes = {
        @Index(name = "ix_tb_eq_cv_mst_index_0", columnList = "eq_id,id", unique = true)
})
public class TbEqCvMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "eq_id", length = 50)
    private String eqId;

    /**
     * Conveyor 타입/구분 코드
     */
    @Column(name = "type", length = 30)
    private int type;

    @Column(name = "auto_yn")
    private boolean autoYn;

    @Column(name = "cargo_yn")
    private boolean cargoYn;

    @Column(name = "stopper_open_yn")
    private boolean stopperOpenYn;

    @Column(name = "run_yn")
    private boolean runYn;

    @Column(name = "level")
    private int level;

    @Column(name = "plc_cmd_id")
    private int plcCmdId;

    @Column(name = "rack_row")
    private int rackRow;
    @Column(name = "rack_bay")
    private int rackBay;

    @Column(name = "status")
    private int status;

    @Column(name = "error_id", length = 50)
    private String errorId;

    @Column(name = "error_desc", length = 500)
    private String errorDesc;

    @Column(name = "use_yn")
    private boolean useYn;
}
