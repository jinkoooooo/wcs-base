package operato.logis.samsung.entity.buffer;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainUpdateStampHook;

/**
 * 설비관리 - 시퀀스버퍼 동작모드 관리
 */
@Getter
@Setter
@Table(name = "tb_mw_buffer_status", idStrategy = GenerationRule.UUID)
public class TbMwBufferStatus extends DomainUpdateStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "operation_type", nullable = false, length = 1)
    private String operationType; // 작업모드
}