package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.time.OffsetDateTime;

@Getter
@Setter
@Table(name = "tb_wcs_host_order", idStrategy = GenerationRule.UUID,
        uniqueFields = "hostOrderKey",
        indexes = {
                @Index(name = "ux_tb_wcs_host_order_key", columnList = "host_order_key", unique = true),
        })
public class TbWcsHostOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "host_system_code", length = 20)
    private String hostSystemCode;

    @Column(name = "host_order_key", length = 64)
    private String hostOrderKey;

    @Column(name = "eq_group_id", length = 64)
    private String eqGroupId;

    @Column(name = "order_type", length = 16)
    private String orderType;

    @Column(name = "order_status")
    private int orderStatus;

    @Column(name = "priority")
    private int priority;

    @Column(name = "owner_code", length = 20)
    private String ownerCode;

    @Column(name = "request_port_code", length = 64)
    private String requestPortCode;

    @Column(name = "wcs_order_key", length = 64)
    private String wcsOrderKey;

    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "error_code", length = 32)
    private String errorCode;

    @Column(name = "error_desc", length = 4000)
    private String errorDesc;
}