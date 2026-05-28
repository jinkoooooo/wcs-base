package operato.logis.simulator.tspg.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_eq_car_mst", idStrategy = GenerationRule.UUID)
public class TbEqCarMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "eq_id", nullable = false)
    private String eqId;

    @Column(name = "type")
    private String type;

    @Column(name = "row", nullable = false)
    private Integer row;

    @Column(name = "bay", nullable = false)
    private Integer bay;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "rack_id", nullable = false)
    private String rackId;

    @Column(name = "rack_eq_id", nullable = false)
    private String rackEqId;

    @Column(name = "auto_yn", nullable = false)
    private Boolean autoYn;

    @Column(name = "plc_cmd_id", nullable = false)
    private Integer plcCmdId;

    @Column(name = "plc_comp_cmd_id", nullable = false)
    private Integer plcCompCmdId;

    @Column(name = "complete_yn", nullable = false)
    private Boolean completeYn;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "battery_status", nullable = false)
    private Integer batteryStatus;

    @Column(name = "cargo_yn", nullable = false)
    private Boolean cargoYn;

    @Column(name = "req_move_home_yn", nullable = false)
    private Boolean reqMoveHomeYn;

    @Column(name = "min_row")
    private Integer minRow;

    @Column(name = "max_row")
    private Integer maxRow;

    @Column(name = "error_id")
    private String errorId;

    @Column(name = "error_desc")
    private String errorDesc;

    @Column(name = "use_yn", nullable = false)
    private Boolean useYn;
}