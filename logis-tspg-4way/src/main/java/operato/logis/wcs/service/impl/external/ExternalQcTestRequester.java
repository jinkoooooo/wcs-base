package operato.logis.wcs.service.impl.external;

import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

/**
 * 외부 품질검사 시스템(NIA)으로 QC 테스트 요청을 발송하는 stub.
 *
 * 실제 호출은 미구현 — 향후 외부 연동 시 실제 클라이언트로 교체.
 */
@Service
public class ExternalQcTestRequester {

    private static final Logger logger = LoggerFactory.getLogger(ExternalQcTestRequester.class);

    /**
     * 외부 NIA 로 품질검사 요청 발송.
     */
    public void requestQcTest(TbWcsHostOrder order, TbWcsHostOrderItem item) {
        if (ValueUtil.isEmpty(order) || ValueUtil.isEmpty(item)) return;
        logger.info("[ External ][ QcTest ] requestQcTest - hostOrderKey={}, eqGroupId={}, barcode={}, sku={}, lot={}, testRequestNo={}",
                order.getHostOrderKey(), order.getEqGroupId(), order.getBarcode(),
                item.getItemCode(), item.getLotNo(), item.getTestRequestNo());
    }
}
