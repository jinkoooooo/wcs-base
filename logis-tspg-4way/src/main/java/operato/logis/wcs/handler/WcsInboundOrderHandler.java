package operato.logis.wcs.handler;

import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.QcTestStatus;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.EcsCallbackApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.allocation.location.InboundLocationAllocator;
import operato.logis.wcs.service.impl.order.host.HostOrderCompletion;
import operato.logis.wcs.service.impl.inventory.reservation.InboundReservationService;
import operato.logis.wcs.service.impl.inventory.reservation.ReservationReleaseService;
import operato.logis.wcs.service.impl.allocation.location.LocationService;
import operato.logis.wcs.common.util.generator.HostOrderKeyGenerator;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 입고(INBOUND) / 재입고(PUTBACK) 주문 처리 핸들러.
 *
 * 통일 정책:
 *   - reserveInventory: stockId 확보 + allocation.stockId set (입고는 신규 생성)
 *   - createShuttleOrder: 무조건 order.carryingStockId = allocation.stockId
 *   - releaseInventory: ECS 전송 실패 등으로 산출 롤백 시 호출
 *
 * 입고 상태 흐름:
 *   - reserveInventory  : (없음) → INBOUND  (stock 신규 생성, toLoc 선매핑)
 *   - handleCompletion  : INBOUND → IDLE
 *   - releaseInventory  : INBOUND → DELETE  (산출 롤백)
 *   - handleCancellation: INBOUND → DELETE  (취소)
 *   - handleFailure     : INBOUND → DELETE + 랙 격리
 */
@Component
@RequiredArgsConstructor
public class WcsInboundOrderHandler implements WcsOrderHandler {

    private static final Logger logger = LoggerFactory.getLogger(WcsInboundOrderHandler.class);

    private final InboundLocationAllocator locationAllocator;
    private final InboundReservationService inboundReservationService;
    private final ReservationReleaseService reservationReleaseService;
    private final LocationService wcsLocationService;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;
    private final HostOrderRepository hostOrderRepository;
    protected final HostOrderKeyGenerator orderKeyGenerator;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final LocationService lockService;
    private final HostOrderCompletion hostOrderCompletion;

    @Override
    public boolean supports(String orderType) {
        return OrderType.INBOUND.codeAsString().equalsIgnoreCase(orderType);
    }

    @Override
    public AllocationResult allocateLocation(WcsOrderCommand command) {
        return locationAllocator.allocate(command);
    }

    @Override
    public boolean lockOrderLocationForShuttleOrderCalculate(WcsOrderCommand command,
                                                             AllocationResult allocation,
                                                             String taskId) {
        lockService.lockForOrder(allocation, command.getOrderType(),
                taskId, command.getHostOrderKey());
        logger.info("[ Handler ][ Inbound ] location reserved - taskId={}", taskId);
        return true;
    }

    /**
     * 입고 산출 — INBOUND_READY stock 신규 생성 + toLoc 후보 매핑.
     *
     * 일반 파렛트는 items 별 stock, 빈 파렛트는 SKU/qty 없이 stock 1행 생성.
     * 생성된 stockId 는 allocation 에 set.
     * 최종 toLocId/carryingStockId 확정은 BCR 스캔 시점에 이루어진다.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveInventory(WcsOrderCommand command, AllocationResult allocation) {
        if (ValueUtil.isEmpty(command)) {
            logger.warn("[ Handler ][ Inbound ] reserve - empty command");
            return false;
        }

        if (ValueUtil.isEmpty(command.getBarCode())) {
            throw new ElidomRuntimeException(
                    WcsError.MISSING_REQUIRED_FIELD.codeAsString(),
                    "입고 산출 실패: barcode 필수. hostOrderKey=" + command.getHostOrderKey());
        }

        // command.items → TbWcsShuttleOrderItem 변환 (Reservation 서비스에 넘기는 임시 객체)
        // items 가 비면 빈 파렛트 입고로 처리
        List<TbWcsShuttleOrderItem> items = new ArrayList<>();
        if (ValueUtil.isNotEmpty(command.getItems())) {
            for (WcsOrderCommand.Item ci : command.getItems()) {
                if (ValueUtil.isEmpty(ci)) continue;
                TbWcsShuttleOrderItem it = new TbWcsShuttleOrderItem();
                it.setItemCode(ci.getItemCode());
                it.setLotNo(ci.getLotNo());
                it.setQty(ci.getQty());
                it.setProduceDate(ci.getProduceDate());
                it.setExpiryDate(ci.getExpiryDate());
                items.add(it);
            }
        }

        // 예약 실행 + stockId 회수
        String stockId = inboundReservationService.reserveInboundPallet(
                allocation.getEqGroupId(),
                allocation.getToLocId(),
                command.getOwnerCode(),
                items,
                command.getBarCode(),
                command.getHostOrderKey()
        );

        if (ValueUtil.isEmpty(stockId)) {
            logger.warn("[ Handler ][ Inbound ] reserve returned null stockId - hostOrderKey={}",
                    command.getHostOrderKey());
            return false;
        }

        allocation.setStockId(stockId);

        logger.info("[ Handler ][ Inbound ] stock reserved - hostOrderKey={}, stockId={}, toLocId={}",
                command.getHostOrderKey(), stockId, allocation.getToLocId());
        return true;
    }

    /**
     * 산출 롤백 (ECS 전송 실패 등) — INBOUND stock 삭제 + 로케이션 선매핑 해제.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseInventory(WcsOrderCommand command, AllocationResult allocation) {
        if (ValueUtil.isEmpty(allocation) || ValueUtil.isEmpty(allocation.getStockId())) {
            logger.debug("[ Handler ][ Inbound ] release - no reserved stock");
            return;
        }
        logger.info("[ Handler ][ Inbound ] cancel reservation - hostOrderKey={}, stockId={}, toLocId={}",
                command == null ? null : command.getHostOrderKey(),
                allocation.getStockId(),
                allocation.getToLocId());

        reservationReleaseService.cancelInboundReservation(
                allocation.getEqGroupId(),
                allocation.getStockId(),
                allocation.getToLocId()
        );
    }

    @Override
    public TbWcsShuttleOrder createShuttleOrder(WcsOrderCommand command, AllocationResult allocation) {
        String wcsOrderKey = ValueUtil.isEmpty(command.getWcsOrderKey()) ?
                orderKeyGenerator.generate("ORDER_KEY") :
                command.getWcsOrderKey();

        // 헤더 빌드
        TbWcsShuttleOrder order = new TbWcsShuttleOrder();
        order.setOrderKey(wcsOrderKey);
        order.setOrderType(OrderType.INBOUND.codeAsString());

        // sub_order_type — 재입고는 NORMAL 로 강제
        if (ValueUtil.isNotEmpty(command.getSubOrderType())) {
            order.setSubOrderType(command.getSubOrderType());
        } else {
            order.setSubOrderType(SubOrderType.NORMAL.codeAsString());
        }

        order.setOrderStatus(ShuttleOrderStatus.CREATED.codeAsIntOrNull());
        order.setPriority(ValueUtil.isEmpty(command.getPriority()) ? 0 : command.getPriority());
        order.setFromLocCode(allocation.getFromLocId());
        order.setToLocCode(allocation.getToLocId());
        order.setEqGroupId(allocation.getEqGroupId());
        order.setOwnerCode(command.getOwnerCode());
        order.setHostOrderKey(command.getHostOrderKey());
        order.setEcsIfStatus(EcsIfStatus.READY.codeAsIntOrNull());

        // parent_order_key — 시험 사이클 재입고 추적용
        if (ValueUtil.isNotEmpty(command.getParentOrderKey())) {
            order.setParentOrderKey(command.getParentOrderKey());
        }

        Integer level = inventoryLocationRepository.getLevelByLocId(allocation.getEqGroupId(), allocation.getToLocId());
        order.setLevel(level);

        if (ValueUtil.isEmpty(command.getBarCode())) {
            throw new ElidomRuntimeException(
                    WcsError.MISSING_REQUIRED_FIELD.codeAsString(),
                    "입고 주문 생성 실패: barcode는 필수입니다. hostOrderKey=" + command.getHostOrderKey());
        }
        order.setBarcode(command.getBarCode());

        if (ValueUtil.isNotEmpty(allocation.getStockId())) {
            order.setCarryingStockId(allocation.getStockId());
        }

        // 시험 정보 — host_order 에서 복사
        if (ValueUtil.isNotEmpty(command.getHostOrderKey())) {
            TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(command.getHostOrderKey());
            if (ValueUtil.isNotEmpty(host)) {
                order.setTestRequired(host.getTestRequired());
            }
        }

        shuttleOrderRepository.insert(order);
        logger.info("[ Handler ][ Inbound ] shuttle order created - orderKey={}, sub={}, parent={}, stockId={}, toLocId={}",
                order.getOrderKey(), order.getSubOrderType(), order.getParentOrderKey(),
                order.getCarryingStockId(), order.getToLocCode());
        return order;
    }

    @Override
    public List<TbWcsShuttleOrderItem> createShuttleOrderItems(WcsOrderCommand command,
                                                               TbWcsShuttleOrder shuttleOrder) {
        if (ValueUtil.isEmpty(command) || ValueUtil.isEmpty(command.getItems())) {
            logger.info("[ Handler ][ Inbound ] no items in command - orderKey={}", shuttleOrder.getOrderKey());
            return new ArrayList<>();
        }

        List<TbWcsShuttleOrderItem> orderItems = new ArrayList<>();
        for (WcsOrderCommand.Item commandItem : command.getItems()) {
            TbWcsShuttleOrderItem item = new TbWcsShuttleOrderItem();
            item.setOrderKey(shuttleOrder.getOrderKey());
            item.setItemCode(commandItem.getItemCode());
            item.setLotNo(commandItem.getLotNo());
            item.setQty(commandItem.getQty());
            item.setUom(commandItem.getUom());
            item.setProduceDate(commandItem.getProduceDate());
            item.setExpiryDate(commandItem.getExpiryDate());
            item.setTestRequired(commandItem.getTestRequired());
            item.setTestRequestNo(commandItem.getTestRequestNo());
            item.setTestNo(commandItem.getTestNo());
            item.setTestStatus(commandItem.getTestStatus());
            item.setLineStatus(ShuttleOrderStatus.CREATED.codeAsIntOrNull());

            shuttleOrderItemRepository.insert(item);
            orderItems.add(item);
        }
        logger.info("[ Handler ][ Inbound ] items created - count={}, orderKey={}",
                orderItems.size(), shuttleOrder.getOrderKey());
        return orderItems;
    }

    @Override
    public List<RelocationTaskDto> resolveObstacles(WcsOrderCommand command, AllocationResult allocation) {
        // 입고는 빈 로케이션으로 가는 동작이라 방해물 산출 불필요
        return null;
    }

    @Override
    public void handleFromLoadingComplete(TbWcsShuttleOrder shuttleOrder,
                                          List<TbWcsShuttleOrderItem> items,
                                          EcsCallbackApi.Request request) {
        logger.info("[ Handler ][ Inbound ] fromLoadingComplete - orderKey={}, fromLocId(port)={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getFromLocCode());
    }

    @Override
    public void handleToUnloadingComplete(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        // 입고: to 에 화물 도착 — 처리 없음 (재고/로케이션은 COMPLETE 에서 처리)
        logger.info("[ Handler ][ Inbound ] toUnloadingComplete - orderKey={}, toLocId={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getToLocCode());
    }

    /**
     * 입고 완료 — shuttle/inventory 도메인만 처리.
     * host_order 상태 전이는 caller(EcsCallbackProcessor) 가 HostOrderTransitioner 로 처리.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        logger.info("[ Handler ][ Inbound ] complete - orderKey={}, stockId={}, parentOrderKey={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getCarryingStockId(),
                shuttleOrder.getParentOrderKey());

        String stockId = shuttleOrder.getCarryingStockId();

        if (ValueUtil.isNotEmpty(stockId)) {
            // 우선순위: 반품 입고 → 시험 보류 → 국검 대상 → 일반
            SubOrderType sub = SubOrderType.fromOrNormal(shuttleOrder.getSubOrderType());
            String eqGroupId = shuttleOrder.getEqGroupId();
            String toLoc = shuttleOrder.getToLocCode();
            String barcode = shuttleOrder.getBarcode();
            String hostKey = shuttleOrder.getHostOrderKey();

            if (sub == SubOrderType.RETURN_IN) {
                inboundReservationService.completeInboundAsReturn(eqGroupId, stockId, toLoc, barcode, hostKey);
            } else if (isHoldForTest(hostKey)) {
                inboundReservationService.completeInboundAwaitingTest(eqGroupId, stockId, toLoc, barcode, hostKey);
            } else if (inboundReservationService.isNiaRequiredForHostOrder(hostKey)) {
                inboundReservationService.completeInboundAwaitingNia(eqGroupId, stockId, toLoc, barcode, hostKey);
            } else {
                inboundReservationService.completeInboundAsNormal(eqGroupId, stockId, toLoc, barcode, hostKey);
            }
        } else {
            // stockId 없으면 reserve + complete NORMAL 2-step 으로 신규 생성
            String createdStockId = inboundReservationService.reserveInboundPallet(
                    shuttleOrder.getEqGroupId(),
                    shuttleOrder.getToLocCode(),
                    shuttleOrder.getOwnerCode(),
                    items,
                    shuttleOrder.getBarcode(),
                    shuttleOrder.getHostOrderKey()
            );
            if (ValueUtil.isNotEmpty(createdStockId)) {
                inboundReservationService.completeInboundAsNormal(
                        shuttleOrder.getEqGroupId(),
                        createdStockId,
                        shuttleOrder.getToLocCode(),
                        shuttleOrder.getBarcode(),
                        shuttleOrder.getHostOrderKey()
                );
            }
            shuttleOrder.setCarryingStockId(createdStockId);
            shuttleOrderRepository.update(shuttleOrder, "carryingStockId");
        }

        // 로케이션 잠금 해제 + 호스트 완료 평가
        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getToLocCode());
        hostOrderCompletion.tryCompleteHostOrder(shuttleOrder.getHostOrderKey());
    }

    /**
     * host 의 시험 대상 여부 — PASSED 가 아니면 보관 대기.
     */
    private boolean isHoldForTest(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) return false;
        TbWcsHostOrder h = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(h)) return false;
        if (!Boolean.TRUE.equals(h.getTestRequired())) return false;
        return !QcTestStatus.PASSED.code().equals(h.getTestStatus());
    }

    /**
     * 하드웨어 에러 — 산출된 INBOUND stock 정리 + 목적지 랙 격리.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc) {
        logger.error("[ Handler ][ Inbound ] failure - orderKey={}, stockId={}, errorCode={}, errorDesc={}",
                ValueUtil.isEmpty(shuttleOrder) ? "UNKNOWN" : shuttleOrder.getOrderKey(),
                shuttleOrder == null ? null : shuttleOrder.getCarryingStockId(),
                errorCode, errorDesc);

        if (ValueUtil.isEmpty(shuttleOrder)) return;

        // 재고 예약 해제
        if (ValueUtil.isNotEmpty(shuttleOrder.getCarryingStockId())) {
            reservationReleaseService.cancelInboundReservation(
                    shuttleOrder.getEqGroupId(),
                    shuttleOrder.getCarryingStockId(),
                    shuttleOrder.getToLocCode()
            );
        }

        // 목적지 랙 격리
        wcsLocationService.blockLocationOnError(
                shuttleOrder.getEqGroupId(),
                shuttleOrder.getToLocCode(),
                shuttleOrder.getOrderKey()
        );
    }

    /**
     * 작업 취소 — INBOUND stock 삭제 + 로케이션 unlock (목적지 + 포트).
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCancellation(TbWcsShuttleOrder shuttleOrder) {
        logger.info("[ Handler ][ Inbound ] cancel - orderKey={}, stockId={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getCarryingStockId());

        if (ValueUtil.isNotEmpty(shuttleOrder.getCarryingStockId())) {
            reservationReleaseService.cancelInboundReservation(
                    shuttleOrder.getEqGroupId(),
                    shuttleOrder.getCarryingStockId(),
                    shuttleOrder.getToLocCode()
            );
        }

        // 목적지 rack 락 해제
        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getToLocCode());
        // 포트 락 방어 해제 — ECS 콜백 미도착 시 잔류 방지
        if (ValueUtil.isNotEmpty(shuttleOrder.getFromLocCode())) {
            wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode());
        }
    }

    @Override
    public void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder) {
        logger.info("[ Handler ][ Inbound ] rackConveyorArrived - orderKey={}", shuttleOrder.getOrderKey());
    }
}
