package operato.logis.wcs.handler;

import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.EcsCallbackApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;

import java.util.Collections;
import java.util.List;

/**
 * WCS 주문 처리 핸들러 (Strategy Pattern) — 주문 유형별(INBOUND / OUTBOUND / MOVE) 처리 로직 분리.
 *
 * 처리 입력은 DB 엔티티가 아니라 내부 공통 커맨드(WcsOrderCommand) 를 쓴다.
 * HOST 주문이든 DIRECT 주문이든 같은 처리 흐름을 사용한다.
 *
 * 책임 범위: 주문 유형 매칭, 로케이션 할당, 재고 예약, ECS Shuttle Order/Item 생성, ECS 콜백 후처리.
 * 비범위: HOST 주문 저장 결정, REST 응답 생성, 멱등성 체크, 외부 시스템 오케스트레이션 — Facade 가 담당.
 *
 * 전체 흐름:
 *   1) Facade 가 orderType 으로 Handler 선택
 *   2) allocateLocation() → 3) reserveInventory() → 4) createShuttleOrder() → 5) createShuttleOrderItems()
 *   6) ECS 전송 → 7) ECS 콜백 시 handleCompletion / handleFailure / handleCancellation
 *
 * 보상 흐름: reserveInventory() 성공 후 이후 단계에서 실패하면 Facade 가 releaseInventory() 로 원복.
 *
 * 구현체: InboundOrderHandler / OutboundOrderHandler / MoveOrderHandler.
 */
public interface WcsOrderHandler {

    /**
     * Handler 가 해당 orderType 을 처리할 수 있는지 — Facade.findHandler() 가 사용.
     */
    boolean supports(String orderType);

    /**
     * 로케이션 할당. 출발지/목적지를 주문 유형별 규칙으로 결정.
     *   - INBOUND: from=입고 포트, to=적치 가능한 빈 위치
     *   - OUTBOUND: from=재고 적치 위치, to=출고 포트
     *   - MOVE: from=현재 재고 위치, to=이동 대상 위치
     *
     * 실패 시 결과의 errorCode/errorDesc 로 사유 전달.
     */
    AllocationResult allocateLocation(WcsOrderCommand command);

    /**
     * 셔틀 산출 직전 from/to 로케이션을 taskId 로 잠근다.
     */
    boolean lockOrderLocationForShuttleOrderCalculate(WcsOrderCommand command, AllocationResult allocation, String taskId);

    /**
     * 재고 예약 — 같은 재고의 중복 소비 방지를 위해 allocQty 증가.
     *   - INBOUND: 신규 입고라 기존 재고 예약 불필요
     *   - OUTBOUND/MOVE: 출발지 재고 예약
     */
    boolean reserveInventory(WcsOrderCommand command, AllocationResult allocation);

    /**
     * 재고 예약 취소 (보상) — reserveInventory 이후 단계 실패 시 호출.
     *   - INBOUND: no-op
     *   - OUTBOUND: fromLocId 기준 품목별 해제
     *   - MOVE: fromLocId 기준 전체 해제
     *
     * 주의: ECS 콜백 경로 처리(handleFailure/handleCancellation) 와는 분리된다.
     */
    void releaseInventory(WcsOrderCommand command, AllocationResult allocation);

    /**
     * ECS 에 전송할 상위 작업 단위(TbWcsShuttleOrder) 생성.
     * 실제 insert 여부는 구현체 정책에 따른다.
     */
    TbWcsShuttleOrder createShuttleOrder(WcsOrderCommand command, AllocationResult allocation);

    /**
     * 품목 단위 상세(TbWcsShuttleOrderItem) 생성 — command.items 를 shuttle item 으로 변환.
     */
    List<TbWcsShuttleOrderItem> createShuttleOrderItems(WcsOrderCommand command,
                                                        TbWcsShuttleOrder shuttleOrder);

    /**
     * 완료 후처리 — ECS 완료 콜백 시 호출.
     *   - INBOUND: 목적지 재고 생성/증가
     *   - OUTBOUND: 출고 재고 qty/allocQty 감소
     *   - MOVE: 출발지 감소 + 목적지 증가
     * 공통: 로케이션 잠금 해제.
     */
    void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items);

    /**
     * 실패 후처리 — ECS 실패 콜백 시 예약 해제 + 로케이션 잠금 해제 등 복구.
     */
    void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc);

    /**
     * 취소 후처리 — ECS 취소 콜백 시 실패와 유사하게 자원 복구.
     */
    void handleCancellation(TbWcsShuttleOrder shuttleOrder);

    /**
     * 랙단 컨베이어 도착 콜백 후처리.
     */
    void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder);

    /**
     * 출발지 로딩 완료(FROM_LOADING_COMPLETE).
     *   - 출고/이동: from 로케이션의 stock_id NULL 처리 + 재고 차감
     *   - 입고: 포트 픽업이라 별도 처리 없음
     */
    void handleFromLoadingComplete(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items, EcsCallbackApi.Request request);

    /**
     * 목적지 언로딩 완료(TO_UNLOADING_COMPLETE).
     *   - 입고/이동: to 로케이션에 stock_id 세팅 + 재고 생성
     *   - 출고: 포트 도착이라 별도 처리 없음
     */
    void handleToUnloadingComplete(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items);

    /**
     * 락 이후 방해물 계획 산출 — orderType 별 적절한 locId 기준으로 MultiDeepSortService 호출.
     *   - INBOUND: toLocId 기준
     *   - OUTBOUND/MOVE: fromLocId 기준
     */
    default List<RelocationTaskDto> resolveObstacles(WcsOrderCommand command, AllocationResult allocation) {
        return Collections.emptyList();
    }
}
