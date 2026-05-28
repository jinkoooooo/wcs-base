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
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.allocation.location.MoveLocationAllocator;
import operato.logis.wcs.service.impl.allocation.location.ObstacleResolver;
import operato.logis.wcs.service.impl.inventory.reservation.OutboundReservationService;
import operato.logis.wcs.service.impl.inventory.reservation.ReservationReleaseService;
import operato.logis.wcs.service.impl.allocation.location.LocationService;
import operato.logis.wcs.common.util.generator.HostOrderKeyGenerator;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
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
 * 이동(MOVE / RELOCATION) 주문 처리 핸들러.
 *
 * 통일 정책:
 *   - reserveInventory: markRelocationPending 으로 IDLE → RELOCATION 즉시 전이.
 *                       fromLoc 의 stock_id 가 clear 되며 stockId 는 allocation 에 set.
 *   - createShuttleOrder: 무조건 order.carryingStockId = allocation.stockId
 *   - handleFromLoading: 상태 전이는 reserve 시점에 끝났으므로 로케이션 매핑 해제 + unlock 만
 *
 * 이동 상태 흐름:
 *   - reserveInventory  : IDLE → RELOCATION
 *   - handleFromLoading : (fromLoc clear)
 *   - handleCompletion  : RELOCATION → IDLE  (toLoc 재매핑)
 *   - releaseInventory  : RELOCATION → IDLE  (산출 롤백 — fromLoc 재매핑)
 *   - handleCancellation: RELOCATION → IDLE  (취소 — 픽업 전이면 fromLoc 재매핑)
 *   - handleFailure     : 픽업 전이면 fromLoc 복원 + 격리 / 픽업 후면 fromLoc 복원 + toLoc 격리
 *
 * MOVE 는 fromLocId / toLocId 양쪽 로케이션을 잠근다.
 */
@Component
@RequiredArgsConstructor
public class WcsMoveOrderHandler implements WcsOrderHandler {

    private static final Logger logger = LoggerFactory.getLogger(WcsMoveOrderHandler.class);

    protected final MoveLocationAllocator locationAllocator;
    protected final OutboundReservationService outboundReservationService;
    protected final ReservationReleaseService reservationReleaseService;
    protected final LocationService wcsLocationService;
    protected final ShuttleOrderRepository shuttleOrderRepository;
    protected final ShuttleOrderItemRepository shuttleOrderItemRepository;
    protected final HostOrderKeyGenerator orderKeyGenerator;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final ObstacleResolver obstacleResolver;
    private final LocationService lockService;

    @Override
    public boolean supports(String orderType) {
        return OrderType.MOVE.codeAsString().equalsIgnoreCase(orderType);
    }

    @Override
    public AllocationResult allocateLocation(WcsOrderCommand command) {
        return locationAllocator.allocate(command);
    }

    @Override
    public boolean lockOrderLocationForShuttleOrderCalculate(WcsOrderCommand command,
                                                             AllocationResult allocation,
                                                             String taskId) {
        lockService.lockForOrder(allocation, command.getOrderType(), taskId, command.getHostOrderKey());
        logger.info("[ Handler ][ Move ] location reserved - taskId={}", taskId);
        return true;
    }

    /**
     * 이동 산출 — markRelocationPending(IDLE → RELOCATION) 후 stockId 를 allocation 에 set.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveInventory(WcsOrderCommand command, AllocationResult allocation) {
        // 입력 가드
        if (ValueUtil.isEmpty(command)) {
            logger.error("[ Handler ][ Move ] reserve - empty command");
            return false;
        }
        if (ValueUtil.isEmpty(allocation) || !allocation.isSuccess()) {
            logger.error("[ Handler ][ Move ] reserve - allocation empty/unsuccessful");
            return false;
        }
        if (ValueUtil.isEmpty(allocation.getFromLocId())) {
            logger.error("[ Handler ][ Move ] reserve - fromLocId empty. hostOrderKey={}",
                    command.getHostOrderKey());
            return false;
        }
        if (ValueUtil.isEmpty(allocation.getToLocId())) {
            logger.error("[ Handler ][ Move ] reserve - toLocId empty. hostOrderKey={}",
                    command.getHostOrderKey());
            return false;
        }

        logger.info("[ Handler ][ Move ] reserve - hostOrderKey={}, fromLocId={}, toLocId={}",
                command.getHostOrderKey(),
                allocation.getFromLocId(),
                allocation.getToLocId());

        // IDLE → RELOCATION 전이
        String stockId = outboundReservationService.markRelocationPendingOnlyStatus(
                allocation.getEqGroupId(), allocation.getFromLocId());

        allocation.setStockId(stockId);

        logger.info("[ Handler ][ Move ] stock pending - hostOrderKey={}, stockId={}, fromLocId={}",
                command.getHostOrderKey(), stockId, allocation.getFromLocId());
        return true;
    }

    /**
     * 산출 롤백 (ECS 전송 실패 등) — RELOCATION → IDLE 복원 + fromLoc 재매핑.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseInventory(WcsOrderCommand command, AllocationResult allocation) {
        if (ValueUtil.isEmpty(allocation) || ValueUtil.isEmpty(allocation.getStockId())) {
            logger.debug("[ Handler ][ Move ] release - no reserved stock");
            return;
        }

        logger.info("[ Handler ][ Move ] restore stock - hostOrderKey={}, stockId={}, fromLocId={}",
                command == null ? null : command.getHostOrderKey(),
                allocation.getStockId(),
                allocation.getFromLocId());

        reservationReleaseService.restoreRelocation(
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
        order.setOrderType(OrderType.MOVE.codeAsString());
        order.setSubOrderType(SubOrderType.NORMAL.codeAsString());
        order.setOrderStatus(ShuttleOrderStatus.CREATED.codeAsIntOrNull());
        order.setPriority(ValueUtil.isEmpty(command.getPriority()) ? 0 : command.getPriority());
        order.setFromLocCode(allocation.getFromLocId());
        order.setToLocCode(allocation.getToLocId());
        order.setEqGroupId(allocation.getEqGroupId());
        order.setOwnerCode(command.getOwnerCode());
        order.setParentOrderKey(command.getParentOrderKey());
        order.setHostOrderKey(command.getHostOrderKey());
        order.setEcsIfStatus(EcsIfStatus.READY.codeAsIntOrNull());

        // fromLoc 정보로 level / barcode 채우기
        ExtTbInventoryLocation fromLocation = inventoryLocationRepository.findByEqGroupIdAndLocId(
                allocation.getEqGroupId(), allocation.getFromLocId());

        order.setLevel(fromLocation.getLocLevel());

        if (ValueUtil.isNotEmpty(command.getBarCode())) {
            order.setBarcode(command.getBarCode());
        } else if (ValueUtil.isNotEmpty(allocation.getStockId())) {
            // 안전장치 — command 에 barcode/items 둘 다 없으면 source stock 의 barcode 사용
            ExtTbInventoryStock stock = inventoryStockRepository.findById(allocation.getStockId());
            if (ValueUtil.isNotEmpty(stock) && ValueUtil.isNotEmpty(fromLocation.getBarcode())) {
                order.setBarcode(fromLocation.getBarcode());
            }
        }

        // reserveInventory 에서 잡은 stockId 를 무조건 order 에 기록
        if (ValueUtil.isNotEmpty(allocation.getStockId())) {
            order.setCarryingStockId(allocation.getStockId());
        }

        shuttleOrderRepository.insert(order);

        logger.info("[ Handler ][ Move ] shuttle order created - hostOrderKey={}, orderKey={}, stockId={}, fromLocId={}, toLocId={}",
                command.getHostOrderKey(), order.getOrderKey(), order.getCarryingStockId(),
                order.getFromLocCode(), order.getToLocCode());

        return order;
    }

    @Override
    public List<TbWcsShuttleOrderItem> createShuttleOrderItems(WcsOrderCommand command,
                                                               TbWcsShuttleOrder shuttleOrder) {
        List<TbWcsShuttleOrderItem> orderItems = new ArrayList<>();

        if (ValueUtil.isEmpty(command.getItems())) {
            logger.warn("[ Handler ][ Move ] no items in command - orderKey={}", shuttleOrder.getOrderKey());
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

        logger.info("[ Handler ][ Move ] items created - count={}, orderKey={}",
                orderItems.size(), shuttleOrder.getOrderKey());
        return orderItems;
    }

    @Override
    public List<RelocationTaskDto> resolveObstacles(WcsOrderCommand command, AllocationResult allocation) {
        return obstacleResolver.resolve("MOVE", command.getEqGroupId(), allocation.getFromLocId());
    }

    /**
     * 픽업 시점 — 상태 전이는 reserveInventory 에서 완료(IDLE → RELOCATION).
     * 여기서는 로케이션 매핑 해제 + fromLoc unlock 만.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFromLoadingComplete(TbWcsShuttleOrder shuttleOrder,
                                          List<TbWcsShuttleOrderItem> items,
                                          EcsCallbackApi.Request request) {
        logger.info("[ Handler ][ Move ] fromLoadingComplete - orderKey={}, fromLocId={}, stockId={}",
                shuttleOrder.getOrderKey(),
                shuttleOrder.getFromLocCode(),
                shuttleOrder.getCarryingStockId());

        outboundReservationService.clearLocationMappingOnPickup(
                shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode());

        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode());
    }

    @Override
    public void handleToUnloadingComplete(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        // 이동: to 에 화물 도착 — 처리 없음 (재고/로케이션은 COMPLETE 에서 처리)
        logger.info("[ Handler ][ Move ] toUnloadingComplete - orderKey={}, toLocId={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getToLocCode());
    }

    /**
     * 이동 완료 — RELOCATION → IDLE, toLoc 에 stockId/barcode 매핑.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        String stockId = shuttleOrder.getCarryingStockId();
        String eqGroupId = shuttleOrder.getEqGroupId();
        String fromLocId = shuttleOrder.getFromLocCode();
        String toLocId = shuttleOrder.getToLocCode();

        logger.info("[ Handler ][ Move ] complete - orderKey={}, carryingStockId={}",
                shuttleOrder.getOrderKey(), stockId);

        // 가드 — stockId 없거나 이미 IDLE 이면 락만 해제하고 종료
        if (ValueUtil.isEmpty(stockId)) {
            logger.warn("[ Handler ][ Move ] complete - no carryingStockId. orderKey={}", shuttleOrder.getOrderKey());
            wcsLocationService.unlockBoth(eqGroupId, fromLocId, toLocId);
            return;
        }

        StockStatus currentStatus = reservationReleaseService.getStockStatus(eqGroupId, stockId);
        if (currentStatus == StockStatus.IDLE) {
            logger.info("[ Handler ][ Move ] complete skip - already idle. orderKey={}", shuttleOrder.getOrderKey());
            wcsLocationService.unlockBoth(eqGroupId, fromLocId, toLocId);
            return;
        }

        // FROM_LOADING 콜백 누락 대비 멱등 호출
        outboundReservationService.clearLocationMappingOnPickup(eqGroupId, fromLocId);
        wcsLocationService.unlock(eqGroupId, fromLocId);

        // RELOCATION → IDLE + toLoc 재매핑
        outboundReservationService.finalizeRelocation(eqGroupId, toLocId, stockId, shuttleOrder.getBarcode());
        wcsLocationService.unlock(eqGroupId, toLocId);
    }

    /**
     * 하드웨어 에러:
     *   - 픽업 전: 상태 IDLE 복원 + 문제가 발생한 fromLoc 격리 (toLoc 은 단순 해제)
     *   - 픽업 후: fromLoc 으로 복원(수작업 원위치 가정) + 문제가 발생한 toLoc 격리
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc) {
        String orderKey = ValueUtil.isNotEmpty(shuttleOrder) ? shuttleOrder.getOrderKey() : "UNKNOWN";
        logger.error("[ Handler ][ Move ] failure - orderKey={}, errorCode={}, errorDesc={}",
                orderKey, errorCode, errorDesc);

        if (ValueUtil.isEmpty(shuttleOrder)) return;

        String stockId = shuttleOrder.getCarryingStockId();
        String eqGroupId = shuttleOrder.getEqGroupId();

        StockStatus current = ValueUtil.isNotEmpty(stockId)
                ? reservationReleaseService.getStockStatus(eqGroupId, stockId)
                : null;

        if (current == StockStatus.RELOCATION) {
            ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, shuttleOrder.getFromLocCode());
            boolean isPickedUp = (ValueUtil.isEmpty(loc) || !stockId.equals(loc.getStockId()));

            if (isPickedUp) {
                // 픽업 후 에러 (예: 목적지 이중입고) — 출발지 복원 + 목적지 격리
                reservationReleaseService.restoreRelocation(
                        eqGroupId, shuttleOrder.getFromLocCode(), stockId, shuttleOrder.getBarcode()
                );
                wcsLocationService.blockLocationOnError(eqGroupId, shuttleOrder.getToLocCode(), shuttleOrder.getOrderKey());
            } else {
                // 픽업 전 에러 (예: 출발지 공출고) — 상태 IDLE 복원 + 출발지 격리, 목적지는 정상 해제
                reservationReleaseService.restoreRelocation(
                        eqGroupId, shuttleOrder.getFromLocCode(), stockId, shuttleOrder.getBarcode()
                );
                wcsLocationService.blockLocationOnError(eqGroupId, shuttleOrder.getFromLocCode(), shuttleOrder.getOrderKey());
                wcsLocationService.unlock(eqGroupId, shuttleOrder.getToLocCode());
            }
        } else {
            // 이미 IDLE 또는 finalize 된 상태 → from 격리 + to unlock
            wcsLocationService.blockLocationOnError(eqGroupId, shuttleOrder.getFromLocCode(), shuttleOrder.getOrderKey());
            wcsLocationService.unlock(eqGroupId, shuttleOrder.getToLocCode());
        }
    }

    /**
     * 작업 취소:
     *   - 픽업 전: 상태를 IDLE 로 복원
     *   - 픽업 후: 이미 화물이 랙을 떠났으므로 복원 생략
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCancellation(TbWcsShuttleOrder shuttleOrder) {
        logger.info("[ Handler ][ Move ] cancel - orderKey={}, stockId={}",
                shuttleOrder.getOrderKey(), shuttleOrder.getCarryingStockId());

        String stockId = shuttleOrder.getCarryingStockId();
        String eqGroupId = shuttleOrder.getEqGroupId();

        StockStatus status = ValueUtil.isNotEmpty(stockId)
                ? reservationReleaseService.getStockStatus(eqGroupId, stockId)
                : null;

        if (status == StockStatus.RELOCATION) {
            ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, shuttleOrder.getFromLocCode());
            boolean isPickedUp = (ValueUtil.isEmpty(loc) || !stockId.equals(loc.getStockId()));

            if (!isPickedUp) {
                // 픽업 전 취소 — 상태를 IDLE 로 정상 복원
                reservationReleaseService.restoreRelocation(
                        eqGroupId, shuttleOrder.getFromLocCode(), stockId, shuttleOrder.getBarcode()
                );
            } else {
                // 픽업 후 취소 — 이미 셔틀이 들고 있으므로 재매핑 금지
                logger.warn("[ Handler ][ Move ] cancel - already picked up. skip restore. orderKey={}", shuttleOrder.getOrderKey());
            }
        }

        wcsLocationService.unlockBoth(
                eqGroupId, shuttleOrder.getFromLocCode(), shuttleOrder.getToLocCode()
        );
    }

    @Override
    public void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder) {
        logger.info("[ Handler ][ Move ] rackConveyorArrived - orderKey={}", shuttleOrder.getOrderKey());
    }
}
