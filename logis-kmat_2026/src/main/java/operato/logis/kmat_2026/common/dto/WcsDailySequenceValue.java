package operato.logis.kmat_2026.common.dto;

/**
 * ====================================================================
 * WCS Daily Sequence Value
 * ====================================================================
 *
 * [역할]
 * - 날짜별 범용 시퀀스 발급 결과 DTO
 */
public class WcsDailySequenceValue {

    private final String seqType;
    private final String bizDate;
    private final long sequence;

    public WcsDailySequenceValue(String seqType, String bizDate, long sequence) {
        this.seqType = seqType;
        this.bizDate = bizDate;
        this.sequence = sequence;
    }

    public String getSeqType() {
        return seqType;
    }

    public String getBizDate() {
        return bizDate;
    }

    public long getSequence() {
        return sequence;
    }

    @Override
    public String toString() {
        return "WcsDailySequenceValue{" +
                "seqType='" + seqType + '\'' +
                ", bizDate='" + bizDate + '\'' +
                ", sequence=" + sequence +
                '}';
    }
}