package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * ========================================================================
 * ECS 콜백 응답 DTO
 * ========================================================================
 *
 * [목적]
 * - ECS → WCS 콜백 처리 결과를 ECS 측으로 응답
 * - REST API 응답 바디로 사용
 *
 * [성공 응답 예시]
 * {
 *   "success": true,
 *   "orderKey": "INB-1704067200000-abc12345",
 *   "message": "Callback processed successfully"
 * }
 *
 * [실패 응답 예시]
 * {
 *   "success": false,
 *   "orderKey": "INB-1704067200000-abc12345",
 *   "errorCode": "ERR_BAD_REQUEST",
 *   "errorDesc": "Unknown status: XXX",
 *   "message": "Callback processing failed"
 * }
 * ========================================================================
 */
@Getter
@Builder
public class EcsCallbackResponse {

    // ========================================================================
    // 공통 필드
    // ========================================================================

    /**
     * 처리 성공 여부
     * - true: 콜백이 정상적으로 처리됨
     * - false: 콜백 처리 중 오류 발생
     */
    private boolean success;

    /**
     * 주문 키
     * - ECS가 통보한 주문 키
     */
    private String orderKey;

    /**
     * 안내 메시지
     * - 사람이 읽을 수 있는 처리 결과 메시지
     */
    private String message;

    // ========================================================================
    // 실패 시 반환 필드
    // ========================================================================

    /**
     * 에러 코드
     * - 실패 시에만 세팅
     * - 예: ERR_INTERNAL, ERR_BAD_REQUEST ...
     */
    private String errorCode;

    /**
     * 에러 상세 설명
     * - 실패 시에만 세팅
     */
    private String errorDesc;

    // ========================================================================
    // 정적 팩토리 메서드
    // ========================================================================

    /**
     * 성공 응답 생성
     *
     * @param orderKey 주문 키
     * @return 성공 응답
     */
    public static EcsCallbackResponse success(String orderKey) {
        return EcsCallbackResponse.builder()
                .success(true)
                .orderKey(orderKey)
                .message("Callback processed successfully")
                .build();
    }

    /**
     * 성공 응답 생성 (메시지 지정)
     *
     * @param orderKey 주문 키
     * @param message  메시지
     * @return 성공 응답
     */
    public static EcsCallbackResponse success(String orderKey, String message) {
        return EcsCallbackResponse.builder()
                .success(true)
                .orderKey(orderKey)
                .message(message)
                .build();
    }

    /**
     * 실패 응답 생성 (기본)
     *
     * @param errorCode 에러 코드
     * @param errorDesc 에러 상세
     * @return 실패 응답
     */
    public static EcsCallbackResponse fail(String errorCode, String errorDesc) {
        return EcsCallbackResponse.builder()
                .success(false)
                .message("Callback processing failed")
                .errorCode(errorCode)
                .errorDesc(errorDesc)
                .build();
    }

    /**
     * 실패 응답 생성 (orderKey 포함)
     *
     * @param orderKey  주문 키
     * @param errorCode 에러 코드
     * @param errorDesc 에러 상세
     * @return 실패 응답
     */
    public static EcsCallbackResponse fail(String orderKey, String errorCode, String errorDesc) {
        return EcsCallbackResponse.builder()
                .success(false)
                .orderKey(orderKey)
                .message("Callback processing failed")
                .errorCode(errorCode)
                .errorDesc(errorDesc)
                .build();
    }
}