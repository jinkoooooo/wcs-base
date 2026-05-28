package operato.logis.kmat_2026.biz.ecs.tspg4way.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_eq_car_mst", idStrategy = GenerationRule.UUID, uniqueFields = "eqId,id", indexes = {
    @Index(name = "ix_tb_eq_car_mst_index_0", columnList = "eq_id,id", unique = true)
})
public class TbEqCarMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "eq_id", length = 50)
    private String eqId;

    /**
     * 설비/차량 타입 코드 (예: SHUTTLE, 4WAY 등)
     * 실제 컬럼명이 car_type 이면 name만 car_type으로 바꾸면 됨
     */
    @Column(name = "type", length = 30)
    private String type;

    @Column(name = "row")
    private int row;

    @Column(name = "bay")
    private int bay;

    @Column(name = "level")
    private int level;

    @Column(name = "rack_id", length = 50)
    private String rackId;

    @Column(name = "rack_eq_id", length = 50)
    private String rackEqId;
    @Column(name = "auto_yn")
    private boolean autoYn;

    @Column(name = "plc_cmd_id")
    private int plcCmdId;

    @Column(name = "plc_comp_cmd_id")
    private int plcCompCmdId;

    @Column(name = "status")
    private int status;

    @Column(name = "battery_status")
    private int batteryStatus;

    @Column(name = "cargo_yn")
    private boolean cargoYn;

    @Column(name = "min_row")
    private int minRow;

    @Column(name = "max_row")
    private int maxRow;

    @Column(name = "error_id", length = 50)
    private String errorId;

    @Column(name = "error_desc", length = 500)
    private String errorDesc;

    @Column(name = "use_yn")
    private boolean useYn;

    @Column(name = "complete_yn")
    private boolean completeYn;
}
