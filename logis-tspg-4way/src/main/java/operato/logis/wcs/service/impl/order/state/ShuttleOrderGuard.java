package operato.logis.wcs.service.impl.order.state;

import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import xyz.elidom.exception.server.ElidomRuntimeException;

/**
 * 셔틀 주문 전제조건 가드.
 * ARRIVED 미도착 시 OUTBOUND_NOT_ARRIVED 던지는 throw 게이트를 한 곳으로 모은다.
 */
public final class ShuttleOrderGuard {

    private ShuttleOrderGuard() {}

    /** ARRIVED 이전이면 OUTBOUND_NOT_ARRIVED. actionPhrase 는 조사 포함 문구(예: "출고 확정이"). */
    public static void requireArrived(TbWcsShuttleOrder shuttle, String actionPhrase) {
        if (!ShuttleOrderStatus.isArrived(shuttle.getOrderStatus())) {
            throw new ElidomRuntimeException("OUTBOUND_NOT_ARRIVED",
                    "ECS 운송 완료(ARRIVED) 이후에만 " + actionPhrase
                            + " 가능합니다. (현재 상태 코드: " + shuttle.getOrderStatus() + ")");
        }
    }
}
