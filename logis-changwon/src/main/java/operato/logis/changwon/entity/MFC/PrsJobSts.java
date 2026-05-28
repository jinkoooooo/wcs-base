package operato.logis.changwon.entity.MFC;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

@Getter
@Setter
@Table(name = "prs_job_sts", idStrategy = GenerationRule.UUID)
public class PrsJobSts extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    @Column(name = "machine_id", nullable = false)
    private Integer machineId;

    @Column(name = "job_no", nullable = false)
    private Integer jobNo;

    @Column(name = "job_phase", nullable = false)
    private Integer jobPhase;

    @Column(name = "tier", nullable = false)
    private Integer tier;

    @Column(name = "bay", nullable = false)
    private Integer bay;

    @Column(name = "bank", nullable = false)
    private Integer bank;

    @Column(name = "x_act_pos", nullable = false)
    private Integer xActPos;

    @Column(name = "mode_status", nullable = false)
    private Integer modeStatus;

    @Column(name = "obstacle_chk")
    private Integer obstacleChk;

    @Column(name = "load_chk", nullable = false)
    private Integer loadChk;

    @Column(name = "job_status", nullable = false)
    private Integer jobStatus;

    @Column(name = "status_send", nullable = false)
    private Integer statusSend;

    @Column(name = "error_code", nullable = false)
    private Integer errorCode;

    @Column(name = "battery", nullable = false)
    private Integer battery;

    @Column(name = "pallet_id", length = 20)
    private String palletId;

    @Column(name = "order_id", length = 20)
    private String orderId;

    @Column(name = "wms_ord_no", length = 40)
    private String wmsOrdNo;
}
