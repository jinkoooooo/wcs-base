package operato.logis.samsung.entity.buffer.relation;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.io.Serializable;

@Getter
@Setter
@Table(name = "tb_mw_item_master", idStrategy = GenerationRule.UUID, isRef = true)
public class TbMwItemMasterRef implements Serializable {

    private static final long serialVersionUID = 5522200852930742111L;

    @PrimaryKey
    private String id;
}