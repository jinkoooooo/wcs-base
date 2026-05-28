package operato.logis.simulator.asrs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_simulator_order", idStrategy = GenerationRule.UUID)
public class TbSimulatorAsrsOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "order_type", nullable = false)
    private Integer orderType;

    @Column(name = "src_row", nullable = false)
    private Integer srcRow;

    @Column(name = "src_bay", nullable = false)
    private Integer srcBay;

    @Column(name = "src_level", nullable = false)
    private Integer srcLevel;

    @Column(name = "dest_row", nullable = false)
    private Integer destRow;

    @Column(name = "dest_bay", nullable = false)
    private Integer destBay;

    @Column(name = "dest_level", nullable = false)
    private Integer destLevel;

    @Column(name = "order_status", nullable = false)
    private Integer orderStatus;

    @Column(name = "is_read", nullable = false)
    private Integer isRead;
}