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
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.LocationAllocator.WcsOutboundLocationAllocator;
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
 * 출고(OUTBOUND) 주문 처리 핸들러
 * ====================================================================
 *
 * [역할]
 * - 공통 입력 DTO(WcsOrderCommand)를 출고 Shuttle 작업 지시로 변환
 * - 출고 로케이션 할당, 재고 예약, Shuttle Order 생성, 완료/실패/취소 후처리를 담당
 *
 * [정책]
 * - 하나의 주문 = 하나의 shuttleOrder = 하나의 fromLocCode
 * - 재고 예약/차감/해제는 allocation.inventoryId 가 아니라
 *   allocation.fromLocCode 기준으로 item별 수행
 */
@Component
public class WcsOutboundOrderHandler implements WcsOrderHandler {

    private static final Logger logger = LoggerFactory.getLogger(WcsOutboundOrderHandler.class);

    @Autowired
    protected WcsOutboundLocationAllocator locationAllocator;

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
        return OrderTypeEnumCode.OUTBOUND.codeAsString().equalsIgnoreCase(orderType);
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

        if (ValueUtil.isEmpty(command.getItems())) {
            logger.error("Reserve inventory failed: command items are empty. sourceOrderKey={}", command.getSourceOrderKey());
            return false;
        }

        if (ValueUtil.isEmpty(command.getOwnerCode())) {
            logger.error("Reserve inventory failed: ownerCode is empty. sourceOrderKey={}", command.getSourceOrderKey());
            return false;
        }

        if (ValueUtil.isEmpty(allocation.getFromLocCode())) {
            logger.error("Reserve inventory failed: fromLocCode is empty. sourceOrderKey={}", command.getSourceOrderKey());
            return false;
        }

        logger.info(
                "Reserving inventory for outbound. sourceOrderKey={}, ownerCode={}, fromLocCode={}, itemCount={}",
                command.getSourceOrderKey(),
                command.getOwnerCode(),
                allocation.getFromLocCode(),
                command.getItems().size()
        );

        return wcsInventoryReservationService.reserveForItems(
                command.getEqGroupId(),
                command.getOwnerCode(),
                allocation.getFromLocCode(),
                command.getItems()
        );
    }

    @Override
    public void releaseInventory(WcsOrderCommand command, AllocationResult allocation) {
        if (ValueUtil.isEmpty(command) || ValueUtil.isEmpty(allocation) || !allocation.isSuccess()) {
            return;
        }
        if (ValueUtil.isEmpty(command.getItems())) {
            return;
        }

        logger.info("Releasing outbound inventory reservation. sourceOrderKey={}, fromLocCode={}",
                command.getSourceOrderKey(), allocation.getFromLocCode());

        wcsInventoryReservationService.releaseForItems(
                command.getEqGroupId(),
                command.getOwnerCode(),
                allocation.getFromLocCode(),
                command.getItems()
        );
    }

    @Override
    public TbWcsShuttleOrder createShuttleOrder(WcsOrderCommand command, AllocationResult allocation) {
        String wcsOrderKey = ValueUtil.isEmpty(command.getWcsOrderKey()) ?
                orderKeyGenerator.generate("ORDER_KEY") :
                command.getWcsOrderKey();

        TbWcsShuttleOrder order = new TbWcsShuttleOrder();
        order.setOrderKey(wcsOrderKey);
        order.setOrderType(OrderTypeEnumCode.OUTBOUND.codeAsString());
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
                "Created outbound shuttle order. sourceOrderKey={}, shuttleOrderKey={}, fromLocCode={}, toLocCode={}, eqGroupId={}",
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
            logger.warn("No command items for outbound shuttle order items. orderKey={}", shuttleOrder.getOrderKey());
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
                "Created {} outbound shuttle order items. orderKey={}",
                orderItems.size(),
                shuttleOrder.getOrderKey()
        );

        return orderItems;
    }

    @Override
    public void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        logger.info("Handling outbound completion: orderKey={}", shuttleOrder.getOrderKey());

        if (!ValueUtil.isEmpty(items)) {
            for (TbWcsShuttleOrderItem item : items) {
                if (item == null) {
                    continue;
                }

                wcsInventoryReservationService.confirmOutboundByLocCode(
                        shuttleOrder.getEqGroupId(),
                        shuttleOrder.getFromLocCode(),
                        shuttleOrder.getOwnerCode(),
                        item.getSkuCode(),
                        item.getLotNo(),
                        safeInt(item.getQty())
                );
            }
        }

        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode(), LocStatusEnumCode.EMPTY);
    }

    @Override
    public void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc) {
        logger.error(
                "Handling outbound failure: orderKey={}, errorCode={}, errorDesc={}",
                shuttleOrder.getOrderKey(),
                errorCode,
                errorDesc
        );

        List<TbWcsShuttleOrderItem> items = shuttleOrderItemService.findByOrderKey(shuttleOrder.getOrderKey());
        if (!ValueUtil.isEmpty(items)) {
            for (TbWcsShuttleOrderItem item : items) {
                if (item == null) {
                    continue;
                }

                wcsInventoryReservationService.releaseByLocCode(
                        shuttleOrder.getEqGroupId(),
                        shuttleOrder.getFromLocCode(),
                        shuttleOrder.getOwnerCode(),
                        item.getSkuCode(),
                        item.getLotNo(),
                        safeInt(item.getQty())
                );
            }
        }

        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode(), LocStatusEnumCode.OCCUPIED);
    }

    @Override
    public void handleCancellation(TbWcsShuttleOrder shuttleOrder) {
        logger.info("Handling outbound cancellation: orderKey={}", shuttleOrder.getOrderKey());

        List<TbWcsShuttleOrderItem> items = shuttleOrderItemService.findByOrderKey(shuttleOrder.getOrderKey());
        if (!ValueUtil.isEmpty(items)) {
            for (TbWcsShuttleOrderItem item : items) {
                if (item == null) {
                    continue;
                }

                wcsInventoryReservationService.releaseByLocCode(
                        shuttleOrder.getEqGroupId(),
                        shuttleOrder.getFromLocCode(),
                        shuttleOrder.getOwnerCode(),
                        item.getSkuCode(),
                        item.getLotNo(),
                        safeInt(item.getQty())
                );
            }
        }

        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getFromLocCode(), LocStatusEnumCode.OCCUPIED);
    }

    @Override
    public void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder) {
        logger.info("Handling outbound rack conveyor arrived: orderKey={}", shuttleOrder.getOrderKey());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}