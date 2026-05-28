package operato.logis.wcs.service.impl.order.intake;

import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * shuttle 등록 결과 컨테이너 — 재귀 트리(부모 + 방해물 MOVE 자식들) 평탄화 조회 제공.
 */
public class OrderContext {

    public final List<ShuttleOrderUnit> shuttles;

    public OrderContext(List<ShuttleOrderUnit> shuttles) {
        this.shuttles = ValueUtil.isEmpty(shuttles) ? new ArrayList<>() : shuttles;
    }

    public OrderContext(ShuttleOrderUnit single) {
        this(ValueUtil.isEmpty(single) ? new ArrayList<>() : new ArrayList<>(List.of(single)));
    }

    public TbWcsShuttleOrder firstOrder() {
        return ValueUtil.isEmpty(shuttles) ? null : shuttles.get(0).order();
    }

    public String firstOrderKey() {
        TbWcsShuttleOrder o = firstOrder();
        return ValueUtil.isEmpty(o) ? null : o.getOrderKey();
    }

    public List<TbWcsShuttleOrder> getAllOrders() {
        List<TbWcsShuttleOrder> all = new ArrayList<>();
        collectOrders(shuttles, all);
        return all;
    }

    public List<TbWcsShuttleOrderItem> getAllItems() {
        List<TbWcsShuttleOrderItem> all = new ArrayList<>();
        collectItems(shuttles, all);
        return all;
    }

    /**
     * 재귀 평탄화 — 부모 + 자식 트리 전체 shuttle 수집.
     */
    private static void collectOrders(List<ShuttleOrderUnit> units, List<TbWcsShuttleOrder> out) {
        if (ValueUtil.isEmpty(units)) return;
        for (ShuttleOrderUnit u : units) {
            if (ValueUtil.isEmpty(u)) continue;
            if (ValueUtil.isNotEmpty(u.order())) out.add(u.order());
            collectOrders(u.subOrders(), out);
        }
    }

    /**
     * 재귀 평탄화 — 부모 + 자식 트리 전체 items 수집.
     */
    private static void collectItems(List<ShuttleOrderUnit> units, List<TbWcsShuttleOrderItem> out) {
        if (ValueUtil.isEmpty(units)) return;
        for (ShuttleOrderUnit u : units) {
            if (ValueUtil.isEmpty(u)) continue;
            if (ValueUtil.isNotEmpty(u.items())) out.addAll(u.items());
            collectItems(u.subOrders(), out);
        }
    }
}
