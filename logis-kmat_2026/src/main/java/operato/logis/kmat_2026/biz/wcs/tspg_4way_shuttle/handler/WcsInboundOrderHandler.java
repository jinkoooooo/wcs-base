package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler;

import operato.logis.kmat_2026.common.util.OrderKeyGenerator;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsIfStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.AllocationResult;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommandItem;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.LocationAllocator.WcsInboundLocationAllocator;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.WcsInventoryReservationService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.WcsLocationService;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ====================================================================
 * 입고(INBOUND) 주문 처리 핸들러
 * ====================================================================
 *
 * [역할]
 * - HOST 시스템에서 받은 입고 주문을 Shuttle 작업 지시로 변환
 * - 입고 로케이션 할당, Shuttle Order 생성, 완료/실패/취소 처리
 *
 * [입고 프로세스 흐름]
 * 1. HOST → WCS: 입고 주문 수신 (HostOrderController)
 * 2. WCS: 입고 가능한 빈 로케이션 할당 (InboundLocationAllocator)
 * 3. WCS: Shuttle Order 생성 및 DB 저장
 * 4. WCS → ECS: 작업 지시 전송 (EcsCommandService)
 * 5. ECS → WCS: 작업 완료/실패 콜백 (EcsCallbackController)
 * 6. WCS: 재고 생성 및 HOST 결과 전송 (HostCallbackService)
 *
 * [입고 특징]
 * - 재고 예약 불필요 (새로운 재고 생성)
 * - fromLocCode: 입고 스테이션 (컨베이어 끝점)
 * - toLocCode: 랙 내 저장 위치
 *
 * @author WCS Development Team
 * @since 2026-03-04
 */
@Component
public class WcsInboundOrderHandler implements WcsOrderHandler {

    private static final Logger logger = LoggerFactory.getLogger(WcsInboundOrderHandler.class);

    @Autowired
    private WcsInboundLocationAllocator locationAllocator;

    @Autowired
    private WcsInventoryReservationService wcsInventoryReservationService;

    @Autowired
    private WcsLocationService wcsLocationService;

    @Autowired
    private TbWcsShuttleOrderService shuttleOrderService;

    @Autowired
    private TbWcsShuttleOrderItemService shuttleOrderItemService;

    @Autowired
    protected OrderKeyGenerator orderKeyGenerator;

    @Override
    public boolean supports(String orderType) {
        return OrderTypeEnumCode.INBOUND.codeAsString().equalsIgnoreCase(orderType);    }

    @Override
    public AllocationResult allocateLocation(WcsOrderCommand command) {
        return locationAllocator.allocate(command);
    }

    @Override
    public boolean reserveInventory(WcsOrderCommand command, AllocationResult allocation) {
        // 입고는 재고 예약 불필요 (새 재고 생성)
        logger.debug("Inbound order does not require inventory reservation");
        return true;
    }

    @Override
    public void releaseInventory(WcsOrderCommand command, AllocationResult allocation) {
        // 입고는 예약한 재고가 없으므로 no-op
        logger.debug("Inbound order does not have inventory reservation to release");
    }

    @Override
    public TbWcsShuttleOrder createShuttleOrder(WcsOrderCommand command, AllocationResult allocation) {
        String wcsOrderKey = ValueUtil.isEmpty(command.getWcsOrderKey()) ?
                orderKeyGenerator.generate("ORDER_KEY") :
                command.getWcsOrderKey();

        TbWcsShuttleOrder order = new TbWcsShuttleOrder();
        order.setOrderKey(wcsOrderKey);
        order.setOrderType(OrderTypeEnumCode.INBOUND.codeAsString());
        order.setOrderStatus((Integer) ShuttleOrderStatusEnumCode.CREATED.code());
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
        logger.info("Created inbound shuttle order: orderKey={}", order.getOrderKey());
        return order;
    }

    @Override
    public List<TbWcsShuttleOrderItem> createShuttleOrderItems(WcsOrderCommand command,
                                                               TbWcsShuttleOrder shuttleOrder) {

        if(ValueUtil.isEmpty(command) || ValueUtil.isEmpty(command.getItems())) {
            logger.info("Inbound order does not exist items");
            return new ArrayList<>();
        }

        List<TbWcsShuttleOrderItem> orderItems = new ArrayList<>();
        for (WcsOrderCommandItem commandItem : command.getItems()) {
            TbWcsShuttleOrderItem item = new TbWcsShuttleOrderItem();
            item.setOrderKey(shuttleOrder.getOrderKey());
            item.setLineNo(commandItem.getLineNo());
            item.setSkuCode(commandItem.getSkuCode());
            item.setLotNo(commandItem.getLotNo());
            item.setQty(commandItem.getQty());
            item.setUom(commandItem.getUom());
            item.setLineStatus(ShuttleOrderStatusEnumCode.CREATED.codeAsIntOrNull());

            shuttleOrderItemService.insert(item);
            orderItems.add(item);
        }
        logger.info("Created {} inbound shuttle order items", orderItems.size());
        return orderItems;
    }

    @Override
    public void handleCompletion(TbWcsShuttleOrder shuttleOrder, List<TbWcsShuttleOrderItem> items) {
        logger.info("Handling inbound completion: orderKey={}", shuttleOrder.getOrderKey());

        // 입고 확정: 재고 생성
        for (TbWcsShuttleOrderItem item : items) {
            wcsInventoryReservationService.confirmInbound(
                    shuttleOrder.getEqGroupId(),
                    shuttleOrder.getToLocCode(),
                    shuttleOrder.getOwnerCode(),
                    item.getSkuCode(),
                    item.getLotNo(),
                    item.getQty()
            );
        }

        // 로케이션 잠금 해제 && loc_status → OCCUPIED (재고가 생겼으니 출고 가능 상태로)
        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getToLocCode(),LocStatusEnumCode.OCCUPIED);
    }

    @Override
    public void handleFailure(TbWcsShuttleOrder shuttleOrder, String errorCode, String errorDesc) {
        logger.error("Handling inbound failure: orderKey={}, error={}:{}",
                shuttleOrder.getOrderKey(), errorCode, errorDesc);

        // 로케이션 잠금 해제 && loc_status → EMPTY (원복)
        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getToLocCode(), LocStatusEnumCode.EMPTY);
    }

    @Override
    public void handleCancellation(TbWcsShuttleOrder shuttleOrder) {
        logger.info("Handling inbound cancellation: orderKey={}", shuttleOrder.getOrderKey());

        // 로케이션 잠금 해제 && loc_status → EMPTY (원복)
        wcsLocationService.unlock(shuttleOrder.getEqGroupId(), shuttleOrder.getToLocCode(), LocStatusEnumCode.EMPTY);
    }

    @Override
    public void handleRackConveyorArrived(TbWcsShuttleOrder shuttleOrder) {
        logger.info("Handling outbound rack conveyor arrived: orderKey={}", shuttleOrder.getOrderKey());
    }
}
