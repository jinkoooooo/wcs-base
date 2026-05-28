package operato.logis.samsung.entity.buffer;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.buffer.relation.TbMwBufferStorageAreaRef;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * 작업관리 - 시퀀스버퍼 상세 작업 관리
 */
@Getter
@Setter
@Table(name = "tb_mw_buffer_task_detail", idStrategy = GenerationRule.UUID)
public class TbMwBufferTaskDetail extends ElidomStampHook {

    // todo: 작업 상세 번호 생성 로직 추가

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "parent_task_id", length = OrmConstants.FIELD_SIZE_UUID)
    private String parentTaskId; // FK: 작업번호

    @Relation(field = "parentTaskId")
    private TbMwBufferTask parentTask;

    @Column(name = "step_no")
    private Integer stepNo; // 작업 순번

    @Column(name = "item_code", length = 100)
    private String itemCode; // 88바코드 (tb_mw_box의 item_code, tb_mw_item_master의 inner_item_code와 동일)

    @Column(name = "target_qty")
    private Integer targetQty; // area 내 목표 작업 수량

    @Column(name = "current_qty")
    private Integer currentQty; // area 내 현재 작업 수량

    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType; // 작업 유형

    @Column(name = "task_status", nullable = false)
    private Integer taskStatus; // 작업 상태

    @Column(name = "task_result")
    private Integer taskResult; // 작업 결과

    @Column(name = "started_at", type = ColumnType.DATETIME)
    private Date startedAt; // 작업 시작일시

    @Column(name = "ended_at", type = ColumnType.DATETIME)
    private Date endedAt; // 작업 완료일시

    @Column(name = "from_area_id", length = OrmConstants.FIELD_SIZE_UUID)
    private String fromAreaId; // FK: Area Id

    @Relation(field = "fromAreaId")
    private TbMwBufferStorageAreaRef fromAreaRef;

    @Column(name = "from_aisle_no")
    private Integer fromAisleNo; // 1~3

    @Column(name = "from_level_no")
    private Integer fromLevelNo; // 1~7

    @Column(name = "to_area_id", length = OrmConstants.FIELD_SIZE_UUID)
    private String toAreaId; // FK: Area Id

    @Relation(field = "toAreaId")
    private TbMwBufferStorageAreaRef toAreaRef;

    @Column(name = "to_aisle_no")
    private Integer toAisleNo; // 1~3

    @Column(name = "to_level_no")
    private Integer toLevelNo; // 1~7
}