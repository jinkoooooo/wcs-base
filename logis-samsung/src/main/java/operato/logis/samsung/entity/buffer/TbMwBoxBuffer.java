package operato.logis.samsung.entity.buffer;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.buffer.relation.TbMwBufferStorageAreaRef;
import operato.logis.samsung.entity.buffer.relation.TbMwItemMasterRef;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * 재고관리 - 시퀀스버퍼 재고 현황 관리
 */
@Getter
@Setter
@Table(name = "tb_mw_box_buffer", idStrategy = GenerationRule.UUID)
public class TbMwBoxBuffer extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "box_id", length = 100)
    private String boxId; // 재고 시리얼 (tb_mw_box의 box_id와 동일)

    @Column(name = "item_code", length = 100)
    private String itemCode; // 88바코드 (tb_mw_box의 item_code, tb_mw_item_master의 inner_item_code와 동일)

    @Column(name = "item_name", length = 100)
    private String itemName; // 재고 명 (tb_mw_box의 item_name과 동일)

    @Column(name = "priority")
    private Integer priority; // 출고 우선순위

    @Column(name = "stock_status", nullable = false)
    private Integer stockStatus; // 재고 상태

    @Column(name = "area_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String areaId; // FK : area Id

    @Relation(field = "areaId")
    private TbMwBufferStorageAreaRef area;

    @Column(name = "area_col")
    private Integer areaCol; // 현재 보관 위치 (열)

    @Column(name = "task_detail_id", length = OrmConstants.FIELD_SIZE_UUID)
    private String taskDetailId; // 현재 할당된 작업 번호

    @Relation(field = "taskDetailId")
    private TbMwItemMasterRef taskDetail;

    @Column(name = "stored_at", type = ColumnType.DATETIME)
    private Date storedAt; // 입고 일시

    @Column(name = "picked_at", type = ColumnType.DATETIME)
    private Date pickedAt; // 출고 일시
}