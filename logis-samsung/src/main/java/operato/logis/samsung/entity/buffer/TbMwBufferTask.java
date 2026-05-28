package operato.logis.samsung.entity.buffer;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * 작업관리 - 시퀀스버퍼 입고/출고/재배치 관리
 */
@Getter
@Setter
@Table(name = "tb_mw_buffer_task", idStrategy = GenerationRule.UUID)
public class TbMwBufferTask extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "task_id", length = OrmConstants.FIELD_SIZE_MEANINGFUL_ID)
    private String taskId; // 작업 ID

    @Column(name = "priority")
    private Integer priority; // 작업 우선순위

    @Column(name = "item_code", length = 100)
    private String itemCode; // 88바코드 (tb_mw_box의 item_code, tb_mw_item_master의 inner_item_code와 동일)

    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType; // 작업 유형

    @Column(name = "task_mode", nullable = false, length = 1)
    private String taskMode; // 작업 모드

    @Column(name = "task_status", nullable = false)
    private Integer taskStatus; // 작업 상태

    @Column(name = "task_result")
    private Integer taskResult; // 작업 결과

    @Column(name = "started_at", type = ColumnType.DATETIME)
    private Date startedAT; // 작업 시작 일시

    @Column(name = "ended_at", type = ColumnType.DATETIME)
    private Date endedAT; // 작업 완료 일시
}