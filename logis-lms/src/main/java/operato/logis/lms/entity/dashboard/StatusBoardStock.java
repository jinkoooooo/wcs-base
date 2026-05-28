package operato.logis.lms.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "status_board_stock", idStrategy = GenerationRule.UUID)
public class StatusBoardStock extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 40)
    private String lcId;

    @Column(name = "stock_id", length = 100)
    private String stockId;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(name = "item_qty")
    private Integer itemQty;
}
