package operato.logis.kmat_2026.common.util;

import operato.logis.kmat_2026.common.dto.WcsDailySequenceValue;
import operato.logis.kmat_2026.common.service.WcsDailySequenceService;
import org.springframework.stereotype.Component;

/**
 * ====================================================================
 * Order Key Generator
 * ====================================================================
 *
 * [포맷]
 * {YYYYMMDD}-{SEQ6}
 *
 * 예)
 * 20260313-000001
 * 20260313-000002
 * 20260313-000003
 *
 * [정책]
 * - 날짜 기준 시퀀스 증가
 * - ORDER_KEY 타입 기준으로 시퀀스 관리
 * - 동시성 안전
 */
@Component
public class OrderKeyGenerator {

    private static final String SEQ_TYPE_ORDER_KEY = "ORDER_KEY";
    private static final long DEFAULT_DOMAIN_ID = 7L;

    private final WcsDailySequenceService wcsDailySequenceService;

    public OrderKeyGenerator(WcsDailySequenceService wcsDailySequenceService) {
        this.wcsDailySequenceService = wcsDailySequenceService;
    }

    /**
     * 기본 Order Key 생성
     */
    public String generate(String seqType) {
        return generate(seqType,DEFAULT_DOMAIN_ID);
    }

    /**
     * Domain 지정 Order Key 생성
     */
    public String generate(String seqType, long domainId) {

        WcsDailySequenceValue value =
                wcsDailySequenceService.next(seqType, domainId);

        return String.format(
                "%s_%06d",
                value.getBizDate(),
                value.getSequence()
        );
    }
}