package operato.logis.samsung.entity.buffer;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.buffer.relation.TbMwBufferStorageAreaRef;
import operato.logis.samsung.entity.buffer.relation.TbMwItemMasterRef;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainUpdateStampHook;

/**
 * 재고관리 - 시퀀스버퍼 창고 컨베이어 할당 SKU 관리
 */
@Getter
@Setter
@Table(name = "tb_mw_buffer_storage_allocation", idStrategy = GenerationRule.UUID)
public class TbMwBufferStorageAllocation extends DomainUpdateStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "area_id", length = 10)
    private String areaId; // FK : area Id

    @Relation(field = "areaId")
    private TbMwBufferStorageAreaRef area;

    @Column(name = "item_id", length = 40)
    private String itemId; // FK : area에 할당된 재고종류

    @Relation(field = "itemId")
    private TbMwItemMasterRef itemMaster;

    @Column(name = "target_grade")
    private Integer targetGrade; // area 대상 SKU 등급

    @Column(name = "allocated_qty", nullable = false)
    private Integer allocatedQty; // area에 현재 할당 된 재고 수 (입고, 재정리 예정 수량. 실 재고 수와 상이)

/*    public void setArea(TbMwBufferStorageAreaRef area) {
        this.area = area;
        if (this.area != null) {
            String refId = this.area.getId();
            if (refId != null) {
                this.areaId = refId;
            }
        }
        else {
            this.areaId = SysConstants.EMPTY_STRING;
        }
    }
*/
}