package operato.logis.changwon.entity.WCS;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import operato.logis.changwon.WcsConstants;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "wcs_task", idStrategy = GenerationRule.UUID)
public class WcsTask extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 20)
    private String lcId;

    @Column(name = "order_kind", length = 2)
    private String orderKind;

    @Column(name = "task_priority", length = 4)
    private Integer taskPriority;

    @Column(name = "task_id", nullable = false, length = 12)
    private String taskId;

    @Column(name = "task_no", nullable = false, length = 30)
    private String taskNo;

    @Column(name = "order_id", length = 15)
    private String orderId;

    @Column(name = "ord_seq", length = 15)
    private String ordSeq;

    @Column(name = "cust_id", length = 20)
    private String custId;

    @Column(name = "stock_id", length = 40)
    private String stockId;

    @Column(name = "stock_type", length = 4)
    private String stockType;

    @Column(name = "item_code", length = 30)
    private String itemCode;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(name = "plan_qty", length = 4)
    private Integer planQty;

    @Column(name = "rslt_qty", length = 4)
    private Integer rsltQty;

    @Column(name = "ng_qty", length = 4)
    private Integer ngQty;

    @Column(name = "start_point_cd", nullable = false, length = 50)
    private String startPointCd;

    @Column(name = "end_point_cd", nullable = false, length = 50)
    private String endPointCd;

    @Column(name = "from_stock_area", length = 4)
    private Integer fromStockArea;

    @Column(name = "to_stock_area", length = 4)
    private Integer toStockArea;

    @Column(name = "pod_code", length = 20)
    private String podCode;

    @Column(name = "process_status", nullable = false, length = 4)
    private Integer processStatus;

    @Column(name = "accept_datetime", nullable = false)
    private Date acceptDatetime;

    @Column(name = "start_datetime")
    private Date startDatetime;

    @Column(name = "complete_datetime")
    private Date completeDatetime;

    @Column(name = "data_transmit_status", nullable = false, length = 4)
    private Integer dataTransmitStatus;

    @Column(name = "error_code", length = 4)
    private Integer errorCode;

    @Column(name = "error_msg")
    private String errorMsg;

    @Column(name = "next_task_id", length = 40)
    private String nextTaskId;

    @Column(name = "next_task_method", length = 20)
    private String nextTaskMethod;

    @Column(name = "round_no", length = 4)
    private Integer roundNo;

    @Column(name = "high_rank_task_id", length = 12)
    private String highRankTaskId;

    @Column(name = "high_rank_task_no", length = 30)
    private String highRankTaskNo;

    @Column(name = "attribute_a", length = 40)
    private String attributeA;

    @Column(name = "attribute_b", length = 40)
    private String attributeB;

    public WcsTask() {
        this.setDomainId(WcsConstants.DOMAIN_ID);
    }
}