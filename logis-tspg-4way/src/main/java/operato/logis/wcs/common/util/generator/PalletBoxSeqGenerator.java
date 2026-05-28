package operato.logis.wcs.common.util.generator;

import operato.logis.wcs.common.dto.WcsDailySequenceValue;
import operato.logis.wcs.common.service.WcsDailySequenceService;
import org.springframework.stereotype.Component;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Pallet Box Seq Generator.
 *
 * (item_code, lot_no, 입고일자) 그룹 단위 박스 일련번호 발급.
 * 같은 그룹이면 파렛트 경계를 넘어 1,2,3... 으로 이어진다.
 *
 * 내부 인코딩:
 *   - seq_type = "BOX_SEQ:{itemCode}:{lotNo}"
 *   - biz_date = inboundDate.format("yyyyMMdd")
 *
 * 동시성: WcsDailySequenceService 의 UPSERT + RETURNING.
 * 시퀀스 점프는 운영상 허용 (트랜잭션 롤백 시 점프 가능).
 */
@Component
public class PalletBoxSeqGenerator {

    private static final String SEQ_TYPE_PREFIX = "BOX_SEQ:";
    private static final long DEFAULT_DOMAIN_ID = 7L;
    private static final DateTimeFormatter BIZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WcsDailySequenceService wcsDailySequenceService;

    public PalletBoxSeqGenerator(WcsDailySequenceService wcsDailySequenceService) {
        this.wcsDailySequenceService = wcsDailySequenceService;
    }

    public int next(String itemCode, String lotNo, LocalDate inboundDate) {
        return next(itemCode, lotNo, inboundDate, DEFAULT_DOMAIN_ID);
    }

    public int next(String itemCode, String lotNo, LocalDate inboundDate, long domainId) {
        if (ValueUtil.isEmpty(itemCode)) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "itemCode 는 필수입니다.");
        }
        if (inboundDate == null) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "inboundDate 는 필수입니다.");
        }
        String seqType = SEQ_TYPE_PREFIX + itemCode + ":" + (lotNo == null ? "" : lotNo);
        String bizDate = inboundDate.format(BIZ_DATE);
        WcsDailySequenceValue value = wcsDailySequenceService.next(seqType, bizDate, domainId);
        return (int) value.getSequence();
    }
}
