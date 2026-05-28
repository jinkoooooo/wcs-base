package operato.logis.wcs.service.impl.order.intake;

import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;

import java.util.List;

/**
 * 단일 shuttle 주문 + items + (선택) 자식 sub-orders 의 트리 노드.
 * 재귀 트리 구조로 방해물 MOVE 자식 등이 subOrders 에 매달린다.
 */
public record ShuttleOrderUnit(
        TbWcsShuttleOrder order,
        List<TbWcsShuttleOrderItem> items,
        List<ShuttleOrderUnit> subOrders) {

    public ShuttleOrderUnit(TbWcsShuttleOrder order, List<TbWcsShuttleOrderItem> items) {
        this(order, items, null);
    }
}
