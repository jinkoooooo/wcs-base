package operato.logis.wcs.common.service;

import operato.logis.wcs.common.dto.WcsDailySequenceValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * tb_wcs_daily_sequence 기반 날짜별/유형별 증가 시퀀스 발급.
 *
 * PostgreSQL UPSERT + RETURNING 으로 동시성 안전하게 발급하며 날짜별 리셋된다.
 * order key, batch key, document no 등에 공통 사용. 유니크 기준은 seq_type + biz_date.
 */
@Service
public class WcsDailySequenceService {

    private static final DateTimeFormatter BIZ_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbcTemplate;

    public WcsDailySequenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 오늘 날짜 기준 다음 시퀀스 발급.
     */
    public WcsDailySequenceValue next(String seqType, long domainId) {
        String bizDate = LocalDate.now().format(BIZ_DATE_FORMATTER);
        long sequence = nextSequence(seqType, bizDate, domainId);
        return new WcsDailySequenceValue(normalizeSeqType(seqType), bizDate, sequence);
    }

    /**
     * 특정 날짜 기준 다음 시퀀스 발급.
     */
    public WcsDailySequenceValue next(String seqType, String bizDate, long domainId) {
        long sequence = nextSequence(seqType, bizDate, domainId);
        return new WcsDailySequenceValue(normalizeSeqType(seqType), bizDate, sequence);
    }

    /**
     * UPSERT 로 시퀀스를 1 증가시키고 갱신된 last_seq 를 반환한다.
     */
    private long nextSequence(String seqType, String bizDate, long domainId) {
        String normalizedSeqType = normalizeSeqType(seqType);

        // bizDate 필수
        if (!StringUtils.hasText(bizDate)) {
            throw new IllegalArgumentException("bizDate is required");
        }

        // 신규 insert, 충돌 시 last_seq 증가 후 RETURNING
        String sql = """
                insert into tb_wcs_daily_sequence
                    (id, seq_type, biz_date, last_seq, domain_id, created_at, updated_at)
                values
                    (replace(gen_random_uuid()::text, '-', ''), ?, ?, 1, ?, now(), now())
                on conflict (seq_type, biz_date)
                do update set
                    last_seq = tb_wcs_daily_sequence.last_seq + 1,
                    updated_at = now()
                returning last_seq
                """;

        Map<String, Object> row = jdbcTemplate.queryForMap(sql, normalizedSeqType, bizDate, domainId);
        return ((Number) row.get("last_seq")).longValue();
    }

    /**
     * seqType 필수 검증 후 trim + 대문자 정규화.
     */
    private String normalizeSeqType(String seqType) {
        if (!StringUtils.hasText(seqType)) {
            throw new IllegalArgumentException("seqType is required");
        }
        return seqType.trim().toUpperCase();
    }
}