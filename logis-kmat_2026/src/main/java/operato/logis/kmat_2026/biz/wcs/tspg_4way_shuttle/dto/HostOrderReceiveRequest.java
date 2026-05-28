package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * ========================================================================
 * HOST 주문 수신 요청 DTO
 * ========================================================================
 *
 * [목적]
 * - HOST 시스템(WMS 등)에서 WCS로 주문을 전달할 때 사용되는 요청 데이터 구조
 * - REST API 요청 바디를 Java 객체로 매핑
 *
 * [사용 위치]
 * - HostOrderController.receiveOrder() : HTTP 요청 바디로 수신
 * - Tspg4WayShuttleWcsFacade.receiveHostOrder() : 비즈니스 로직에서 사용
 * - HostOrderValidationService : 요청 데이터 검증
 *
 * [API 엔드포인트]
 * - POST /api/wcs/tspg4way/host-order/receive
 *
 * [요청 예시]
 * {
 *   "hostSystemCode": "WMS",
 *   "hostOrderKey": "WMS-ORD-20240101-001",
 *   "orderType": "INBOUND",
 *   "priority": 5,
 *   "ownerCode": "OWNER001",
 *   "requestPortCode": "PORT-01",
 *   "items": [
 *     {
 *       "lineNo": 1,
 *       "skuCode": "SKU-001",
 *       "lotNo": "LOT-20240101",
 *       "qty": 10,
 *       "uom": "EA"
 *     }
 *   ]
 * }
 *
 * ========================================================================
 */
@Getter
@Setter
public class HostOrderReceiveRequest {

    // ========================================================================
    // 필수 필드 (Required Fields)
    // ========================================================================

    /**
     * HOST 시스템 코드
     * - HOST 시스템을 식별하는 코드 (예: "WMS", "ERP", "MES")
     * - 멀티 테넌트 환경에서 주문 출처를 구분하는데 사용
     * - 필수값
     */
    @JsonProperty("hostSystemCode")
    private String hostSystemCode;

    /**
     * HOST 주문 키
     * - HOST 시스템에서 부여한 고유 주문 번호
     * - hostSystemCode + hostOrderKey 조합으로 멱등성 체크에 사용
     * - 동일 키로 재요청 시 중복 처리 방지
     * - 필수값
     */
    @JsonProperty("hostOrderKey")
    private String hostOrderKey;

    /**
     * 주문 유형
     * - 작업의 종류를 구분
     * - 허용값: "INBOUND"(입고), "OUTBOUND"(출고), "MOVE"(이동)
     * - WcsConstants.ORDER_TYPE_* 참조
     * - 필수값
     */
    @JsonProperty("orderType")
    private String orderType;

    /**
     * 화주 코드
     * - 물품의 소유자/고객사를 식별하는 코드
     * - 재고 관리 시 화주별로 분리 관리할 때 사용
     * - 필수값
     */
    @JsonProperty("ownerCode")
    private String ownerCode;

    @JsonProperty("eqGroupId")
    private String eqGroupId;

    /**
     * 주문 아이템 목록
     * - 이 주문에 포함된 SKU 품목들의 목록
     * - 최소 1개 이상의 아이템이 필수
     * - 필수값
     */
    @JsonProperty("items")
    private List<HostOrderItemRequest> items;

    // ========================================================================
    // 선택 필드 (Optional Fields)
    // ========================================================================

    /**
     * 우선순위
     * - 작업 처리 우선순위 (1: 최고 ~ 9: 최저)
     * - 미입력 시 기본값 5 적용
     * - 동일 시간대 작업 중 우선 처리할 주문 결정에 사용
     */
    private Integer priority;

    /**
     * 원본 페이로드
     * - HOST에서 전송한 원본 JSON 데이터
     * - 디버깅 및 감사 추적용으로 저장
     * - 파싱 실패 시 원인 분석에 활용
     */
    private String rawPayload;

    // ========================================================================
    // 내부 클래스: 주문 아이템 요청
    // ========================================================================

    /**
     * 주문 아이템 요청 DTO
     * - 주문에 포함된 개별 품목(SKU) 정보
     * - 한 주문에 여러 아이템이 포함될 수 있음
     */
    @Getter
    @Setter
    public static class HostOrderItemRequest {

        /**
         * 라인 번호
         * - 주문 내 아이템의 순번 (1부터 시작)
         * - 동일 주문 내에서 중복 불가
         * - 필수값, 양수만 허용
         */
        @JsonProperty("lineNo")
        private Integer lineNo;

        /**
         * SKU 코드
         * - 품목(Stock Keeping Unit) 식별 코드
         * - 상품의 고유 식별자
         * - 필수값
         */
        @JsonProperty("skuCode")
        private String skuCode;

        /**
         * LOT 번호
         * - 생산 로트 또는 입고 배치 번호
         * - 선입선출(FIFO) 관리, 유통기한 추적에 사용
         * - 선택값 (미입력 시 LOT 구분 없이 처리)
         */
        @JsonProperty("lotNo")
        private String lotNo;

        /**
         * 수량
         * - 요청 수량
         * - 필수값, 양수만 허용
         */
        @JsonProperty("qty")
        private Integer qty;

        /**
         * 단위
         * - 수량의 단위 (예: "EA", "BOX", "PLT")
         * - EA: 낱개, BOX: 박스, PLT: 팔레트
         * - 선택값
         */
        @JsonProperty("uom")
        private String uom;

        /**
         * 원본 속성
         * - HOST에서 전달한 아이템별 추가 속성 (JSON 형식)
         * - 확장 필드로 활용 (예: 중량, 크기, 특수 취급 지시 등)
         */
        @JsonProperty("rawAttr")
        private String rawAttr;
    }
}
