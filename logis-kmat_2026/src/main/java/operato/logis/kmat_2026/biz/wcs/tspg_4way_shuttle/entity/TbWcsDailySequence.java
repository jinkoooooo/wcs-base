package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * ====================================================================
 * WCS Daily Sequence Entity
 * ====================================================================
 *
 * [역할]
 * - 날짜별 범용 시퀀스 관리 테이블
 * - seq_type + biz_date 기준으로 1부터 증가
 *
 * [예시]
 * - ORDER_KEY / 20260313 / 154
 * - BATCH_KEY / 20260313 / 7
 * - DOC_NO    / 20260313 / 31
 */
@Getter
@Setter
@Table(name = "tb_wcs_daily_sequence", idStrategy = GenerationRule.UUID,
        uniqueFields = "seqType,bizDate",
        indexes = {
                // [Unique Index] ON CONFLICT (seq_type, biz_date) 쿼리가 정상 동작하게 만드는 핵심 인덱스
                @Index(name = "ux_tb_wcs_daily_seq_key", columnList = "seq_type,biz_date", unique = true)
        }
)
public class TbWcsDailySequence extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    /** 시퀀스 유형 (예: ORDER_KEY, BATCH_KEY, DOC_NO) */
    @Column(name = "seq_type", length = 50)
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