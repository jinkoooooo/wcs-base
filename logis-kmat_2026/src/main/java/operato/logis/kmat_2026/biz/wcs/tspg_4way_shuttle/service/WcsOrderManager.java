package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsIfStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.AllocationResult;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler.WcsOrderHandler;
import operato.logis.kmat_2026.common.util.OrderKeyGenerator;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.List;

@Component
public class WcsOrderManager {

    private static final Logger logger = LoggerFactory.getLogger(WcsOrderManager.class);

    @Autowired private WcsLocationService lockService;
    @Autowired private HostOrderPersistenceService hostOrderPersistenceService;
    @Autowired private TbWcsShuttleOrderService tbWcsShuttleOrderService;
    @Autowired private OrderKeyGenerator orderKeyGenerator;

    /**
     * [통합 트랜잭션] 할당 + 락 선점 + 재고 예약 + 주문 등록
     * 이 중 하나라도 실패하면 DB는 메서드 실행 전 상태로 자동 롤백되어 데이터 무결성이 보장됩니다.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderContext registerOrder(WcsOrderHandler handler, WcsOrderCommand command) {

        // 1. 로케이션 할당 (트랜잭션 내부에서 수행하여 Race Condition 방지)
        AllocationResult allocation = handler.allocateLocation(command);
        if (allocation == null || !allocation.isSuccess()) {
            throw new RuntimeException("가용 로케이션 또는 재고 할당에 실패했습니다.");
        }

        // 2. Wcs Order Key 생성
        String wcsOrderKey = orderKeyGenerator.generate("ORDER_KEY");
        command.setWcsOrderKey(wcsOrderKey);

        // 3. 로케이션 잠금 (선점)
        lockService.lockForOrder(allocation, command.getOrderType(), wcsOrderKey);

        // 4. Host 주문 이력 기록 (이미 Facade에서 저장했다면 update, 아니면 신규 insert)
        TbWcsHostOrder virtualHostOrder = null;
        if (command.isPersistHostOrder()) {
            virtualHostOrder = hostOrderPersistenceService.saveVirtualHostOrderWithItems(command);
        }

        // 5. 재고 예약 (실패 시 RuntimeException 발생 -> 1번 할당/3번 락까지 자동 롤백)
        handler.reserveInventory(command, allocation);

        // 6. Shuttle Order 및 Item 생성/저장
        TbWcsShuttleOrder order = handler.createShuttleOrder(command, allocation);
        List<TbWcsShuttleOrderItem> items = handler.createShuttleOrderItems(command, order);

        // 7. Host 주문 상태 업데이트 (비동기 처리 시 스케줄러가 이 상태를 보고 다음 단계 판단)
        if (virtualHostOrder != null) {
            hostOrderPersistenceService.markAllocated(virtualHostOrder, order.getOrderKey());
        }

        return new OrderContext(order, items);
    }

    /**
     * [전송 실패 처리] 재고와 락은 유지하고, 상태만 '전송 실패'로 변경하여
     * 나중에 스케줄러나 관리자가 재전송할 수 있게 합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSendFailure(String orderKey, String errorMsg) {
        TbWcsShuttleOrder order = tbWcsShuttleOrderService.findByOrderKey(orderKey);
        if (order != null) {
            order.setOrderStatus(ShuttleOrderStatusEnumCode.ERROR_SEND_FAIL.codeAsIntOrNull());
            order.setEcsIfStatus((Integer) EcsIfStatusEnumCode.FAIL.code());
             order.setRemark("ECS 전송 실패: " + errorMsg);
            tbWcsShuttleOrderService.update(order);
        }

        logger.info("[RETRY_REQUIRED] ECS 전송은 실패했으나 재고/락은 유지됨. 재전송 필요: orderKey={}", orderKey);
    }

    /**
     * [트랜잭션] 단순 데이터 생성 (Insert Only)
     * 로케이션 락과 재고 예약을 건너뛰고 DB에 오더 정보만 기록합니다.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderContext executeInsertOnly(WcsOrderHandler handler, WcsOrderCommand command) {
        // 1. 로케이션 할당 (Insert Only라도 어디로 갈지는 정해야 하므로 호출)
        AllocationResult allocation = handler.allocateLocation(command);
        if (allocation == null || !allocation.isSuccess()) {
            throw new RuntimeException("가용 로케이션 할당에 실패했습니다.");
        }

        // 2. Wcs Order Key 생성
        if (ValueUtil.isEmpty(command.getWcsOrderKey())) {
            command.setWcsOrderKey(orderKeyGenerator.generate("ORDER_KEY"));
        }

        // 3. Host 주문 이력 (설정 시)
        TbWcsHostOrder virtualHostOrder = null;
        if (command.isPersistHostOrder()) {
            virtualHostOrder = hostOrderPersistenceService.saveVirtualHostOrderWithItems(command);
        }

        // 4. Shuttle Order 및 Item 생성 (순수 Insert)
        TbWcsShuttleOrder order = handler.createShuttleOrder(command, allocation);
        List<TbWcsShuttleOrderItem> items = handler.createShuttleOrderItems(command, order);

        // 5. Host 주문 상태 업데이트
        if (virtualHostOrder != null) {
            hostOrderPersistenceService.markAllocated(virtualHostOrder, order.getOrderKey());
        }

        return new OrderContext(order, items);
    }

    public static class OrderContext {
        public final TbWcsShuttleOrder order;
        public final List<TbWcsShuttleOrderItem> items;
        public OrderContext(TbWcsShuttleOrder order, List<TbWcsShuttleOrderItem> items) {
            this.order = order;
            this.items = items;
        }
    }
}