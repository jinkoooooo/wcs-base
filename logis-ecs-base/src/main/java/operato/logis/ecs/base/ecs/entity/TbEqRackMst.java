package operato.logis.ecs.base.ecs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

// 검토중) 사용확인 - 랙의 상태관리
@Getter
@Setter
@Table(name = "tb_eq_rack_mst", idStrategy = GenerationRule.UUID, uniqueFields = "eqId,id", indexes = {
        @Index(name = "ix_tb_eq_rack_mst_index_0", columnList = "eq_id,id", unique = true)
})
public class TbEqRackMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "rack_id", length = 50) // 사용 / TbEcsRackOrder의 fromLocId, toLocId
    private String rackId;

    @Column(name = "eq_id", length = 50) // 사용
    private String eqId;

    @Column(name = "type") // 사용
    private int type;

    @Column(name = "asiel") // 사용 todo: asiel 변경
    private int asiel;

    @Column(name = "bay") // 사용
    private int bay;

    @Column(name = "level") // 사용
    private int level;

    @Column(name = "side", length = 1) // 추가
    private String side;

    @Column(name = "sku_id", length = 50)
    private String skuId;

    @Column(name = "sku_qty")
    private int skuQty;

    @Column(name = "status") // 사용 / EqRackStatus / 화물 적재 상태
    private int status;

    @Column(name = "error_id", length = 50)
    private String errorId;

    @Column(name = "error_desc", length = 400)
    private String errorDesc;

    @Column(name = "use_yn") // 사용 / 셀 활성화여
    private boolean useYn;

    @Column(name = "cargo_yn") // 사용 // todo: isCargoYn과 status의 Cargo상태 동일한 상태인지 확인 후, 컬럼 삭제 고려
    private boolean cargoYn;

    @Column(name = "buffer_yn")
    private boolean bufferYn;

    @Column(name = "drive_only_yn")
    private boolean driveOnlyYn;
}