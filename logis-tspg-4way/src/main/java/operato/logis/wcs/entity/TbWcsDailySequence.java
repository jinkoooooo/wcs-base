package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * 날짜별 범용 시퀀스 관리 엔티티.
 * seq_type + biz_date 기준으로 1부터 증가한다.
 *
 * 예: ORDER_KEY / 20260313 / 154, BATCH_KEY / 20260313 / 7, DOC_NO / 20260313 / 31.
 */
@Getter
@Setter
@Table(name = "tb_wcs_daily_sequence", idStrategy = GenerationRule.UUID,
        uniqueFields = "seqType,bizDate",
        indexes = {
                // ON CONFLICT (seq_type, biz_date) 쿼리 동작을 보장하는 unique 인덱스
                @Index(name = "ux_tb_wcs_daily_seq_key", columnList = "seq_type,biz_date", unique = true)
        }
)
public class TbWcsDailySequence extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    /** 시퀀스 유형 (예: ORDER_KEY, BATCH_KEY, DOC_NO, BOX_SEQ:{itemCode}:{lotNo}) */
    @Column(name = "seq_type", length = 100)
    private String seqType;

    /** 업무 일자 (YYYYMMDD) */
    @Column(name = "biz_date", length = 8)
    private String bizDate;

    /** 마지막 발급 시퀀스 값 */
    @Column(name = "last_seq")
    private Long lastSeq;

    /** 설명 */
    @Column(name = "description", length = 200)
    private String description;
}