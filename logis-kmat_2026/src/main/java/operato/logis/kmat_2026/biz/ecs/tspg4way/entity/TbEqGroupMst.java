package operato.logis.kmat_2026.biz.ecs.tspg4way.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_eq_group_mst", idStrategy = GenerationRule.UUID)
public class TbEqGroupMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", length = 100)
    private String name;

    /**
     * 그룹 타입(예: TSPG_AMBIENT 등). 실제 값 길이에 맞춰 조정 가능
     */
    @Column(name = "type", length = 30)
    private String type;
}
