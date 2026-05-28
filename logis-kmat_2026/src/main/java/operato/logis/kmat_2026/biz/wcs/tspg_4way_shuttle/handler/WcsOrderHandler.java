package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.AllocationResult;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;

import java.util.List;

/**
 * ====================================================================
 * WCS 주문 처리 핸들러 인터페이스 (Strategy Pattern)
 * ====================================================================
 *
 * [설계 목적]
 * - 주문 유형별(INBOUND / OUTBOUND / MOVE) 처리 로직을 분리한다.
 * - 상위 입력이 HOST 주문이든 DIRECT 주문이든 같은 처리 흐름을 사용한다.
 * - 처리 입력은 DB 엔티티가 아니라 내부 공통 커맨드(WcsOrderCommand)를 사용한다.
 *
 * [왜 이렇게 설계했는가]
 * - 과거 방식은 TbWcsHostOrder / TbWcsHostOrderItem 엔티티에 의존했다.
 * - 하지만 DIRECT 주문은 HOST 엔티티가 없을 수 있다.
 * - 비즈니스 로직이 DB 엔티티 존재 여부에 따라 흔들리면 확장성이 떨어진다.
 *
 * 따라서 이 인터페이스는
 *
 *   "무엇을 처리할 것인가" = WcsOrderCommand
 *
 * 를 기준으로 동작하도록 설계한다.
 *
 * [이 인터페이스가 담당하는 범위]
 * - 주문 유형 매칭
 * - 로케이션 할당
 * - 재고 예약
 * - ECS용 Shuttle Order / Item 생성
 * - ECS 완료/실패/취소 콜백 후처리
 *
 * [이 인터페이스가 담당하지 않는 것]
 * - HOST 주문 저장 여부 결정
 * - REST 응답 생성
 * - 멱등성 체크
 * - 외부 시스템 연동 오케스트레이션
 *
 * 이런 것들은 Facade / Application Service 레이어에서 담당한다.
 *
 * [전체 흐름]
 *
 * 1. Facade가 orderType을 보고 적절한 Handler 선택
 * 2. allocateLocation() 수행
 * 3. reserveInventory() 수행
 * 4. createShuttleOrder() 수행
 * 5. createShuttleOrderItems() 수행
 * 6. ECS 전송
 * 7. ECS 콜백 시 handleCompletion / handleFailure / handleCancellation 수행
 *
 * [보상 흐름 — 중간 실패 시]
 * - reserveInventory() 성공 후 이후 단계에서 실패하면
 *   Facade가 releaseInventory()를 호출하여 예약을 원복한다.
 *
 * [구현체]
 * - InboundOrderHandler
 * - OutboundOrderHandler
 * - MoveOrderHandler
 * ====================================================================
 */
public interface WcsOrderHandler {

    /**
     * ====================================================================
     * 지원 주문 유형 확인
     * ====================================================================
     *
     * [목적]
     * - 현재 Handler가 특정 orderType을 처리할 수 있는지 확인한다.
     *
     * [예]
     * - InboundOrderHandler  -> INBOUND 일 때 true
     * - OutboundOrderHandler -> OUTBOUND 일 때 true
     * - MoveOrderHandler     -> MOVE 일 때 true
     *
     * [사용 위치]
     * - Facade의 findHandler() 로직
     *
     * @param orderType 주문 유형 코드
     * @return 처리 가능하면 true
     */
    boolean supports(String orderType);

    /**
     * ====================================================================
     * 로케이션 할당
     * ====================================================================
     *
     * [목적]
     * - 이 주문이 실제로 수행될 출발지/목적지 로케이션을 결정한다.
     *
     * [주문 유형별 예]
     * - INBOUND
     *   fromLocCode = 입고 포트
     *   toLocCode   = 적치 가능한 빈 위치
     *
     * - OUTBOUND
     *   fromLocCode = 재고가 존재하는 적치 위치
     *   toLocCode   = 출고 포트
     *
     * - MOVE
     *   fromLocCode = 현재 재고 위치
     *   toLocCode   = 이동 대상 위치
     *
     * [반환값]
     * - 성공 시: fromLocCode, toLocCode, eqGroupId, inventoryId 등 포함
     * - 실패 시: errorCode, errorDesc 포함
     *
     * @param command 공통 주문 입력 DTO
     * @return 로케이션 할당 결과
     */
    AllocationResult allocateLocation(WcsOrderCommand command);

    /**
     * ====================================================================
     * 재고 예약
     * ====================================================================
     *
     * [목적]
     * - 출고/이동 시 같은 재고가 중복 소비되지 않도록 allocQty를 증가시켜 예약한다.
     *
     * [주문 유형별 정책]
     * - INBOUND
     *   신규 입고이므로 기존 재고 예약 불필요
     *
     * - OUTBOUND / MOVE
     *   출발지 재고에 대해 예약 필요
     *
     * [주의]
     * - allocation 내부 inventoryId를 사용할 수 있다.
     * - 예약 실패 시 이후 흐름은 중단되어야 한다.
     *
     * @param command 공통 주문 입력 DTO
     * @param allocation 로케이션 할당 결과
     * @return 예약 성공 여부
     */
    boolean reserveInventory(WcsOrderCommand command, AllocationResult allocation);

    /**
     * ====================================================================
     * 재고 예약 취소 (보상 처리)
     * ====================================================================
     *
     * [목적]
     * - reserveInventory() 성공 후 이후 단계(ShuttleOrder 생성, ECS 전송 등)에서
     *   실패했을 때 이미 예약된 재고를 원복한다.
     *
     * [정책]
     * - INBOUND: 예약한 재고가 없으므로 no-op
     * - OUTBOUND: fromLocCode 기준으로 품목별 예약 해제
     * - MOVE: fromLocCode 기준 전체 예약 해제
     *
     * [주의]
     * - 이 메서드는 ECS 콜백 취소/실패 경로가 아니라
     *   주문 수신 흐름 내부 실패 시에만 호출된다.
     *   ECS 콜백 경로에서의 재고 처리는 handleFailure / handleCancellation 이 담당한다.
     *
     * @param command    공통 주문 입력 DTO
     * @param allocation 로케이션 할당 결과
     */
    void releaseInventory(WcsOrderCommand command, AllocationResult allocation);

    /**
     * ====================================================================
     * Shuttle Order 생성
     * ====================================================================
     *
     * [목적]
     * - ECS에 전송할 상위 작업 단위(TbWcsShuttleOrder)를 생성한다.
     *
     * [포함 정보 예]
     * - orderKey
     * - orderType
     * - priority
     * - fromLocCode / toLocCode
     * - eqGroupId
     * - ownerCode
     * - ecsIfStatus
     * - barcode
     *
     * [주의]
     * - 이 메서드는 생성만 담당한다.
     * - 실제 insert 여부는 구현체 정책에 따라 포함될 수 있다.
     *
     * @param command 공통 주문 입력 DTO
     * @param allocation 로케이션 할당 결과
     * @return 생성된 Shuttle Order
     */
    TbWcsShuttleOrder createShuttleOrder(WcsOrderCommand command, AllocationResult allocation);

    /**
     * ====================================================================
     * Shuttle Order Item 생성
     * ====================================================================
     *
     * [목적]
     * - ECS에 전송할 품목 단위 상세(TbWcsShuttleOrderItem)를 생성한다.
     *
     * [역할]
     * - command.items 를 shuttle item 형태로 변환
     * - lineNo / skuCode / lotNo / qty / uom 등을 채운다.
     *
     * @param command 공통 주문 입력 DTO
     * @param shuttleOrder 상위 Shuttle Order
     * @return 생성된 Shuttle Order Item 목록
     */
    List<TbWcsShuttleOrderItem> createShuttleOrderItems(WcsOrderCommand command,
                                                        TbWcsShuttleOrder shuttleOrder);

    /**
     * ====================================================================
     * 완료 후처리
     * ====================================================================
     *
     * [목적]
     * - ECS가 작업 완료를 통보했을 때 주문 유형별 후처리를 수행한다.
     *
     * [주문 유형별 예]
     * - INBOUND
     *   목적지 위치에 재고 생성 또는 증가
     *
     * - OUTBOUND
     *   출고 재고 qty 감소 및 allocQty 감소
     *
     * - MOVE
     *   출발지 재고 감소 + 목적지 재고 증가
     *
     * [공통 처리]
     * - 로케이션 잠금 해제
     *
     * @param shuttleOrder 셔틀 주문
     * @param items 셔틀 주문 아이템 목록
     */
    void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items);

    /**
     * ====================================================================
     * 실패 후처리
     * ====================================================================
     *
     * [목적]
     * - ECS 실패 콜백 수신 시 예약 해제 및 잠금 해제 등 복구 처리를 수행한다.
     *
     * [예]
     * - 출고/이동 예약 해제
     * - 잠긴 로케이션 해제
     * - 내부 에러 상태 기록 보조
     *
     * @param shuttleOrder 셔틀 주문
     * @param errorCode 에러 코드
     * @param errorDesc 에러 설명
     */
    void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc);

    /**
     * ====================================================================
     * 취소 후처리
     * ====================================================================
     *
     * [목적]
     * - ECS 취소 콜백 수신 시 실패와 유사한 자원 복구를 수행한다.
     *
     * [예]
     * - 예약 해제
     * - 로케이션 잠금 해제
     *
     * @param shuttleOrder 셔틀 주문
     */
    void handleCancellation(TbWcsShuttleOrder shuttleOrder);

    /**
     * ====================================================================
     * 렉단 컨베이어 도착 후처리
     * ====================================================================
     *
     * [목적]
     * - ECS 렉단 컨베이어 도착 콜백 수신 시 후처리 진행.
     *
     * @param shuttleOrder 셔틀 주문
     */
    void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder);
}