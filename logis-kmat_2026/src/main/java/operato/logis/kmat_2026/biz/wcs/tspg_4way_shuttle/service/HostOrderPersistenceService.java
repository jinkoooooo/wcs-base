package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.HostOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommandItem;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsHostOrderItemService;
import operato.logis.kmat_2026.service.impl.TbWcsHostOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class HostOrderPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderPersistenceService.class);

    @Autowired
    private TbWcsHostOrderService hostOrderService;

    @Autowired
    private TbWcsHostOrderItemService hostOrderItemService;

    // =======================================================================
    // [1] 실제 HOST 주문 수신 처리 (Facade 등에서 호출)
    // - 호출자의 실패와 무관하게 이력은 남아야 하므로 REQUIRES_NEW 사용
    // =======================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TbWcsHostOrder saveHostOrderWithItems(HostOrderReceiveRequest request) {
        TbWcsHostOrder hostOrder = this.createHostOrder(request);
        this.createHostOrderItems(request, hostOrder);
        return hostOrder;
    }

    private TbWcsHostOrder createHostOrder(HostOrderReceiveRequest request) {
        TbWcsHostOrder order = new TbWcsHostOrder();
        order.setHostSystemCode(request.getHostSystemCode());
        order.setHostOrderKey(request.getHostOrderKey());
        order.setOrderType(request.getOrderType());
        order.setEqGroupId(request.getEqGroupId());
        order.setOrderStatus((Integer) HostOrderStatusEnumCode.RECEIVED.code());
        order.setPriority(request.getPriority() != null ? request.getPriority() : 5);
        order.setOwnerCode(request.getOwnerCode());
        order.setReceivedAt(OffsetDateTime.now());

        try { order.getClass().getMethod("setRawPayload", String.class).invoke(order, request.getRawPayload()); } catch (Exception ignored) {}

        hostOrderService.insert(order);
        logger.info("Created host order. hostSystemCode={}, hostOrderKey={}", order.getHostSystemCode(), order.getHostOrderKey());
        return order;
    }

    private List<TbWcsHostOrderItem> createHostOrderItems(HostOrderReceiveRequest request, TbWcsHostOrder hostOrder) {
        List<TbWcsHostOrderItem> result = new ArrayList<>();
        if (ValueUtil.isEmpty(request.getItems())) return result;

        for (HostOrderReceiveRequest.HostOrderItemRequest reqItem : request.getItems()) {
            if (reqItem == null) continue;
            TbWcsHostOrderItem item = new TbWcsHostOrderItem();
            item.setHostSystemCode(hostOrder.getHostSystemCode());
            item.setHostOrderKey(hostOrder.getHostOrderKey());
            item.setLineNo(reqItem.getLineNo());
            item.setSkuCode(reqItem.getSkuCode());
            item.setLotNo(reqItem.getLotNo());
            item.setQty(reqItem.getQty());
            item.setUom(reqItem.getUom());
            item.setRawAttr(reqItem.getRawAttr());

            hostOrderItemService.insert(item);
            result.add(item);
        }
        return result;
    }

    // =======================================================================
    // [2] HOST 상태 업데이트 로직
    // - 비즈니스 로직(WCS 롤백 등)과 분리하여 상태를 확정해야 하므로 REQUIRES_NEW 사용
    // =======================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markAllocated(TbWcsHostOrder hostOrder, String wcsOrderKey) {
        if (hostOrder == null) return;
        hostOrder.setOrderStatus((Integer) HostOrderStatusEnumCode.ALLOCATED.code());
        hostOrder.setWcsOrderKey(wcsOrderKey);
        hostOrderService.update(hostOrder);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markError(TbWcsHostOrder hostOrder, String errorCode, String errorDesc) {
        if (hostOrder == null) return;
        hostOrder.setOrderStatus((Integer) HostOrderStatusEnumCode.ERROR.code());
        hostOrder.setErrorCode(errorCode);
        hostOrder.setErrorDesc(errorDesc);
        hostOrderService.update(hostOrder);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateStatusByWcsOrderKey(String wcsOrderKey, int status) {
        TbWcsHostOrder hostOrder = hostOrderService.findByWcsOrderKey(wcsOrderKey);
        if (hostOrder == null) return;
        hostOrder.setOrderStatus(status);
        hostOrderService.update(hostOrder);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markErrorByWcsOrderKey(String wcsOrderKey, String errorCode, String errorDesc) {
        TbWcsHostOrder hostOrder = hostOrderService.findByWcsOrderKey(wcsOrderKey);
        if (hostOrder == null) return;
        hostOrder.setOrderStatus((Integer) HostOrderStatusEnumCode.ERROR.code());
        hostOrder.setErrorCode(errorCode);
        hostOrder.setErrorDesc(errorDesc);
        hostOrderService.update(hostOrder);
    }

    // =======================================================================
    // [3] 내부/시나리오용 가상 주문 생성 로직
    // - 범용 사용을 위해 기본 REQUIRED 적용 (호출자 트랜잭션이 있으면 합류, 없으면 새로 생성)
    // =======================================================================

    /**
     * [추가/수정됨] 범용적으로 안전하게 사용할 수 있는 가상 주문 생성 (Header + Item 원자성 보장)
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsHostOrder saveVirtualHostOrderWithItems(WcsOrderCommand command) {
        TbWcsHostOrder hostOrder = this.createVirtualHostOrderFromCommand(command);
        this.createVirtualHostOrderItemsFromCommand(command, hostOrder);
        return hostOrder;
    }

    private TbWcsHostOrder createVirtualHostOrderFromCommand(WcsOrderCommand command) {
        TbWcsHostOrder order = new TbWcsHostOrder();
        String hostSystemCode = StringUtils.hasText(command.getSourceSystemCode()) ? command.getSourceSystemCode() : "DIRECT";
        String hostOrderKey = StringUtils.hasText(command.getSourceOrderKey()) ? command.getSourceOrderKey() : "DIRECT-" + System.currentTimeMillis();

        order.setHostSystemCode(hostSystemCode);
        order.setHostOrderKey(hostOrderKey);
        order.setOrderType(command.getOrderType());
        order.setOrderStatus((Integer) HostOrderStatusEnumCode.RECEIVED.code());
        order.setPriority(command.getPriority() != null ? command.getPriority() : 5);
        order.setOwnerCode(command.getOwnerCode());
        order.setReceivedAt(OffsetDateTime.now());

        try { order.getClass().getMethod("setRawPayload", String.class).invoke(order, command.getRawPayload()); } catch (Exception ignored) {}

        hostOrderService.insert(order);
        return order;
    }

    private List<TbWcsHostOrderItem> createVirtualHostOrderItemsFromCommand(WcsOrderCommand command, TbWcsHostOrder hostOrder) {
        List<TbWcsHostOrderItem> result = new ArrayList<>();
        if (ValueUtil.isEmpty(command.getItems())) return result;

        for (WcsOrderCommandItem cmdItem : command.getItems()) {
            if (cmdItem == null) continue;
            TbWcsHostOrderItem item = new TbWcsHostOrderItem();
            item.setHostSystemCode(hostOrder.getHostSystemCode());
            item.setHostOrderKey(hostOrder.getHostOrderKey());
            item.setLineNo(cmdItem.getLineNo());
            item.setSkuCode(cmdItem.getSkuCode());
            item.setLotNo(cmdItem.getLotNo());
            item.setQty(cmdItem.getQty());
            item.setUom(cmdItem.getUom());
            item.setRawAttr(cmdItem.getRawAttr());

            hostOrderItemService.insert(item);
            result.add(item);
        }
        return result;
    }
}