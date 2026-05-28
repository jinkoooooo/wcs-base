package operato.logis.wcs.common.util.generator;

import operato.logis.wcs.common.dto.WcsDailySequenceValue;
import operato.logis.wcs.common.service.WcsDailySequenceService;
import org.springframework.stereotype.Component;

/**
 * HOST 주문 키 발급기.
 *
 * 포맷: {YYYYMMDD}_{SEQ6}  (예: 20260514_000001)
 * - 날짜 기준 시퀀스 증가, SEQ_TYPE 단위 관리, 동시성 안전.
 */
@Component
public class HostOrderKeyGenerator {

    private static final long DEFAULT_DOMAIN_ID = 7L;

    private final WcsDailySequenceService wcsDailySequenceService;

    public HostOrderKeyGenerator(WcsDailySequenceService wcsDailySequenceService) {
        this.wcsDailySequenceService = wcsDailySequenceService;
    }

    public String generate(String seqType) {
        return generate(seqType, DEFAULT_DOMAIN_ID);
    }

    public String generate(String seqType, long domainId) {
        WcsDailySequenceValue value = wcsDailySequenceService.next(seqType, domainId);
        return String.format("%s_%06d", value.getBizDate(), value.getSequence());
    }
}
