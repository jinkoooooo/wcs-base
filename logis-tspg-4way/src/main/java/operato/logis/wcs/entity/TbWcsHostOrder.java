package operato.logis.wcs.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * HOST(WMS) 수신 주문 헤더 엔티티.
 * (host_order_key, eq_group_id) 단위 1건이며 시험·산출 진행 상태를 보유한다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_host_order", idStrategy = GenerationRule.UUID,
        uniqueFields = "hostOrderKey,eqGroupId",
        indexes = {
                @Index(name = "ux_tb_wcs_host_order_key", columnList = "host_order_key,eq_group_id", unique = true)
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

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "from_loc_code", nullable = true, length = 50)
    private String fromLocCode;

    @Column(name = "to_loc_code", nullable = true, length = 50)
    private String toLocCode;

    @Column(name = "priority")
    private int priority;

    @Column(name = "owner_code", length = 20)
    private String ownerCode;

    @Column(name = "wcs_order_key", length = 64)
    private String wcsOrderKey;

    @Column(name = "requested_at", type = ColumnType.DATETIME)
    private Date requestedAt;

    @Column(name = "received_at", type = ColumnType.DATETIME)
    private Date receivedAt;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "error_code", length = 32)
    private String errorCode;

    @Column(name = "error_desc", length = 4000)
    private String errorDesc;

    /**
     * 출고 예정일 — null/오늘이면 즉시 산출. 미래면 WAITING_SCHEDULE.
     *
     * 타입을 java.util.Date 로 두는 이유: next-dbist ORM 이 LocalDate ResultSet 매핑 미지원.
     * Service 경계에서 LocalDateUtils 로 LocalDate ↔ Date 변환.
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "scheduled_date", type = ColumnType.DATETIME)
    private Date scheduledDate;

    /** 입고 예정(tb_wcs_inbound_plan.id) 연계 키 — 입고 예정 기반 등록 시 설정, 그 외 null. */
    @Column(name = "parent_host_order_key", length = 64)
    private String parentHostOrderKey;

    /**
     * 시험 대상 입고 여부 — HOST(WMS) 가 지정.
     */
    @Column(name = "test_required")
    private Boolean testRequired;

    @Column(name = "nia_required")
    private Boolean niaRequired;

    /**
     * 시험 진행 상태(헤더 집계 캐시).
     */
    @Column(name = "test_status", length = 20)
    private String testStatus;

    /** 시험 의뢰 시점 (헤더 집계 — 첫 requestTest 시점) */
    @Column(name = "test_requested_at", type = ColumnType.DATETIME)
    private Date testRequestedAt;

    /** 시험 결과 도착 시점 (헤더 집계 — 모든 item 결과가 도착한 마지막 시점) */
    @Column(name = "test_resulted_at", type = ColumnType.DATETIME)
    private Date testResultedAt;

    /** 시험 결과 사유 / 실패 비고 (헤더 집계 — FAILED 시 첫 실패 item 사유) */
    @Column(name = "test_reason", length = 500)
    private String testReason;
}