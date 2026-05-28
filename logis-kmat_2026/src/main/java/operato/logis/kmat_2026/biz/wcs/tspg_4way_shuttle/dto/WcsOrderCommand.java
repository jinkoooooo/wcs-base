package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * ========================================================================
 * WCS 주문 공통 Command DTO
 * ========================================================================
 *
 * [목적]
 * - WCS 내부 비즈니스 처리의 기준 입력 모델
 * - HOST 주문 / DIRECT 주문 / 내부 배치 / 시뮬레이터 주문 등을
 *   하나의 공통 구조로 변환하여 처리하기 위한 내부 DTO
 *
 * [핵심 아이디어]
 * - 비즈니스 처리 로직은 DB 엔티티(TbWcsHostOrder)에 의존하지 않는다.
 * - 입력 주문을 먼저 WcsOrderCommand로 정규화(normalize)한 뒤,
 *   Handler / Allocator / Reservation 로직이 이를 사용한다.
 *
 * [왜 필요한가]
 * - 기존 구조는 HOST 주문 엔티티가 없으면 Direct 주문 처리에 불리했다.
 * - 실제로는 HOST가 없는 주문도 처리해야 하므로
 *   "주문 입력 자체"를 표현하는 내부 모델이 필요하다.
 *
 * [사용 예]
 * 1. HOST API 수신
 *    HostOrderReceiveRequest -> WcsOrderCommand 변환
 *
 * 2. DIRECT API 수신
 *    Direct 요청 DTO -> WcsOrderCommand 변환
 *
 * 3. 내부 배치 생성
 *    배치 데이터 -> WcsOrderCommand 변환
 *
 * [주의]
 * - 이 DTO는 DB 저장용이 아니라 처리용이다.
 * - DB 저장 필요 여부는 Facade/Application Layer에서 별도로 결정한다.
 *
 * [fromLocCode / toLocCode 사용 정책]
 * - INBOUND
 *   fromLocCode : 입고 포트/요청 시작 위치
 *   toLocCode   : 지정된 적치 위치(없으면 allocator가 계산)
 *
 * - OUTBOUND
 *   fromLocCode : 지정된 출고 시작 위치(없으면 allocator가 계산)
 *   toLocCode   : 출고 포트(없으면 allocator가 기본 포트 사용)
 *
 * - MOVE
 *   fromLocCode : 이동 시작 위치
 *   toLocCode   : 이동 목표 위치(없으면 allocator가 계산)
 * ========================================================================
 */
@Getter
@Setter
@Builder
public class WcsOrderCommand {

    /**
     * 주문 발생 소스 유형
     * 예)
     * - HOST
     * - DIRECT
     * - BATCH
     * - SIMULATION
     */
    @JsonProperty("sourceType")
    private final String sourceType;

    /**
     * 상위 시스템 코드 또는 내부 소스 코드
     * 예)
     * - WMS
     * - ERP
     * - DIRECT
     * - INTERNAL
     */
    @JsonProperty("sourceSystemCode")
    private final String sourceSystemCode;

    /**
     * 소스 주문 키
     * - HOST 주문이면 hostOrderKey
     * - DIRECT 주문이면 directOrderKey 또는 fallback key
     */
    @JsonProperty("sourceOrderKey")
    private final String sourceOrderKey;

    /**
     * WCS Order Key
     * 미리 생성했다면 해당 Wcs Order Key 사용
     */
    @JsonProperty("wcsOrderKey")
    private String wcsOrderKey;

    /**
     * 주문 유형
     * - INBOUND
     * - OUTBOUND
     * - MOVE
     */
    @JsonProperty("orderType")
    private final String orderType;

    /**
     * 화주 코드
     * - 재고 조회/생성 시 owner 기준으로 사용
     */
    @JsonProperty("ownerCode")
    private final String ownerCode;

    /**
     * 우선순위
     * - 셔틀 오더 생성 시 priority 반영
     */
    @JsonProperty("priority")
    private final Integer priority;

    @JsonProperty("eqGroupId")
    private final String eqGroupId;

    /**
     * 출발지 로케이션 코드
     *
     * [주문 유형별 의미]
     * - INBOUND  : 입고 포트/시작 위치
     * - OUTBOUND : 출고 시작 재고 위치
     * - MOVE     : 이동 시작 위치
     *
     * [사용 정책]
     * - 값이 있으면 allocator가 우선 사용
     * - 값이 없으면 주문 유형별 allocator가 계산할 수 있음
     */
    @JsonProperty("fromLocCode")
    private final String fromLocCode;

    /**
     * 목적지 로케이션 코드
     *
     * [주문 유형별 의미]
     * - INBOUND  : 적치 목적지
     * - OUTBOUND : 출고 포트
     * - MOVE     : 이동 목적지
     *
     * [사용 정책]
     * - 값이 있으면 allocator가 우선 사용
     * - 값이 없으면 주문 유형별 allocator가 계산할 수 있음
     */
    @JsonProperty("toLocCode")
    private final String toLocCode;

    /**
     * 대표 바코드
     * - barcode
     */
    @JsonProperty("barCode")
    private final String barCode;

    /**
     * 원본 payload
     * - 디버깅 및 추적용
     */
    @JsonProperty("rawPayload")
    private final String rawPayload;

    /**
     * HOST 오더를 실제 DB에 저장할지 여부
     * - true  : HOST 오더 저장
     * - false : 내부 처리만 하고 HOST 테이블 미사용
     */
    @JsonProperty("persistHostOrder")
    private boolean persistHostOrder;

    /**
     * 주문 아이템 목록
     */
    @JsonProperty("items")
    private final List<WcsOrderCommandItem> items;
}