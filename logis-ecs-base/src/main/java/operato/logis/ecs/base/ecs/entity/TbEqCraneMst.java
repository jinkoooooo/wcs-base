package operato.logis.ecs.base.ecs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

// 검토중) 사용확인
@Getter
@Setter
@Table(name = "tb_eq_crane_mst", idStrategy = GenerationRule.UUID, uniqueFields = "eqId,id", indexes = {
        @Index(name = "ix_tb_eq_car_mst_index_0", columnList = "eq_id,id", unique = true)
})
public class TbEqCraneMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "eq_id", length = 50) // 사용 / plc registry와 통신
    private String eqId;

    /**
     * 설비/차량 타입 코드 (예: CRANE, 4WAY 등)
     * 실제 컬럼명이 car_type 이면 name만 car_type으로 바꾸면 됨
     */
    @Column(name = "type", length = 30)
    private String type;

    @Column(name = "asiel") // 사용 / todo: asiel 변경 / 스태커크레인 현재 위치
    private int asiel;

    @Column(name = "bay") // 사용 / 스태커크레인 현재 위치
    private int bay;

    @Column(name = "level") // 사용 / 스태커크레인 현재 위치
    private int level;

    @Column(name = "rack_id", length = 50) // 사용 / todo: 구조확인 BayLevel 형식인지 (MovecCraneOrderService.isLoadTarget())
    private String rackId;

    @Column(name = "rack_eq_id", length = 50) // 사용 / 랙에 해당하는 설비번호
    private String rackEqId;

    @Column(name = "auto_yn") // 사용
    private boolean autoYn;

    @Column(name = "plc_cmd_id") // 사용
    private int plcCmdId;

    @Column(name = "plc_comp_cmd_id") // 사용
    private int plcCompCmdId;

    @Column(name = "status") // 사용 / EqCraneStatus
    private int status;

    @Column(name = "cargo_yn") // 사용
    private boolean cargoYn;

    @Column(name = "req_move_home_yn")
    private boolean reqMoveHomeYn;

    @Column(name = "min_asiel") // todo: min_row 변경. min_bay, min_level 로 변경
    private int minAsiel;

    @Column(name = "max_asiel") // todo: max_row 변경. max_bay, max_level 로 변경
    private int maxAsiel;

    @Column(name = "error_id", length = 50)
    private String errorId;

    @Column(name = "error_desc", length = 500)
    private String errorDesc;

    @Column(name = "use_yn")
    private boolean useYn;

    @Column(name = "complete_yn") // 사용
    private boolean completeYn;

    @Column(name = "origin_level")
    private int originLevel;
}