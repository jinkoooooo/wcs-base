package operato.logis.wcs.common.util.generator;

import operato.logis.wcs.common.dto.WcsDailySequenceValue;
import operato.logis.wcs.common.service.WcsDailySequenceService;
import org.springframework.stereotype.Component;

/**
 * Pallet Barcode Generator.
 *
 * 포맷: {YYYYMMDD}_{SEQ6}  (예: 20260513_000001)
 * - 날짜 기준 시퀀스 증가, PALLET_BARCODE 타입으로 ORDER_KEY 와 분리.
 * - 동시성 안전. 발번 규칙 변경 시 이 클래스만 수정.
 */
@Component
public class PalletBarcodeGenerator {

    private static final String SEQ_TYPE_PALLET_BARCODE = "PALLET_BARCODE";
    private static final long DEFAULT_DOMAIN_ID = 7L;

    private final WcsDailySequenceService wcsDailySequenceService;

    public PalletBarcodeGenerator(WcsDailySequenceService wcsDailySequenceService) {
        this.wcsDailySequenceService = wcsDailySequenceService;
    }

    public String generate() {
        return generate(DEFAULT_DOMAIN_ID);
    }

    public String generate(long domainId) {
        WcsDailySequenceValue value =
                wcsDailySequenceService.next(SEQ_TYPE_PALLET_BARCODE, domainId);

        return String.format("%s_%06d", value.getBizDate(), value.getSequence());
    }
}
