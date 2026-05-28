package operato.logis.inventory.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_inventory_setting", idStrategy = GenerationRule.UUID)
public class TbInventorySetting extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "option_value", nullable = false, length = 100)
    private String optionValue;

    @Column(name = "option_description", nullable = false, length = 100)
    private String optionDescription;
}