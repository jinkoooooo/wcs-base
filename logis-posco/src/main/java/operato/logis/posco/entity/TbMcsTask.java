package operato.logis.posco.entity;

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
@Table(name = "tb_mcs_task", idStrategy = GenerationRule.UUID)
public class TbMcsTask extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "task_id", length = 40)
    private String taskId;

    @Column(name = "task_type", length = 20)
    private String taskType;

    @Column(name = "task_priority")
    private Integer taskPriority;

    @Column(name = "stock_id", length = 40)
    private String stockId;

    @Column(name = "pod_code", length = 40)
    private String podCode;

    @Column(name = "pod_type", length = 20)
    private String podType;

    @Column(name = "start_point_cd", length = 40)
    private String startPointCd;

    @Column(name = "end_point_cd", length = 40)
    private String endPointCd;

    @Column(name = "process_status")
    private Integer processStatus;

    @Column(name = "accept_datetime")
    private Date acceptDatetime;

    @Column(name = "start_datetime")
    private Date startDatetime;

    @Column(name = "loading_datetime")
    private Date loadingDatetime;

    @Column(name = "complete_datetime")
    private Date completeDatetime;

    @Column(name = "equip_type", length = 20)
    private String equipType;

    @Column(name = "equip_code", length = 40)
    private String equipCode;

    @Column(name = "error_code", length = 20)
    private String errorCode;

    @Column(name = "error_msg")
    private String errorMsg;
}