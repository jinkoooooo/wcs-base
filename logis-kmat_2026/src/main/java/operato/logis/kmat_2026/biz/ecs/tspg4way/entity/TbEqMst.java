package operato.logis.kmat_2026.biz.ecs.tspg4way.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_eq_mst", idStrategy = GenerationRule.UUID, uniqueFields = "eqGroupId,id", indexes = {
        @Index(name = "ix_tb_eq_mst_index_0", columnList = "eq_group_id,id", unique = true)
})
public class TbEqMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    /**
     * 설비 그룹 ID (tb_eq_group_mst.id)
     */
    @JsonProperty("eqGroupId")
    @Column(name = "eq_group_id", length = 50)
    private String eqGroupId;

    /**
     * 설비명 (화면 표시용)
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 설비 타입 (SHUTTLE, LIFTER, CONVEYOR 등)
     */
    @Column(name = "type", length = 30)
    private int type;

    /**
     * PLC ID (실제 PLC 매핑 키)
     */
    @JsonProperty("plcId")
    @Column(name = "plc_id", length = 50)
    private String plcId;
}
