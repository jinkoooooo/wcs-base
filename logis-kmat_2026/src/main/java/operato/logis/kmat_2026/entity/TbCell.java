package operato.logis.kmat_2026.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_cell", idStrategy = GenerationRule.UUID)
public class TbCell extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id")
    private String id;

    @Column(name = "cell_id",  length = 50)
    private String cellId;

    @Column(name = "loc_x")
    private int locX;

    @Column(name = "loc_y")
    private int locY;
    
    @Column(name = "has_cargo")
    private boolean hasCargo;
}
