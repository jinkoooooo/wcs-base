package operato.logis.ecs.base.ecs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

// 검토) 사용확인
@Getter
@Setter
@Table(name = "tb_eq_mst", idStrategy = GenerationRule.UUID, uniqueFields = "eqGroupId,id", indexes = {
        @Index(name = "ix_tb_eq_mst_index_0", columnList = "eq_group_id,id", unique = true)
})
public class TbEqMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    /** 설비 그룹 ID (tb_eq_group_mst.id) */
    @Column(name = "eq_group_id", length = 50) // 사용
    private String eqGroupId;

    /** 설비명 (화면 표시용) */
    @Column(name = "name", length = 100)
    private String name;

    /** 설비 타입 (CRANE, CONVEYOR 등) */
    @Column(name = "type", length = 30) // 사용
    private int type;

    /** PLC ID (실제 PLC 매핑 키) */
    @Column(name = "plc_id", length = 50)
    private String plcId;
}
