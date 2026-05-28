package operato.logis.lms.entity.center;

import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;

import java.io.Serial;

@Table(name = "lms_center_users", idStrategy = GenerationRule.UUID, notnullFields = "userId,lcId", uniqueFields = "lcId,userId",
        indexes = { @Index(name = "uq_user_lc", columnList = "user_id,lc_id") })
public class LmsCenterUsers extends DomainStampHook {

    @Serial
    private static final long serialVersionUID = 2542331447162495131L;

    @PrimaryKey
    @Column(name = "id", nullable = false, generator = GenerationRule.UUID, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 36)
    private String lcId;

    @Column(name = "user_id", nullable = false, length = 25)
    private String userId;

    public String getId() { return id; }

    public String getLcId() { return lcId; }

    public String getUserId() { return userId; }

    public void setLcId(String lcId) { this.lcId = lcId; }

    public void setUserId(String userId) { this.userId = userId; }
}