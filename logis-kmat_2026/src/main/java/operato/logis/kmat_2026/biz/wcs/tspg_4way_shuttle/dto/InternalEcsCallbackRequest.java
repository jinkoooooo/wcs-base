package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * ====================================================================
 * Internal ECS Callback Request DTO
 * ====================================================================
 *
 * [용도]
 * - 내부 ECS 콜백 테스트용 REST API 요청 DTO
 *
 * [예시]
 * {
 *   "orderKey": "MOV-20260313212803-5f",
 *   "errorCode": "ECS_ERR",
 *   "message": "equipment failure"
 * }
 */
@Getter
@Setter
public class InternalEcsCallbackRequest {

    /** 대상 WCS orderKey */
    @JsonProperty("orderKey")
    private String orderKey;

    /** error 호출 시 사용 */
    @JsonProperty("errorCode")
    private String errorCode;

    /** error 호출 시 사용 */
    @JsonProperty("message")
    private String message;
}