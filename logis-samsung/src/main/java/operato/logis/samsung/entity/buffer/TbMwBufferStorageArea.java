package operato.logis.samsung.entity.buffer;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.DomainUpdateStampHook;

/**
 * 재고관리 - 시퀀스버퍼 창고 컨베이어 상태 관리
 */
@Getter
@Setter
@Table(name = "tb_mw_buffer_storage_area", idStrategy = GenerationRule.MEANINGFUL, meaningfulFields = "aisle_no,level_no")
public class TbMwBufferStorageArea extends DomainUpdateStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 10)
    private String id;

    @Column(name = "aisle_no", nullable = false)
    private Integer aisleNo; // 1~3

    @Column(name = "level_no", nullable = false)
    private Integer levelNo; // 1~7

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty; // area 내 실 재고 수

    @Column(name = "sku_cnt", nullable = false)
    private Integer maxSkuCnt; // area 내 최대 SKU 종류

    @Column(name = "inspection_yn", nullable = false, length = 1)
    private String maintYn; // area 점검 상태 (Y/N)

    @Column(name = "operation_type", nullable = false, length = 1)
    private String operationType; // 작업모드
}