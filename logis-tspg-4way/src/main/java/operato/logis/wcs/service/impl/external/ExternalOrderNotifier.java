package operato.logis.wcs.service.impl.external;

import operato.logis.wcs.entity.TbWcsHostOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

/**
 * 외부 시스템(WMS/HOST)으로 host_order 이벤트를 통지하는 stub.
 *
 * 실제 호출은 미구현 — 향후 외부 연동 시 실제 클라이언트로 교체.
 */
@Service
public class ExternalOrderNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ExternalOrderNotifier.class);

    /**
     * host_order 신규 생성 통지.
     */
    public void notifyOrderCreated(TbWcsHostOrder o) {
        if (ValueUtil.isEmpty(o)) return;
        logger.info("[ External ][ Notify ] orderCreated - hostOrderKey={}, type={}, eqGroupId={}",
                o.getHostOrderKey(), o.getOrderType(), o.getEqGroupId());
    }

    /**
     * host_order 상태 전이 통지.
     */
    public void notifyOrderStatusChanged(TbWcsHostOrder o, String previousStatus) {
        if (ValueUtil.isEmpty(o)) return;
        logger.info("[ External ][ Notify ] statusChanged - hostOrderKey={}, {} -> {}",
                o.getHostOrderKey(), previousStatus, o.getOrderStatus());
    }
}
