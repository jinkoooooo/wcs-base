package operato.logis.lms.entity.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import operato.logis.lms.consts.SupportCategory;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.util.DateUtil;
import xyz.elidom.util.ValueUtil;

import java.io.Serial;
import java.util.Date;

@AllArgsConstructor
@Builder
@Table(name = "lms_support_request", idStrategy = GenerationRule.UUID, notnullFields = "supportId,lcId,category,title,content,status,requesterId",
        indexes = { @Index(name = "lms_support_request_lc_id_index", columnList = "lc_id"),
                @Index(name = "lms_support_request_lc_id_status_index", columnList = "lc_id,status") })
public class LmsSupportRequest extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @Serial
    private static final long serialVersionUID = -4584571434322654014L;

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_NAME, generator = GenerationRule.UUID)
    private String id;

    @Column(name = "support_id", length = 64, nullable = false)
    private String supportId;

    @Column(name = "lc_id", length = 20, nullable = false)
    private String lcId;

    @Column(name = "equip_id", length = 50)
    private String equipId;

    @Column(name = "alarm_id", length = 30)
    private String alarmId;

    @Column(name = "category", length = OrmConstants.FIELD_SIZE_CATEGORY, nullable = false)
    private String category;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    @Column(name = "status", length = OrmConstants.FIELD_SIZE_CATEGORY, nullable = false)
    private String status;

    @Column(name = "requester_id", length = 32, nullable = false)
    private String requesterId;

    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    @Column(name = "assignee_ct")
    private byte[] assigneeCt;

    @Column(name = "assignee_iv")
    private byte[] assigneeIv;

    @Column(name = "assignee_key_id")
    private Short assigneeKeyId;

    @Column(name = "receiver_id", length = 32, nullable = false)
    private String receiverId;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "received_at", type = ColumnType.DATETIME)
    private Date receivedAt;

    @Column(name = "completed_at", type = ColumnType.DATETIME)
    private Date completedAt;

    public LmsSupportRequest() {}

    public String getId() { return id; }

    public String getSupportId() { return supportId; }

    public String getLcId() { return lcId; }

    public String getEquipId() { return equipId; }

    public String getAlarmId() { return alarmId; }

    public String getCategory() { return category; }

    public String getTitle() { return title; }

    public String getContent() { return content; }

    public String getStatus() { return status; }

    public String getRequesterId() { return requesterId; }

    public String getAssigneeId() { return assigneeId; }

    public byte[] getAssigneeCt() { return assigneeCt; }

    public byte[] getAssigneeIv() { return assigneeIv; }

    public Short getAssigneeKeyId() { return assigneeKeyId; }

    public String getReceiverId() { return receiverId; }

    public Boolean getDeleted() { return isDeleted; }

    public Date getReceivedAt() { return receivedAt; }

    public Date getCompletedAt() { return completedAt; }

    public void setId(String id) { this.id = id; }

    public void setSupportId(String supportId) { this.supportId = supportId; }

    public void setLcId(String lcId) { this.lcId = lcId; }

    public void setEquipId(String equipId) { this.equipId = equipId; }

    public void setAlarmId(String alarmId) { this.alarmId = alarmId; }

    public void setCategory(String category) { this.category = category; }

    public void setTitle(String title) { this.title = title; }

    public void setContent(String content) { this.content = content; }

    public void setStatus(String status) { this.status = status; }

    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public void setAssigneeCt(byte[] assigneeCt) { this.assigneeCt = assigneeCt; }

    public void setAssigneeIv(byte[] assigneeIv) { this.assigneeIv = assigneeIv; }

    public void setAssigneeKeyId(Short assigneeKeyId) { this.assigneeKeyId = assigneeKeyId; }

    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public void setDeleted(Boolean deleted) { isDeleted = deleted; }

    public void setReceivedAt(Date receivedAt) { this.receivedAt = receivedAt; }

    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    @Override
    public void beforeCreate() {
        this.setDefault();
        super.beforeCreate();
    }

    private void setDefault() {
        // 요청자 id 생성
        if (this.getRequesterId() == null || this.requesterId.isBlank()) {
            User user = User.currentUser();
            if (user != null) this.requesterId = user.getId();
        }

        // support_id 생성
        if ((this.supportId == null || this.supportId.isBlank()) && this.lcId != null && !this.lcId.isBlank()) {
            String upperLcId = ValueUtil.toUpperCaseHead(this.lcId);
            String ts = DateUtil.dateTimeStr(xyz.elidom.sys.util.DateUtil.getDate(), "yyyyMMddHHss");
            String rand = String.format("%03d", java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 1000));
            this.supportId = String.join("-", upperLcId, ts, rand);
        }

        // 카테고리 기본값 지정
        if (this.category == null || this.category.isBlank()) {
            this.category = SupportCategory.OTHERS.getValue();
        }

        // 삭제여부 기본값 지정
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }
}