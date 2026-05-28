package operato.logis.wcs.service.impl.qctest;

import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.impl.order.intake.SpecialShuttleOrderFactory;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import operato.logis.wcs.service.impl.order.issuer.ReinboundIssuer;

/**
 * 시험 폐기 발급 — 시험 부적합 판정 시.
 *
 * 시험용 출고 완료 후 부적합으로 판정된 파렛트를 폐기 처리한다.
 * 실제 셔틀 이동이 없는 마커 성격의 OUTBOUND 셔틀 (autoFinalize + COMPLETED) 을 직접 insert —
 * OrderIntakeService 의 산출/디스패치 흐름은 거치지 않는다.
 *
 * 호스트 주문 상태는 HostOrderStatus.TEST_FAILED 로 전이.
 */
@Service
@RequiredArgsConstructor
public class SampleDiscardIssuer {

    private static final Logger logger = LoggerFactory.getLogger(SampleDiscardIssuer.class);

    private final HostOrderStateWriter hostOrderStateWriter;
    private final OrderLookupUtils orderLookup;
    private final SpecialShuttleOrderFactory specialShuttleFactory;
    private final ReinboundIssuer reinboundIssuer;

    /**
     * 시험 폐기 발급 — pendingSampleOut 의 부모 호스트 오더를 TEST_FAILED 로 전이하고
     * 마커 성격의 SAMPLE_DISCARD 셔틀을 즉시 발급한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsShuttleOrder issueSampleDiscard(String palletBarcode, String reason) {
        // 입력 검증
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 입력되지 않았습니다.");

        // 부모 sampleOut 찾기 (없으면 폐기 불가)
        TbWcsShuttleOrder pending = reinboundIssuer.findPendingSampleOut(palletBarcode);
        if (ValueUtil.isEmpty(pending)) {
            throw new ElidomRuntimeException("NO_PENDING_SAMPLE_OUT",
                    "폐기 대상 시험용 출고가 없습니다.");
        }

        // 호스트 + 폐기 셔틀 발급
        TbWcsHostOrder h = orderLookup.getHostOrderOrThrow(pending.getHostOrderKey());
        TbWcsShuttleOrder discard = specialShuttleFactory.createSampleDiscardShuttle(
                h, palletBarcode, pending, reason);

        // 호스트 상태 TEST_FAILED 전이
        hostOrderStateWriter.markTestFailed(h);

        logger.info("[ Qctest ][ Discard ] sample discard issued - pallet={}, parent={}, discard={}, host={}, reason={}",
                palletBarcode, pending.getOrderKey(), discard.getOrderKey(), h.getHostOrderKey(), reason);
        return discard;
    }
}
