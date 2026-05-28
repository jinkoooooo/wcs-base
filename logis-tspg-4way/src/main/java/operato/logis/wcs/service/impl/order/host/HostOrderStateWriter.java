package operato.logis.wcs.service.impl.order.host;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * host_order / host_order_item 쓰기 단일 창구.
 *
 * REQUIRES_NEW 메서드들은 본 트랜잭션과 분리해 HOST 상태를 독립 확정한다.
 */
@Service
@RequiredArgsConstructor
public class HostOrderStateWriter {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderStateWriter.class);

    // 기본 우선순위
    private static final int DEFAULT_PRIORITY = 5;

    // 종결 상태 (HostOrderStatus 코드 기준)
    private static final Set<Integer> TERMINAL_STATUSES = Set.of(
            HostOrderStatus.COMPLETED.code(),
            HostOrderStatus.CANCELLED.code(),
            HostOrderStatus.REJECTED.code(),
            HostOrderStatus.TEST_FAILED.code(),
            HostOrderStatus.ERROR.code()
    );

    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final HostReservationService hostReservationService;

    /**
     * 수신 요청을 host_order + items 로 저장한다.
     * test_required 확정 지점 — header = OR(item.test_required).
     * 명시적 Boolean.TRUE 만 시험 대상, null/false 는 false.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsHostOrder saveHostOrderFromCreateRequest(HostOrderApi.Request request) {

        // 헤더 생성
        TbWcsHostOrder order = buildHeader(request);
        hostOrderRepository.insert(order);
        logger.info("[ Order ][ Host ] saved - hostSystemCode={}, hostOrderKey={}",
                order.getHostSystemCode(), order.getHostOrderKey());

        // 아이템 저장 + 헤더의 testRequired/niaRequired 집계
        ItemFlags flags = saveItemsAndAggregateFlags(order, request.getItems());

        // 집계된 플래그 반영
        applyItemFlagsToHeader(order, flags);

        // from/to 지정 시 즉시 logical reservation (수동 출고, 지정 위치 입고, MOVE 등에서 다른 액션 차단)
        hostReservationService.reserveForHostOrder(order);

        return order;
    }

    /**
     * 요청 → 헤더 entity 변환.
     */
    private TbWcsHostOrder buildHeader(HostOrderApi.Request request) {
        TbWcsHostOrder order = new TbWcsHostOrder();
        order.setHostSystemCode(request.getHostSystemCode());
        order.setHostOrderKey(request.getHostOrderKey());
        order.setOrderType(request.getOrderType());
        order.setEqGroupId(request.getEqGroupId());
        order.setOrderStatus(HostOrderStatus.RECEIVED.code());
        order.setPriority(ValueUtil.isNotEmpty(request.getPriority()) ? request.getPriority() : DEFAULT_PRIORITY);
        order.setOwnerCode(request.getOwnerCode());
        order.setFromLocCode(request.getFromLocId());
        order.setToLocCode(request.getToLocId());
        order.setBarcode(request.getBarcode());
        order.setReceivedAt(new Date());
        order.setRawPayload(request.getRawPayload());
        order.setScheduledDate(LocalDateUtils.toDate(request.getScheduledDate()));
        order.setTestRequired(false);
        order.setNiaRequired(false);
        return order;
    }

    /**
     * 아이템 저장 + testRequired/niaRequired 플래그 집계.
     */
    private ItemFlags saveItemsAndAggregateFlags(TbWcsHostOrder order, List<HostOrderApi.Item> items) {
        if (ValueUtil.isEmpty(items)) return new ItemFlags(false, false);

        boolean anyTest = false;
        boolean anyNia = false;
        for (HostOrderApi.Item req : items) {
            if (ValueUtil.isEmpty(req)) continue;

            TbWcsHostOrderItem item = toItem(order, req);
            hostOrderItemRepository.insert(item);

            if (Boolean.TRUE.equals(item.getTestRequired())) anyTest = true;
            if (Boolean.TRUE.equals(item.getNiaRequired())) anyNia = true;
        }
        return new ItemFlags(anyTest, anyNia);
    }

    /**
     * 아이템 플래그를 헤더에 반영 (변경 있을 때만 update).
     */
    private void applyItemFlagsToHeader(TbWcsHostOrder order, ItemFlags flags) {
        if (flags.anyTest()) {
            order.setTestRequired(true);
            hostOrderRepository.update(order, "testRequired");
        }
        if (flags.anyNia()) {
            order.setNiaRequired(true);
            hostOrderRepository.update(order, "niaRequired");
        }
    }

    /**
     * 요청 Item → entity 변환.
     */
    private static TbWcsHostOrderItem toItem(TbWcsHostOrder order, HostOrderApi.Item req) {
        TbWcsHostOrderItem item = new TbWcsHostOrderItem();
        item.setHostSystemCode(order.getHostSystemCode());
        item.setHostOrderKey(order.getHostOrderKey());
        item.setItemCode(req.getItemCode());
        item.setLotNo(req.getLotNo());
        item.setQty(req.getQty());
        item.setUom(req.getUom());
        item.setProduceDate(req.getProduceDate());
        item.setExpiryDate(req.getExpiryDate());
        item.setRawAttr(req.getRawAttr());
        item.setTestRequestNo(req.getTestRequestNo());
        item.setTestNo(req.getTestNo());
        item.setTestRequired(Boolean.TRUE.equals(req.getTestRequired()));
        item.setNiaRequired(Boolean.TRUE.equals(req.getNiaRequired()));
        return item;
    }

    /**
     * 할당 완료 마킹 — 본 트랜잭션 롤백과 독립적으로 HOST 상태 확정.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markAllocated(TbWcsHostOrder hostOrder, String wcsOrderKey) {
        if (ValueUtil.isEmpty(hostOrder)) return;

        hostOrder.setOrderStatus(HostOrderStatus.WAITING_EXEC.code());
        hostOrder.setWcsOrderKey(wcsOrderKey);
        hostOrder.setErrorCode(null);
        hostOrder.setErrorDesc(null);
        hostOrderRepository.update(hostOrder, "orderStatus", "wcsOrderKey", "errorCode", "errorDesc");

        logger.info("[ Order ][ Host ] allocated - hostSystemCode={}, hostOrderKey={}, wcsOrderKey={}",
                hostOrder.getHostSystemCode(), hostOrder.getHostOrderKey(), wcsOrderKey);
    }

    /**
     * 에러 마킹 — WCS 비즈니스 롤백과 분리.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markError(TbWcsHostOrder hostOrder, String errorCode, String errorDesc) {
        if (ValueUtil.isEmpty(hostOrder)) return;

        hostOrder.setOrderStatus(HostOrderStatus.ERROR.code());
        hostOrder.setErrorCode(errorCode);
        hostOrder.setErrorDesc(errorDesc);
        hostOrderRepository.update(hostOrder, "orderStatus", "errorCode", "errorDesc");

        // reservation 해제 (자기 키 아니면 자동 no-op)
        hostReservationService.releaseForHostOrder(hostOrder);
    }

    /**
     * EXECUTING 마킹 — ECS shuttle STARTED 콜백 직후 분리 갱신.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markExecutingByHostOrderKey(String hostOrderKey) {
        TbWcsHostOrder hostOrder = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(hostOrder)) return;

        int prev = hostOrder.getOrderStatus();
        hostOrder.setOrderStatus(HostOrderStatus.EXECUTING.code());
        hostOrderRepository.update(hostOrder, "orderStatus");

        logger.info("[ Order ][ Host ] executing - hostOrderKey={}, prevStatus={}, newStatus=EXECUTING",
                hostOrderKey, prev);
    }

    /**
     * READY_FOR_ALLOC 마킹 — 산출 대상 진입.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markReadyForAllocation(TbWcsHostOrder hostOrder) {
        transitionStatus(hostOrder, HostOrderStatus.READY_FOR_ALLOC, "READY_FOR_ALLOC");
    }

    /**
     * WAITING_SCHEDULE 마킹 — 미래 일자 예약.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markWaitingSchedule(TbWcsHostOrder hostOrder) {
        transitionStatus(hostOrder, HostOrderStatus.WAITING_SCHEDULE, "WAITING_SCHEDULE");
    }

    /**
     * COMPLETED 마킹 — 호출자가 자식 shuttle 완료 확인 후 호출 (audit 로깅은 호출자 책임).
     */
    @Transactional(rollbackFor = Exception.class)
    public int markCompleted(TbWcsHostOrder hostOrder) {
        if (ValueUtil.isEmpty(hostOrder)) return 0;
        int prev = hostOrder.getOrderStatus();
        transitionStatus(hostOrder, HostOrderStatus.COMPLETED, "COMPLETED");
        return prev;
    }

    /**
     * INBOUND_TEST_WAIT 마킹 — 입고 완료했으나 시험 결과 미종결.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markInboundTestWait(TbWcsHostOrder hostOrder) {
        transitionStatus(hostOrder, HostOrderStatus.INBOUND_TEST_WAIT, "INBOUND_TEST_WAIT");
    }

    /**
     * TEST_FAILED 마킹 — 시험 부적합 확정.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markTestFailed(TbWcsHostOrder hostOrder) {
        transitionStatus(hostOrder, HostOrderStatus.TEST_FAILED, "TEST_FAILED");
    }

    /**
     * RECEIVED 마킹 — 시험 의뢰 진입 (test_status 변경 직전 트랜지션).
     */
    @Transactional(rollbackFor = Exception.class)
    public void markReceived(TbWcsHostOrder hostOrder) {
        transitionStatus(hostOrder, HostOrderStatus.RECEIVED, "RECEIVED");
    }

    /**
     * CANCELLED 마킹 — 외부/수동 취소. reason 있으면 error_desc 기록 후 종결 전이(reservation 자동 해제).
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCancelled(TbWcsHostOrder hostOrder, String reason) {
        if (ValueUtil.isEmpty(hostOrder)) return;

        // error_desc 는 order_status 와 별도 컬럼 — 사유 있으면 먼저 기록
        if (ValueUtil.isNotEmpty(reason)) {
            hostOrder.setErrorDesc("CANCEL: " + reason);
            hostOrderRepository.update(hostOrder, "errorDesc");
        }
        transitionStatus(hostOrder, HostOrderStatus.CANCELLED, "CANCELLED");
    }

    /**
     * 상태 전이 공통 처리 — 종결 상태 전이 시 reservation 자동 해제.
     */
    private void transitionStatus(TbWcsHostOrder hostOrder, HostOrderStatus next, String label) {
        if (ValueUtil.isEmpty(hostOrder)) return;
        int prev = hostOrder.getOrderStatus();
        hostOrder.setOrderStatus(next.code());
        hostOrderRepository.update(hostOrder, "orderStatus");
        logger.info("[ Order ][ Host ] state transition - hostOrderKey={}, prevStatus={}, newStatus={}",
                hostOrder.getHostOrderKey(), prev, label);

        // 종결 상태 전이 시 reservation 해제
        if (TERMINAL_STATUSES.contains(next.code())) {
            hostReservationService.releaseForHostOrder(hostOrder);
        }
    }

    /**
     * 시뮬레이션/직접 생성 흐름용 가상 host_order + items.
     * 범용 호출을 위해 기본 REQUIRED (호출자 트랜잭션 합류, 단독 호출 시 새 트랜잭션).
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsHostOrder saveVirtualHostOrderWithItems(WcsOrderCommand command) {
        TbWcsHostOrder hostOrder = createVirtualHeader(command);
        createVirtualItems(command, hostOrder);
        return hostOrder;
    }

    /**
     * 가상 헤더 생성 — hostSystemCode/hostOrderKey 누락 시 자동 부여.
     */
    private TbWcsHostOrder createVirtualHeader(WcsOrderCommand command) {
        TbWcsHostOrder order = new TbWcsHostOrder();
        order.setHostSystemCode(StringUtils.hasText(command.getHostSystemCode())
                ? command.getHostSystemCode() : "DIRECT");
        order.setHostOrderKey(StringUtils.hasText(command.getHostOrderKey())
                ? command.getHostOrderKey() : "DIRECT-" + System.currentTimeMillis());
        order.setOrderType(command.getOrderType());
        order.setEqGroupId(command.getEqGroupId());
        order.setOrderStatus(HostOrderStatus.VALIDATED.code());
        order.setPriority(ValueUtil.isNotEmpty(command.getPriority()) ? command.getPriority() : DEFAULT_PRIORITY);
        order.setOwnerCode(command.getOwnerCode());
        order.setFromLocCode(command.getFromLocId());
        order.setToLocCode(command.getToLocId());
        order.setReceivedAt(new Date());
        order.setRawPayload(command.getRawPayload());

        hostOrderRepository.insert(order);
        return order;
    }

    /**
     * 가상 아이템 생성.
     */
    private void createVirtualItems(WcsOrderCommand command, TbWcsHostOrder hostOrder) {
        if (ValueUtil.isEmpty(command.getItems())) return;

        for (WcsOrderCommand.Item cmd : command.getItems()) {
            if (ValueUtil.isEmpty(cmd)) continue;

            TbWcsHostOrderItem item = new TbWcsHostOrderItem();
            item.setHostSystemCode(hostOrder.getHostSystemCode());
            item.setHostOrderKey(hostOrder.getHostOrderKey());
            item.setItemCode(cmd.getItemCode());
            item.setLotNo(cmd.getLotNo());
            item.setQty(cmd.getQty());
            item.setUom(cmd.getUom());
            item.setProduceDate(cmd.getProduceDate());
            item.setExpiryDate(cmd.getExpiryDate());
            item.setRawAttr(cmd.getRawAttr());
            hostOrderItemRepository.insert(item);
        }
    }

    /**
     * 아이템 집계 플래그.
     */
    private record ItemFlags(boolean anyTest, boolean anyNia) {}
}
