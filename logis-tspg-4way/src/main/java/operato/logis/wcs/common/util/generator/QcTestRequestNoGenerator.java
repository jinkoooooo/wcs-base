package operato.logis.wcs.common.util.generator;

import operato.logis.wcs.common.dto.WcsDailySequenceValue;
import operato.logis.wcs.common.service.WcsDailySequenceService;
import org.springframework.stereotype.Component;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * QC 시험 의뢰번호 발급기.
 *
 * 포맷: QC-{YYYYMMDD}-{SEQ6}  (예: QC-20260526-000001)
 * - 입고일자 기준 시퀀스 증가, QC_REQ_NO 타입 단위 관리, 동시성 안전.
 * - (inbound_date, item_code, lot_no) 조합당 1건 발번은 호출부(createWithPdfId)의
 *   UNIQUE 중복 체크 + DB 제약으로 보장. 발번 자체는 호출 시마다 증가한다.
 *
 * 동시성: WcsDailySequenceService 의 UPSERT + RETURNING.
 * 시퀀스 점프는 운영상 허용 (트랜잭션 롤백 시 점프 가능).
 */
@Component
public class QcTestRequestNoGenerator {

    private static final String SEQ_TYPE_QC_REQ_NO = "QC_REQ_NO";
    private static final long DEFAULT_DOMAIN_ID = 7L;
    private static final DateTimeFormatter BIZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WcsDailySequenceService wcsDailySequenceService;

    public QcTestRequestNoGenerator(WcsDailySequenceService wcsDailySequenceService) {
        this.wcsDailySequenceService = wcsDailySequenceService;
    }

    public String generate(LocalDate inboundDate) {
        return generate(inboundDate, DEFAULT_DOMAIN_ID);
    }

    public String generate(LocalDate inboundDate, long domainId) {
        if (inboundDate == null) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "inboundDate 는 필수입니다.");
        }
        String bizDate = inboundDate.format(BIZ_DATE);
        // 날짜 리셋 없이 QC_REQ_NO 전역 단일 시퀀스로 증가
        WcsDailySequenceValue value = wcsDailySequenceService.next(SEQ_TYPE_QC_REQ_NO, domainId);
        return String.format("QC-%s-%06d", bizDate, value.getSequence());
    }
}