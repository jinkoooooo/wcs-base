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
@Table(name = "tb_inventory_item_group", idStrategy = GenerationRule.UUID)
public class TbInventoryItemGroup extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "item_group", nullable = false, length = 20)
    private String itemGroup;

    @Column(name = "item_owner", nullable = false, length = 100)
    private String itemOwner;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "restriction_type", nullable = false, length = 10)
    private String restrictionType;

    @Column(name = "dedicated_priority")
    private Integer dedicatedPriority;
}