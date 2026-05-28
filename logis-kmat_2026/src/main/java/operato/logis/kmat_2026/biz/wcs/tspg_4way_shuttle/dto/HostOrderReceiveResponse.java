package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * ========================================================================
 * HOST 주문 수신 응답 DTO
 * ========================================================================
 *
 * [목적]
 * - HOST 시스템으로 주문 처리 결과를 반환하는 응답 데이터 구조
 * - REST API 응답 바디로 사용
 *
 * [사용 위치]
 * - HostOrderController.receiveOrder() : HTTP 응답으로 반환
 * - Tspg4WayShuttleWcsFacade.receiveHostOrder() : 처리 결과 생성
 *
 * [응답 예시 - 성공]
 * {
 *   "success": true,
 *   "wcsOrderKey": "INB-1704067200000-abc12345",
 *   "hostOrderKey": "WMS-ORD-20240101-001",
 *   "message": "Order received and processed successfully"
 * }
 *
 * [응답 예시 - 실패]
 * {
 *   "success": false,
 *   "hostOrderKey": "WMS-ORD-20240101-001",
 *   "errorCode": "ERR_STOCK",
 *   "errorDesc": "Insufficient stock for SKU-001",
 *   "message": "Order processing failed"
 * }
 *
 * ========================================================================
 */
@Getter
@Builder
public class HostOrderReceiveResponse {

    // ========================================================================
    // 공통 필드
    // ========================================================================

    /**
     * 처리 성공 여부
     * - true: 주문이 정상적으로 접수 및 처리됨
     * - false: 주문 처리 중 오류 발생
     */
    private boolean success;

    /**
     * HOST 주문 키
     * - 요청에서 전달받은 hostOrderKey를 그대로 반환
     * - HOST 시스템에서 응답 매칭에 사용
     */
    private String hostOrderKey;

    /**
     * 안내 메시지
     * - 처리 결과에 대한 사람이 읽을 수 있는 메시지
     */
    private String message;

    // ========================================================================
    // 성공 시 반환 필드
    // ========================================================================

    /**
     * WCS 주문 키
     * - WCS에서 생성한 셔틀 주문의 고유 키
     * - 이후 주문 상태 조회, 취소 등에 사용
     * - 성공 시에만 값이 존재
     */
    private String wcsOrderKey;

    // ========================================================================
    // 실패 시 반환 필드
    // ========================================================================

    /**
     * 에러 코드
     * - 오류 유형을 식별하는 코드
     * - WcsConstants.ERR_* 참조
     * - 실패 시에만 값이 존재
     */
    private String errorCode;

    /**
     * 에러 상세 설명
     * - 오류 원인에 대한 상세 설명
     * - 개발자/운영자가 문제를 파악하는데 사용
     * - 실패 시에만 값이 존재
     */
    private String errorDesc;

    // ========================================================================
    // 정적 팩토리 메서드 (Static Factory Methods)
    // ------------------------------------------------------------------------
    // 일관된 응답 객체 생성을 위한 팩토리 메서드 제공
    // 직접 Builder를 사용하지 않고 이 메서드들을 통해 응답 생성 권장
    // ========================================================================

    /**
     * 성공 응답 생성
     *
     * [사용 시점]
     * - 주문이 정상적으로 접수되고 셔틀 주문이 생성된 경우
     *
     * @param wcsOrderKey  생성된 WCS 셔틀 주문 키
     * @param hostOrderKey HOST에서 전달받은 원본 주문 키
     * @return 성공 응답 객체
     */
    public static HostOrderReceiveResponse success(String wcsOrderKey, String hostOrderKey) {
        return HostOrderReceiveResponse.builder()
                .success(true)
                .wcsOrderKey(wcsOrderKey)
                .hostOrderKey(hostOrderKey)
                .message("Order received and processed successfully")
                .build();
    }

    /**
     * 실패 응답 생성
     *
     * [사용 시점]
     * - 검증 실패, 재고 부족, 로케이션 없음 등 처리 불가 시
     *
     * @param hostOrderKey HOST에서 전달받은 원본 주문 키
     * @param errorCode    에러 코드 (WcsConstants.ERR_* 사용)
     * @param errorDesc    에러 상세 설명
     * @return 실패 응답 객체
     */
    public static HostOrderReceiveResponse fail(String hostOrderKey, String errorCode, String errorDesc) {
        return HostOrderReceiveResponse.builder()
                .success(false)
                .hostOrderKey(hostOrderKey)
                .errorCode(errorCode)
                .errorDesc(errorDesc)
                .message("Order processing failed")
                .build();
    }

    /**
     * 중복 주문 응답 생성 (멱등성 처리)
     *
     * [사용 시점]
     * - 동일한 hostSystemCode + hostOrderKey로 이미 주문이 존재하는 경우
     * - 에러가 아닌 정상 응답으로 처리 (멱등성 보장)
     * - 기존에 생성된 wcsOrderKey를 반환
     *
     * @param wcsOrderKey  기존에 생성되었던 WCS 셔틀 주문 키
     * @param hostOrderKey HOST에서 전달받은 원본 주문 키
     * @return 중복 처리 응답 객체 (success=true)
     */
    public static HostOrderReceiveResponse duplicate(String wcsOrderKey, String hostOrderKey) {
        return HostOrderReceiveResponse.builder()
                .success(false)
                .wcsOrderKey(wcsOrderKey)
                .hostOrderKey(hostOrderKey)
                .message("Order already exists (idempotent)")
                .build();
    }
}
