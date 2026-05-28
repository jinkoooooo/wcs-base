package operato.logis.samsung.entity.mw;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

@Getter
@Setter
@Table(name = "tb_mw_xyz_order", idStrategy = GenerationRule.UUID)
public class TbMwXyzOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "order_id", nullable = false, length = 20)
    private String orderId;

    @Column(name = "task_no", nullable = false)
    private Integer taskNo;

    @Column(name = "start_point_cd", length = 20)
    private String startPointCd;

    @Column(name = "end_point_cd", length = 20)
    private String endPointCd;

    @Column(name = "target_num", nullable = false)
    private Integer targetNum;

    @Column(name = "pass_qty", nullable = false)
    private Integer passQty;

    @Column(name = "ng_qty", nullable = false)
    private Integer ngQty;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "accept_datetime", nullable = false)
    private Date acceptDatetime;

    @Column(name = "start_datetime")
    private Date startDatetime;

    @Column(name = "complete_datetime")
    private Date completeDatetime;

    @Column(name = "process_status", nullable = false)
    private Integer processStatus;

    @Column(name = "delivery_no", length = 100)
    private String deliveryNo;

    @Column(name = "cntr_no", length = 100)
    private String cntrNo;
}