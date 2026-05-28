package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * 엔티티 INSERT/UPDATE 변경 감사 로그.
 * 변경 컬럼·전후 JSON·호출자·사유를 기록한다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_audit_log", idStrategy = GenerationRule.UUID)
public class TbWcsAuditLog extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 64)
    private String id;

    /** INSERT | UPDATE */
    @Column(name = "action", length = 10, nullable = false)
    private String action;

    @Column(name = "entity_class", length = 128)
    private String entityClass;

    @Column(name = "table_name", length = 64)
    private String tableName;

    @Column(name = "pk_value", length = 255)
    private String pkValue;

    /** 행위자 유형 USER | HOST | ECS | SCHEDULER | SYSTEM */
    @Column(name = "actor_type", length = 20)
    private String actorType;

    /** 행위자 식별자. 사람=로그인 id, 기계=채널 식별자 */
    @Column(name = "actor_id", length = 64)
    private String actorId;

    /** 행위자 표시명 */
    @Column(name = "actor_name", length = 128)
    private String actorName;

    /** 진입 채널 HTTP_UI | ECS_CALLBACK | SCHEDULER */
    @Column(name = "channel", length = 40)
    private String channel;

    @Column(name = "changed_columns", length = 1000)
    private String changedColumns;

    @Column(name = "before_json", length = 4000)
    private String beforeJson;

    @Column(name = "after_json", length = 4000)
    private String afterJson;

    @Column(name = "caller", length = 255)
    private String caller;

    @Column(name = "reason", length = 500)
    private String reason;
}
