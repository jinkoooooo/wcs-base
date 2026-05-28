package operato.logis.wcs.entity;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * 시뮬레이터 가동 상태 — eq_group_id 별 한 행.
 * 다중 그룹 동시 가동을 지원한다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_simulator_state", idStrategy = GenerationRule.UUID,
        uniqueFields = "eqGroupId")
public class TbWcsSimulatorState extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "eq_group_id", length = 50, nullable = false)
    private String eqGroupId;

    @Column(name = "run", nullable = true)
    private Boolean run = false;

    @Column(name = "host_running", nullable = false)
    private Boolean hostRunning;

    @Column(name = "plc_running", nullable = false)
    private Boolean plcRunning;

    @Column(name = "port_mode_running", nullable = false)
    private Boolean portModeRunning;

    @Column(name = "current_phase", length = 20)
    private String currentPhase;

    @Column(name = "phase_started_at", type = ColumnType.DATETIME)
    private Date phaseStartedAt;

    @Column(name = "target_inbound")
    private Integer targetInbound;

    @Column(name = "target_outbound")
    private Integer targetOutbound;

    @Column(name = "target_move")
    private Integer targetMove;
}