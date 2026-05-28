package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler;

import operato.logis.kmat_2026.common.util.OrderKeyGenerator;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsIfStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.AllocationResult;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommandItem;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.WcsInventoryReservationService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.WcsLocationService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.LocationAllocator.WcsMoveLocationAllocator;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderItemService;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ====================================================================
 * 이동(MOVE) 주문 처리 핸들러
 * ====================================================================
 *
 * [역할]
 * - 공통 입력 DTO(WcsOrderCommand)를 이동 Shuttle 작업 지시로 변환
 * - 출발/목적 로케이션 할당, 재고 예약, Shuttle Order 생성, 완료/실패/취소 후처리를 담당
 *
 * [정책]
 * - MOVE는 fromLocCode를 기준으로 출발지 재고를 예약한다.
 * - 완료 시 fromLocCode 재고 감소 + toLocCode 재고 증가를 수행한다.
 * - 실패/취소 시 fromLocCode 기준 예약 해제한다.
 * - 양쪽 로케이션 잠금 해제가 필요하다.
 */
@Component
public class WcsMoveOrderHandler implements WcsOrderHandler {

    private static final Logger logger = LoggerFactory.getLogger(WcsMoveOrderHandler.class);

    @Autowired
    protected WcsMoveLocationAllocator locationAllocator;

    @Autowired
    protected WcsInventoryReservationService wcsInventoryReservationService;

    @Autowired
    protected WcsLocationService wcsLocationService;

    @Autowired
    protected TbWcsShuttleOrderService shuttleOrderService;

    @Autowired
    protected TbWcsShuttleOrderItemService shuttleOrderItemService;

    @Autowired
    protected OrderKeyGenerator orderKeyGenerator;

    @Override
    public boolean supports(String orderType) {
        return OrderTypeEnumCode.MOVE.codeAsString().equalsIgnoreCase(orderType);
    }

    @Override
    public AllocationResult allocateLocation(WcsOrderCommand command) {
        return locationAllocator.allocate(command);
    }

    @Override
    public boolean reserveInventory(WcsOrderCommand command, AllocationResult allocation) {
        if (ValueUtil.isEmpty(command)) {
            logger.error("Reserve inventory failed: command is empty");
            return false;
        }

        if (ValueUtil.isEmpty(allocation) || !allocation.isSuccess()) {
            logger.error("Reserve inventory failed: allocation is empty or unsuccessful");
            return false;
        }

        if (ValueUtil.isEmpty(allocation.getFromLocCode())) {
            logger.error("Reserve inventory failed: fromLocCode is empty. sourceOrderKey={}", command.getSourceOrderKey());
            return false;
        }

        if (ValueUtil.isEmpty(allocation.getToLocCode())) {
            logger.error("Reserve inventory failed: toLocCode is empty. sourceOrderKey={}", command.getSourceOrderKey());
            return false;
        }

        String eqGroupId = command.getEqGroupId();
        String ownerCode = command.getOwnerCode();
        String fromLocCode = allocation.getFromLocCode();
        String toLocCode = allocation.getToLocCode();

        logger.info(
                "Reserving inventory for move by both locations. sourceOrderKey={}, eqGroupId={}, ownerCode={}, fromLocCode={}, toLocCode={}",
                command.getSourceOrderKey(),
                eqGroupId,
                ownerCode,
                fromLocCode,
                toLocCode
        );

        // MOVE는 fromLocCode의 재고만 예약한다.
        // toLocCode는 이동 목적지(빈 로케이션)이므로 예약 대상이 아니다.
        boolean fromReserved = wcsInventoryReservationService.reserveAllInventoryByLocation(
                eqGroupId,
                ownerCode,
                fromLocCode
        );

        if (!fromReserved) {
            logger.error(
                    "Reserve inventory failed on from location. sourceOrderKey={}, eqGroupId={}, fromLocCode={}",
                    command.getSourceOrderKey(),
                    eqGroupId,
                    fromLocCode
            );
            return false;
        }

        return true;
    }

    @Override
    public void releaseInventory(WcsOrderCommand command, AllocationResult allocation) {
        if (ValueUtil.isEmpty(command) || ValueUtil.isEmpty(allocation) || !allocation.isSuccess()) {
            return;
        }
        if (!org.springframework.util.StringUtils.hasText(allocation.getFromLocCode())) {
            return;
        }

        logger.info("Releasing move inventory reservation. sourceOrderKey={}, eqGroupId={}, fromLocCode={}",
                command.getSourceOrderKey(), command.getEqGroupId(), allocation.getFromLocCode());

        wcsInventoryReservationService.releaseAllInventoryByLocation(
                command.getEqGroupId(),
                command.getOwnerCode(),
                allocation.getFromLocCode()
        );
    }

    @Override
    public TbWcsShuttleOrder createShuttleOrder(WcsOrderCommand command, AllocationResult allocation) {
        String wcsOrderKey = ValueUtil.isEmpty(command.getWcsOrderKey()) ?
                orderKeyGenerator.generate("ORDER_KEY") :
                command.getWcsOrderKey();

        TbWcsShuttleOrder order = new TbWcsShuttleOrder();
        order.setOrderKey(wcsOrderKey);
        order.setOrderType(OrderTypeEnumCode.MOVE.codeAsString());
        order.setOrderStatus(ShuttleOrderStatusEnumCode.CREATED.codeAsIntOrNull());
        order.setPriority(command.getPriority() == null ? 0 : command.getPriority());
        order.setFromLocCode(allocation.getFromLocCode());
        order.setToLocCode(allocation.getToLocCode());
        order.setEqGroupId(allocation.getEqGroupId());
        order.setOwnerCode(command.getOwnerCode());
        order.setEcsIfStatus(EcsIfStatusEnumCode.READY.codeAsIntOrNull());

        if(ValueUtil.isNotEmpty(command.getBarCode())){
            order.setBarcode(command.getBarCode());
        }else{
            if (!command.getItems().isEmpty()) {
                order.setBarcode(command.getItems().get(0).getSkuCode());
            }
        }

        shuttleOrderService.insert(order);

        logger.info(
                "Created move shuttle order. sourceOrderKey={}, shuttleOrderKey={}, fromLocCode={}, toLocCode={}, eqGroupId={}",
                command.getSourceOrderKey(),
                order.getOrderKey(),
                order.getFromLocCode(),
                order.getToLocCode(),
                order.getEqGroupId()
        );

        return order;
    }

    @Override
    public List<TbWcsShuttleOrderItem> createShuttleOrderItems(WcsOrderCommand command,
                                                               TbWcsShuttleOrder shuttleOrder) {
        List<TbWcsShuttleOrderItem> orderItems = new ArrayList<>();

        if (ValueUtil.isEmpty(command.getItems())) {
            logger.warn("No command items for move shuttle order items. orderKey={}", shuttleOrder.getOrderKey());
            return orderItems;
        }

        for (WcsOrderCommandItem commandItem : command.getItems()) {
            if (commandItem == null) {
                continue;
            }

            TbWcsShuttleOrderItem item = new TbWcsShuttleOrderItem();
            item.setOrderKey(shuttleOrder.getOrderKey());
            item.setLineNo(commandItem.getLineNo() == null ? 0 : commandItem.getLineNo());
            item.setSkuCode(commandItem.getSkuCode());
            item.setLotNo(commandItem.getLotNo());
            item.setQty(commandItem.getQty() == null ? 0 : commandItem.getQty());
            item.setUom(commandItem.getUom());
            item.setLineStatus(ShuttleOrderStatusEnumCode.CREATED.codeAsIntOrNull());

            shuttleOrderItemService.insert(item);
            orderItems.add(item);
        }

        logger.info(
                "Created {} move shuttle order items. orderKey={}",
                orderItems.size(),
                shuttleOrder.getOrderKey()
        );

        return orderItems;
    }

    @Override
    public void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        logger.info("Handling move completion: orderKey={}", shuttleOrder.getOrderKey());

        wcsInventoryReservationService.transferAllReservedInventoryByLocation(
                shuttleOrder.getEqGroupId(),
                shuttleOrder.getOwnerCode(),
                shuttleOrder.getFromLocCode(),
                shuttleOrder.getToLocCode()
        );

        wcsLocationService.unlockBoth(
                shuttleOrder.getEqGroupId(),
                shuttleOrder.getFromLocCode(), LocStatusEnumCode.EMPTY,
                shuttleOrder.getToLocCode(),   LocStatusEnumCode.OCCUPIED
        );
    }

    @Override
    public void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc) {
        logger.error(
                "Handling move failure: orderKey={}, errorCode={}, errorDesc={}",
                shuttleOrder.getOrderKey(),
                errorCode,
                errorDesc
        );

        // fromLocCode 예약만 해제 (toLocCode는 예약하지 않았으므로 해제 불필요)
        wcsInventoryReservationService.releaseAllInventoryByLocation(
                shuttleOrder.getEqGroupId(),
                shuttleOrder.getOwnerCode(),
                shuttleOrder.getFromLocCode()
        );

        wcsLocationService.unlockBoth(
                shuttleOrder.getEqGroupId(),
                shuttleOrder.getFromLocCode(), LocStatusEnumCode.OCCUPIED,
                shuttleOrder.getToLocCode(), LocStatusEnumCode.EMPTY
        );
    }

    @Override
    public void handleCancellation(TbWcsShuttleOrder shuttleOrder) {
        logger.info("Handling move cancellation: orderKey={}", shuttleOrder.getOrderKey());

        // fromLocCode 예약만 해제 (toLocCode는 예약하지 않았으므로 해제 불필요)
        wcsInventoryReservationService.releaseAllInventoryByLocation(
                shuttleOrder.getEqGroupId(),
                shuttleOrder.getOwnerCode(),
                shuttleOrder.getFromLocCode()
        );

        wcsLocationService.unlockBoth(
                shuttleOrder.getEqGroupId(),
                shuttleOrder.getFromLocCode(), LocStatusEnumCode.OCCUPIED,
                shuttleOrder.getToLocCode(), LocStatusEnumCode.EMPTY
        );
    }

    @Override
    public void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder) {
        logger.info("Handling outbound rack conveyor arrived: orderKey={}", shuttleOrder.getOrderKey());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}