package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.time.OffsetDateTime;

/**
 * WCS 운영 모드 엔티티.
 * 설비 그룹별 운영 모드와 배차 락·검수 활성 여부 등 운영 토글을 보관한다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_system_mode", idStrategy = GenerationRule.UUID)
public class TbWcsSystemMode extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "operation_mode", length = 20, nullable = false)
    private String operationMode;

    @Column(name = "eq_group_id", length = 64)
    private String eqGroupId;

    @Column(name = "changed_by", length = 50)
    private String changedBy;

    @Column(name = "changed_at")
    private OffsetDateTime changedAt;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "is_dispatch_lock_enabled")
    private Boolean isDispatchLockEnabled;

    @Column(name = "is_operation_mode_enabled")
    private Boolean isOperationModeEnabled;

    @Column(name = "is_inspection_enabled")
    private Boolean isInspectionEnabled;
}