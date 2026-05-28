package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * WCS 주문 처리 공통 Command (내부 DTO). 입고/출고/이동 공용.
 *
 * fromLocId/toLocId 의미:
 *  - INBOUND  : from=입고 포트, to=적치 위치(없으면 allocator 산출)
 *  - OUTBOUND : from=출고 시작(없으면 allocator 산출), to=출고 포트(없으면 기본 포트)
 *  - MOVE     : from=이동 시작, to=이동 목적지(없으면 allocator 산출)
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class WcsOrderCommand {

    @JsonProperty("hostSystemCode")
    private final String hostSystemCode;

    @JsonProperty("hostOrderKey")
    private final String hostOrderKey;

    @JsonProperty("wcsOrderKey")
    private String wcsOrderKey;

    @JsonProperty("parentOrderKey")
    private String parentOrderKey;

    @JsonProperty("subOrderType")
    private String subOrderType;

    @JsonProperty("orderType")
    private final String orderType;

    @JsonProperty("ownerCode")
    private final String ownerCode;

    @JsonProperty("priority")
    private final Integer priority;

    @JsonProperty("eqGroupId")
    private final String eqGroupId;

    @JsonProperty("fromLocId")
    private final String fromLocId;

    @JsonProperty("toLocId")
    private final String toLocId;

    @JsonProperty("barCode")
    private final String barCode;

    @JsonProperty("rawPayload")
    private final String rawPayload;

    @JsonProperty("persistHostOrder")
    private boolean persistHostOrder;

    @JsonProperty("isObstacleCalculate")
    @Builder.Default
    private boolean isObstacleCalculate = false;

    @JsonProperty("items")
    @Builder.Default
    private final List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    public static class Item {

        @JsonProperty("itemCode")
        private String itemCode;

        @JsonProperty("lotNo")
        private String lotNo;

        @JsonProperty("qty")
        private Integer qty;

        @JsonProperty("uom")
        private String uom;

        @JsonProperty("issueQty")
        private Integer issueQty;

        @JsonProperty("issueUom")
        private String issueUom;

        @JsonProperty("produceDate")
        private Date produceDate;

        @JsonProperty("expiryDate")
        private Date expiryDate;

        @JsonProperty("rawAttr")
        private String rawAttr;

        @JsonProperty("testRequired")
        private Boolean testRequired;

        @JsonProperty("testRequestNo")
        private String testRequestNo;

        @JsonProperty("testNo")
        private String testNo;

        /** 시험 상태 스냅샷 (host_order_item.test_status — 산출 시점 값) */
        @JsonProperty("testStatus")
        private String testStatus;
    }
}
