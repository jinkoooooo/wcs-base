package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * host_order 상태 전이 이력 엔티티.
 * REQUIRES_NEW 로 저장되어 본 트랜잭션 롤백과 독립적이다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_host_order_history", idStrategy = GenerationRule.UUID)
public class TbWcsHostOrderHistory extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "host_system_code", length = 50, nullable = false)
    private String hostSystemCode;

    @Column(name = "host_order_key", length = 64, nullable = false)
    private String hostOrderKey;

    @Column(name = "eq_group_id", length = 64)
    private String eqGroupId;

    @Column(name = "from_status")
    private Integer fromStatus;

    @Column(name = "to_status", nullable = false)
    private Integer toStatus;

    /** CREATED/TEST_PASSED/TEST_FAILED/TEST_RETRY/CANCELLED/SCHEDULE_DUE/COMPLETED/BCR_RELEASED/BOX_ADJUSTED/QC_RELEASED */
    @Column(name = "event_type", length = 30, nullable = false)
    private String eventType;

    @Column(name = "operator", length = 50)
    private String operator;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "detail_json")
    private String detailJson;
}
