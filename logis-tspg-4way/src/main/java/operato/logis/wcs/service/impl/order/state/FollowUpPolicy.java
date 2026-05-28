package operato.logis.wcs.service.impl.order.state;

import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

/**
 * 출고가 후속 작업(재입고 등)을 필요로 하는지 평가 — 출고 COMPLETED 시점에 호출된다.
 *
 * 호출자: ShuttleOrderStateWriter.markCompleted. 결과가 true 면
 * tb_wcs_shuttle_order.follow_up_since 를 set 한다.
 *
 * PortLockPolicy 와 평행 패턴이지만 분리 유지:
 *   - PortLockPolicy : ECS 송신 시점 결정 (transit 동안 다른 출고 차단).
 *                       모집단 = NORMAL partial + paired reinbound.
 *   - FollowUpPolicy : 출고 확정 시점 결정 (확정 후 재입고 펜딩 알람).
 *                       모집단 = NORMAL partial + SAMPLE_OUT.
 *
 * 타이밍·모집단·목적 모두 다르므로 한 클래스로 합치지 않는다.
 */
@Component
public class FollowUpPolicy {

    private static final Logger logger = LoggerFactory.getLogger(FollowUpPolicy.class);

    /**
     * 후속 작업 필요 여부. 신규 케이스는 if 한 줄만 추가.
     */
    public boolean requiresFollowUp(TbWcsShuttleOrder parent) {
        if (ValueUtil.isEmpty(parent)) return false;
        if (!OrderType.OUTBOUND.matches(parent.getOrderType())) return false;

        SubOrderType sub = SubOrderType.fromOrNormal(parent.getSubOrderType());
        if (sub == SubOrderType.SAMPLE_OUT) {
            logger.debug("[ Order ][ Shuttle ] follow-up required - reason=sample_out, parentKey={}",
                    parent.getOrderKey());
            return true;
        }
        if (sub == SubOrderType.PARTIAL_OUT) {
            logger.debug("[ Order ][ Shuttle ] follow-up required - reason=partial_out, parentKey={}",
                    parent.getOrderKey());
            return true;
        }
        return false;
    }
}
