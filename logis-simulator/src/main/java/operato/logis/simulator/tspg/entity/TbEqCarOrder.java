package operato.logis.simulator.tspg.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_eq_car_order", idStrategy = GenerationRule.UUID)
public class TbEqCarOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "order_key", nullable = false)
    private String orderKey;

    @Column(name = "shuttle_car_id", nullable = false)
    private String shuttleCarId;

    @Column(name = "work_no", nullable = false)
    private String workNo;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "target_id", nullable = false)
    private Integer targetId;

    @Column(name = "target_bay", nullable = false)
    private Integer targetBay;

    @Column(name = "target_row", nullable = false)
    private Integer targetRow;

    @Column(name = "req_complete_reset", nullable = false)
    private Integer reqCompleteReset;

    @Column(name = "is_read", nullable = false)
    private Integer isRead;
}