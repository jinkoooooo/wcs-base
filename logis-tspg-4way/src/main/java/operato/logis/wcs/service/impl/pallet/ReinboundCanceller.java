package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 재입고 취소 (StateWriter).
 *
 * CREATED / SENT 상태의 재입고 셔틀만 취소 허용.
 * RUNNING 이상 또는 종료 상태에서는 거부.
 */
@Service
@RequiredArgsConstructor
public class ReinboundCanceller {

    private static final Logger logger = LoggerFactory.getLogger(ReinboundCanceller.class);

    private final ShuttleOrderStateWriter shuttleOrderStateWriter;
    private final OrderLookupUtils orderLookup;

    /**
     * 재입고 셔틀 취소 처리.
     * parent_order_key 가 있어야 재입고 — 일반 입고는 거부.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelReinbound(String orderKey, String reason) {
        requireNotEmpty(orderKey, "INVALID_PARAMETER", "재입고 주문번호가 입력되지 않았습니다.");
        TbWcsShuttleOrder s = orderLookup.getShuttleOrderOrThrow(orderKey, OrderType.INBOUND, "재입고");
        if (ValueUtil.isEmpty(s.getParentOrderKey())) {
            throw new ElidomRuntimeException("NOT_A_REINBOUND", "이 주문은 재입고 주문이 아닙니다.");
        }

        // CREATED / SENT 만 취소 허용
        Integer prev = s.getOrderStatus();
        Integer created = ShuttleOrderStatus.CREATED.codeAsIntOrNull();
        Integer sent = ShuttleOrderStatus.SENT.codeAsIntOrNull();
        if (!(created.equals(prev) || sent.equals(prev))) {
            throw new ElidomRuntimeException("REINBOUND_CANCEL_NOT_ALLOWED",
                    "이미 진행 중이거나 완료된 재입고는 취소할 수 없습니다. (현재 상태: " + prev + ")");
        }

        shuttleOrderStateWriter.markCancelled(s);
        logger.info("[ Pallet ][ Reinbound ] cancelled - orderKey={}, parent={}, reason={}",
                orderKey, s.getParentOrderKey(), reason);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("cancelled", true);
        r.put("orderKey", orderKey);
        r.put("parentOrderKey", s.getParentOrderKey());
        r.put("previousStatus", prev);
        r.put("userMessage", "재입고가 취소되었습니다.");
        return r;
    }
}
