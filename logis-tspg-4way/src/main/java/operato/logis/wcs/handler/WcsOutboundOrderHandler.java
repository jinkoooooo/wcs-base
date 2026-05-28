package operato.logis.wcs.handler;

import operato.logis.inventory.consts.StockStatus;
import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.EcsCallbackApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.allocation.location.ObstacleResolver;
import operato.logis.wcs.service.impl.allocation.location.OutboundLocationAllocator;
import operato.logis.wcs.service.impl.order.host.HostOrderCompletion;
import operato.logis.wcs.service.impl.inventory.reservation.OutboundReservationService;
import operato.logis.wcs.service.impl.inventory.reservation.ReservationReleaseService;
import operato.logis.wcs.service.impl.allocation.location.LocationService;
import operato.logis.wcs.common.util.generator.HostOrderKeyGenerator;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 출고(OUTBOUND) 주문 처리 핸들러.
 *
 * 자동 재입고는 사전 발급하지 않는다. 운영자가 [재동기화] 트리거 시점에
 * 단일 산출 경로(orderIntakeService.execute)로 신규 발급된다.
 */
@Component
@RequiredArgsConstructor
public class WcsOutboundOrderHandler implements WcsOrderHandler {

    private static final Logger logger = LoggerFactory.getLogger(WcsOutboundOrderHandler.class);

    protected final OutboundLocationAllocator locationAllocator;
    protected final OutboundReservationService outboundReservationService;
    protected final ReservationReleaseService reservationReleaseService;
    protected final LocationService wcsLocationService;
    protected final ShuttleOrderRepository shuttleOrderRepository;
    protected final ShuttleOrderItemRepository shuttleOrderItemRepository;
    protected final HostOrderKeyGenerator orderKeyGenerator;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final ObstacleResolver obstacleResolver;
    private final LocationService lockService;
    private final HostOrderCompletion hostOrderCompletion;

    @Override
    public boolean supports(String orderType) {
        return OrderType.OUTBOUND.codeAsString().equalsIgnoreCase(orderType);
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
        logger.info("[ Handler ][ Outbound ] location reserved - taskId={}", taskId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveInventory(WcsOrderCommand command, AllocationResult allocation) {
        // 입력 가드
        if (ValueUtil.isEmpty(command)) {
            logger.error("[ Handler ][ Outbound ] reserve - empty command");
            return false;
        }
        if (ValueUtil.isEmpty(allocation) || !allocation.isSuccess()) {
            logger.error("[ Handler ][ Outbound ] reserve - allocation empty/unsuccessful");
            return false;
        }
        if (ValueUtil.isEmpty(allocation.getFromLocId())) {
            logger.error("[ Handler ][ Outbound ] reserve - fromLocId empty. hostOrderKey={}",
                    command.getHostOrderKey());
            return false;
        }
        // 빈 파렛트 출고: items / ownerCode 가 비어있어도 허용 (fromLoc 의 stock 자체로 식별)

        logger.info("[ Handler ][ Outbound ] reserve - hostOrderKey={}, fromLocId={}, itemCount={}",
                command.getHostOrderKey(), allocation.getFromLocId(), command.getItems().size());

        // IDLE → OUTBOUND 전이
        String stockId = outboundReservationService.markOutboundPendingOnlyStatus(
                allocation.getEqGroupId(), allocation.getFromLocId());
        allocation.setStockId(stockId);

        logger.info("[ Handler ][ Outbound ] stock pending - hostOrderKey={}, stockId={}, fromLocId={}",
                command.getHostOrderKey(), stockId, allocation.getFromLocId());
        return true;
    }

    /**
     * 산출 롤백 (ECS 전송 실패 등) — OUTBOUND → IDLE 복원 + fromLoc 재매핑.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseInventory(WcsOrderCommand command, AllocationResult allocation) {
        if (ValueUtil.isEmpty(allocation) || ValueUtil.isEmpty(allocation.getStockId())) {
            logger.debug("[ Handler ][ Outbound ] release - no reserved stock");
            return;
        }

        logger.info("[ Handler ][ Outbound ] restore stock - hostOrderKey={}, stockId={}, fromLocId={}",
                command == null ? null : command.getHostOrderKey(),
                allocation.getStockId(),
                allocation.getFromLocId());

        reservationReleaseService.restoreOutbound(
                allocation.getEqGroupId(),
                allocation.getFromLocId(),
                allocation.getStockId(),
                command == null ? null : command.getBarCode()
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
        order.setOrderType(OrderType.OUTBOUND.codeAsString());

        // sub_order_type — command 우선, 없으면 NORMAL
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

        // fromLoc 정보 1회 조회 후 level/barcode 재사용
        ExtTbInventoryLocation fromLoc = inventoryLocationRepository.findByEqGroupIdAndLocId(
                allocation.getEqGroupId(), allocation.getFromLocId());

        order.setLevel(fromLoc != null ? fromLoc.getLocLevel() : null);

        // 출고 셔틀 barcode = 출발지 파렛트 바코드 (SKU 폴백 금지)
        String resolvedBarcode = resolveOutboundBarcode(command, allocation, fromLoc);
        if (ValueUtil.isNotEmpty(resolvedBarcode)) {
            order.setBarcode(resolvedBarcode);
        } else {
            logger.warn("[ Handler ][ Outbound ] barcode unresolved - orderKey={}, fromLocId={}, stockId={}",
                    wcsOrderKey, allocation.getFromLocId(), allocation.getStockId());
        }

        if (ValueUtil.isNotEmpty(allocation.getStockId())) {
            order.setCarryingStockId(allocation.getStockId());
        }
        if (ValueUtil.isNotEmpty(command.getParentOrderKey())) {
            order.setParentOrderKey(command.getParentOrderKey());
        }

        shuttleOrderRepository.insert(order);

        logger.info("[ Handler ][ Outbound ] shuttle order created - hostOrderKey={}, orderKey={}, sub={}, stockId={}, fromLocId={}, toLocId={}, barcode={}",
                command.getHostOrderKey(), order.getOrderKey(), order.getSubOrderType(),
                order.getCarryingStockId(), order.getFromLocCode(), order.getToLocCode(), order.getBarcode());
        return order;
    }

    /**
     * 출고 셔틀의 barcode (파렛트 바코드) 확정 — 우선순위:
     *   1) command.barCode 명시 지정
     *   2) fromLoc.barcode (BCR 스캔 시점에 매핑됨)
     *   3) stockId 로 location 재조회 후 barcode (fromLoc 와 다른 위치에 매핑된 경우 방어)
     */
    private String resolveOutboundBarcode(WcsOrderCommand command,
                                          AllocationResult allocation,
                                          ExtTbInventoryLocation fromLoc) {
        if (ValueUtil.isNotEmpty(command) && ValueUtil.isNotEmpty(command.getBarCode())) {
            return command.getBarCode();
        }
        if (ValueUtil.isNotEmpty(fromLoc) && ValueUtil.isNotEmpty(fromLoc.getBarcode())) {
            return fromLoc.getBarcode();
        }
        if (ValueUtil.isNotEmpty(allocation.getStockId())) {
            ExtTbInventoryLocation locByStock = inventoryLocationRepository.findByStockId(
                    allocation.getEqGroupId(), allocation.getStockId());
            if (ValueUtil.isNotEmpty(locByStock) && ValueUtil.isNotEmpty(locByStock.getBarcode())) {
                return locByStock.getBarcode();
            }
        }
        return null;
    }

    @Override
    public List<TbWcsShuttleOrderItem> createShuttleOrderItems(WcsOrderCommand command,
                                                               TbWcsShuttleOrder shuttleOrder) {
        List<TbWcsShuttleOrderItem> orderItems = new ArrayList<>();

        if (ValueUtil.isEmpty(command.getItems())) {
            logger.warn("[ Handler ][ Outbound ] no items in command - orderKey={}", shuttleOrder.getOrderKey());
            return orderItems;
        }

        for (WcsOrderCommand.Item commandItem : command.getItems()) {
            if (ValueUtil.isEmpty(commandItem)) continue;
            TbWcsShuttleOrderItem item = new TbWcsShuttleOrderItem();
            item.setOrderKey(shuttleOrder.getOrderKey());
            item.setItemCode(commandItem.getItemCode());
            item.setLotNo(commandItem.getLotNo());
            item.setQty(ValueUtil.isEmpty(commandItem.getQty()) ? 0 : commandItem.getQty());
            item.setUom(commandItem.getUom());
            item.setProduceDate(commandItem.getProduceDate());
            item.setExpiryDate(commandItem.getExpiryDate());
            item.setLineStatus(ShuttleOrderStatus.CREATED.codeAsIntOrNull());
            shuttleOrderItemRepository.insert(item);
            orderItems.add(item);
        }

        logger.info("[ Handler ][ Outbound ] items created - count={}, orderKey={}",
                orderItems.size(), shuttleOrder.getOrderKey());

        // 자동 재입고는 사전 발급하지 않는다. 운영자 트리거 시 orderIntakeService.execute() 로 신규 발급
        return orderItems;
    }

    @Override
    public List<RelocationTaskDto> resolveObstacles(WcsOrderCommand command, AllocationResult allocation) {
        return obstacleResolver.resolve("OUTBOUND", command.getEqGroupId(), allocation.getFromLocId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFromLoadingComplete(TbWcsShuttleOrder shuttleOrder,
                                          List<TbWcsShuttleOrderItem> items,
                                          EcsCallbackApi.Request request) {
        logger.info("[ Handler ][ Outbound ] fromLoadingComplete - orderKey={}, fromLocId={}, stockId={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getFromLocCode(), shuttleOrder.getCarryingStockId());

        outboundReservationService.clearLocationMappingOnPickup(
                shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode());
        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode());
    }

    @Override
    public void handleToUnloadingComplete(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        // 출고: 포트 도착 — 처리 없음
        logger.info("[ Handler ][ Outbound ] toUnloadingComplete - orderKey={}, toLocId={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getToLocCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        String stockId = shuttleOrder.getCarryingStockId();
        String eqGroupId = shuttleOrder.getEqGroupId();
        String fromLocId = shuttleOrder.getFromLocCode();

        if (ValueUtil.isEmpty(stockId)) {
            logger.warn("[ Handler ][ Outbound ] complete skip - no carryingStockId. orderKey={}",
                    shuttleOrder.getOrderKey());
            return;
        }

        // 멱등성 — 이미 finalize 됐으면 skip
        if (reservationReleaseService.isOutboundFinalized(eqGroupId, stockId)) {
            logger.info("[ Handler ][ Outbound ] complete skip - already finalized. orderKey={}", shuttleOrder.getOrderKey());
            return;
        }

        // 매핑 해제 (FROM_LOADING 콜백 누락 대비 — 이미 NULL 이면 no-op)
        outboundReservationService.clearLocationMappingOnPickup(eqGroupId, fromLocId);

        // 락 해제 (이미 풀려있어도 no-op)
        wcsLocationService.unlock(eqGroupId, fromLocId);

        // stock 행 삭제 + host order 완료 평가
        outboundReservationService.finalizeOutbound(eqGroupId, stockId);
        hostOrderCompletion.tryCompleteHostOrder(shuttleOrder.getHostOrderKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc) {
        String orderKey = ValueUtil.isNotEmpty(shuttleOrder) ? shuttleOrder.getOrderKey() : "UNKNOWN";
        logger.error("[ Handler ][ Outbound ] failure - orderKey={}, errorCode={}, errorDesc={}",
                orderKey, errorCode, errorDesc);

        if (ValueUtil.isEmpty(shuttleOrder)) return;

        String stockId = shuttleOrder.getCarryingStockId();
        String eqGroupId = shuttleOrder.getEqGroupId();

        // stockId 없으면 출발지 격리만
        if (ValueUtil.isEmpty(stockId)) {
            wcsLocationService.blockLocationOnError(eqGroupId, shuttleOrder.getFromLocCode(), shuttleOrder.getOrderKey());
            return;
        }

        // OUTBOUND 진행 중이면 픽업 여부에 따라 finalize/restore 분기
        StockStatus current = reservationReleaseService.getStockStatus(eqGroupId, stockId);
        if (current == StockStatus.OUTBOUND) {
            ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, shuttleOrder.getFromLocCode());
            boolean isPickedUp = (ValueUtil.isEmpty(loc) || !stockId.equals(loc.getStockId()));
            if (isPickedUp) {
                outboundReservationService.finalizeOutbound(eqGroupId, stockId);
            } else {
                reservationReleaseService.restoreOutbound(
                        eqGroupId, shuttleOrder.getFromLocCode(), stockId, shuttleOrder.getBarcode());
            }
        }
        wcsLocationService.blockLocationOnError(eqGroupId, shuttleOrder.getFromLocCode(), shuttleOrder.getOrderKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCancellation(TbWcsShuttleOrder shuttleOrder) {
        logger.info("[ Handler ][ Outbound ] cancel - orderKey={}, stockId={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getCarryingStockId());

        String stockId = shuttleOrder.getCarryingStockId();
        String eqGroupId = shuttleOrder.getEqGroupId();
        StockStatus status = ValueUtil.isNotEmpty(stockId)
                ? reservationReleaseService.getStockStatus(eqGroupId, stockId)
                : null;

        // OUTBOUND 진행 중이고 아직 픽업 전이면 출발지 재매핑
        if (status == StockStatus.OUTBOUND) {
            ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, shuttleOrder.getFromLocCode());
            boolean isPickedUp = (ValueUtil.isEmpty(loc) || !stockId.equals(loc.getStockId()));
            if (!isPickedUp) {
                reservationReleaseService.restoreOutbound(
                        eqGroupId, shuttleOrder.getFromLocCode(), stockId, shuttleOrder.getBarcode());
            } else {
                logger.warn("[ Handler ][ Outbound ] cancel - already picked up. skip restore. orderKey={}", shuttleOrder.getOrderKey());
            }
        }
        // rack 락 해제 (fromLocCode = 출발 rack)
        wcsLocationService.unlock(eqGroupId, shuttleOrder.getFromLocCode());
        // 포트 락 방어 해제 — ECS 송신 후 취소 시 포트 잔류 락 정리
        if (ValueUtil.isNotEmpty(shuttleOrder.getToLocCode())) {
            wcsLocationService.unlock(eqGroupId, shuttleOrder.getToLocCode());
        }
    }

    @Override
    public void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder) {
        logger.info("[ Handler ][ Outbound ] rackConveyorArrived - orderKey={}", shuttleOrder.getOrderKey());
    }
}
