package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * HOST 주문 수신/생성 REST 인터페이스 모음.
 * JSON 필드명은 As-Is(HostOrderReceiveRequest / HostOrderCreateRequest / HostOrderReceiveResponse)와 동일.
 */
public final class HostOrderApi {

    private HostOrderApi() {}

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @JsonProperty("hostSystemCode")
        private String hostSystemCode;

        @JsonProperty("hostOrderKey")
        private String hostOrderKey;

        @JsonProperty("orderType")
        private String orderType;

        @JsonProperty("ownerCode")
        private String ownerCode;

        @JsonProperty("eqGroupId")
        private String eqGroupId;

        @JsonProperty("fromLocId")
        private String fromLocId;

        @JsonProperty("toLocId")
        private String toLocId;

        @JsonProperty("barcode")
        private String barcode;

        private Integer priority;

        private String rawPayload;

        @JsonProperty("testRequired")
        private Boolean testRequired;

        @JsonProperty("testRequestNo")
        private String testRequestNo;

        @JsonProperty("testNo")
        private String testNo;

        @JsonProperty("niaRequired")
        private Boolean niaRequired;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        @JsonProperty("scheduledDate")
        private LocalDate scheduledDate;

        @JsonProperty("items")
        private List<Item> items;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @JsonProperty("lineNo")
        private Integer lineNo;

        @JsonProperty("itemCode")
        private String itemCode;

        @JsonProperty("lotNo")
        private String lotNo;

        @JsonProperty("qty")
        private Integer qty;

        @JsonProperty("uom")
        private String uom;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        @JsonProperty("produceDate")
        private Date produceDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        @JsonProperty("expiryDate")
        private Date expiryDate;

        @JsonProperty("testRequired")
        private Boolean testRequired;

        @JsonProperty("testRequestNo")
        private String testRequestNo;

        @JsonProperty("testNo")
        private String testNo;

        @JsonProperty("niaRequired")
        private Boolean niaRequired;

        @JsonProperty("rawAttr")
        private String rawAttr;
    }

    @Getter
    @Builder
    public static class Response {

        private boolean success;
        private String hostOrderKey;
        private String message;
        private String wcsOrderKey;
        private String errorCode;
        private String errorDesc;

        public static Response success(String wcsOrderKey, String hostOrderKey) {
            return Response.builder()
                    .success(true)
                    .wcsOrderKey(wcsOrderKey)
                    .hostOrderKey(hostOrderKey)
                    .message("Order received and processed successfully")
                    .build();
        }

        public static Response fail(String hostOrderKey, String errorCode, String errorDesc) {
            return Response.builder()
                    .success(false)
                    .hostOrderKey(hostOrderKey)
                    .errorCode(errorCode)
                    .errorDesc(errorDesc)
                    .message("Order processing failed")
                    .build();
        }

        public static Response duplicate(String wcsOrderKey, String hostOrderKey) {
            return Response.builder()
                    .success(true)
                    .wcsOrderKey(wcsOrderKey)
                    .hostOrderKey(hostOrderKey)
                    .message("Order already exists (idempotent)")
                    .build();
        }
    }
}
