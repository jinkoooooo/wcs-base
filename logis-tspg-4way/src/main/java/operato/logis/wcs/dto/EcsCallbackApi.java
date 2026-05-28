package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import operato.logis.wcs.consts.EcsCallbackStatus;

/**
 * ECS ↔ WCS 콜백 REST 인터페이스 모음.
 * JSON 필드명은 As-Is(EcsCallbackRequest / EcsCallbackResponse / InternalEcsCallbackRequest)와 동일.
 */
public final class EcsCallbackApi {

    private EcsCallbackApi() {}

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @JsonProperty("orderKey")
        private String orderKey;

        @JsonProperty("status")
        private String status;

        @JsonProperty("message")
        private String message;

        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("payload")
        private String payload;

        @Setter
        @JsonProperty("extra")
        private String extra;

        @Setter
        @JsonProperty("type")
        private String type;

        /** status 문자열을 enum 으로 변환. */
        public EcsCallbackStatus getStatusEnum() {
            return EcsCallbackStatus.from(this.status);
        }

        // status 판정 — EcsCallbackStatus 에 위임
        public boolean isComplete()   { return EcsCallbackStatus.isComplete(this.status); }
        public boolean isError()      { return EcsCallbackStatus.isError(this.status); }
        public boolean isStarted()    { return EcsCallbackStatus.isStarted(this.status); }
        public boolean isCancelled()  { return EcsCallbackStatus.isCancelled(this.status); }
        public boolean isInProgress() { return EcsCallbackStatus.isInProgress(this.status); }
        public boolean isAccepted()   { return EcsCallbackStatus.isAccepted(this.status); }
    }

    @Getter
    @Builder
    public static class Response {

        private boolean success;
        private String orderKey;
        private String message;
        private String errorCode;
        private String errorDesc;

        public static Response success(String orderKey) {
            return Response.builder()
                    .success(true)
                    .orderKey(orderKey)
                    .message("Callback processed successfully")
                    .build();
        }

        public static Response success(String orderKey, String message) {
            return Response.builder()
                    .success(true)
                    .orderKey(orderKey)
                    .message(message)
                    .build();
        }

        public static Response fail(String errorCode, String errorDesc) {
            return Response.builder()
                    .success(false)
                    .message("Callback processing failed")
                    .errorCode(errorCode)
                    .errorDesc(errorDesc)
                    .build();
        }

        public static Response fail(String orderKey, String errorCode, String errorDesc) {
            return Response.builder()
                    .success(false)
                    .orderKey(orderKey)
                    .message("Callback processing failed")
                    .errorCode(errorCode)
                    .errorDesc(errorDesc)
                    .build();
        }
    }

    /** 내부 테스트용 — REST 입력 DTO. */
    @Getter
    @Setter
    public static class InternalRequest {

        @JsonProperty("orderKey")
        private String orderKey;

        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("message")
        private String message;
    }
}
