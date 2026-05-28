package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsCallbackStatusEnumCode;

/**
 * ========================================================================
 * ECS 콜백 요청 DTO
 * ========================================================================
 *
 * [목적]
 * - ECS(Equipment Control System)에서 WCS로 작업 상태를 통보할 때 사용되는 요청 데이터 구조
 *
 * [상태 관리 원칙]
 * - 상태 문자열 상수는 DTO에 두지 않는다.
 * - 상태 정의는 EcsCallbackStatusEnumCode 하나로 관리한다.
 *
 * [상태 예시]
 * - ACCEPTED
 * - STARTED
 * - IN_PROGRESS
 * - COMPLETE
 * - ERROR
 * - CANCELLED
 *
 * [호환 상태]
 * - COMPLETED → COMPLETE
 * - FAILED → ERROR
 *
 * ========================================================================
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcsCallbackRequest {

    /**
     * 주문 키
     * - WCS에서 생성하여 ECS로 전달한 셔틀 주문 키
     */
    @JsonProperty("orderKey")
    private String orderKey;

    /**
     * 작업 상태
     * - 실제 값은 EcsCallbackStatusEnumCode 기준
     */
    @JsonProperty("status")
    private String status;

    /**
     * 상태 메시지
     */
    @JsonProperty("message")
    private String message;

    /**
     * 에러 코드
     */
    @JsonProperty("errorCode")
    private String errorCode;

    /**
     * 추가 데이터 (JSON 문자열)
     */
    @JsonProperty("payload")
    private String payload;

    /**
     * 일부 상황에서 값 변경 필요할 수 있어 Setter 허용
     */
    @Setter
    @JsonProperty("extra")
    private String extra;

    /**
     * 상태 Enum 반환
     */
    public EcsCallbackStatusEnumCode getStatusEnum() {
        return EcsCallbackStatusEnumCode.from(this.status);
    }

    public boolean isComplete() {
        return EcsCallbackStatusEnumCode.isComplete(this.status);
    }

    public boolean isError() {
        return EcsCallbackStatusEnumCode.isError(this.status);
    }

    public boolean isStarted() {
        return EcsCallbackStatusEnumCode.isStarted(this.status);
    }

    public boolean isCancelled() {
        return EcsCallbackStatusEnumCode.isCancelled(this.status);
    }

    public boolean isInProgress() {
        return EcsCallbackStatusEnumCode.isInProgress(this.status);
    }

    public boolean isAccepted() {
        return EcsCallbackStatusEnumCode.isAccepted(this.status);
    }
}