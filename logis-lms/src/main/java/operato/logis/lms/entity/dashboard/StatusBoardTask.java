package operato.logis.lms.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "status_board_task", idStrategy = GenerationRule.UUID)
public class StatusBoardTask extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 40)
    private String lcId;

    @Column(name = "task_id", length = 100)
    private String taskId;

    @Column(name = "command_type", length = 20)
    private String commandType;

    @Column(name = "start_point_cd", length = 40)
    private String startPointCd;

    @Column(name = "end_point_cd", length = 40)
    private String endPointCd;
}
