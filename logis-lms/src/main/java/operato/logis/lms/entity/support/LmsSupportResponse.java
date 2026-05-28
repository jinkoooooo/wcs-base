package operato.logis.lms.entity.support;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.DateUtil;
import xyz.elidom.util.ValueUtil;

import java.io.Serial;

@Table(name = "lms_support_response", idStrategy = GenerationRule.UUID, notnullFields = "lcId,resId,supportId,content,creatorNmCt")
public class LmsSupportResponse extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @Serial
    private static final long serialVersionUID = 3804235693601052358L;

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_NAME, generator = GenerationRule.UUID)
    private String id;

    @Column(name = "res_id", length = 64, nullable = false)
    private String resId;

    @Column(name = "support_id", length = 64, nullable = false)
    private String supportId;

    @Column(name = "lc_id", length = 20, nullable = false)
    private String lcId;

    @Column(name = "content", length = OrmConstants.FIELD_SIZE_VALUE_2000, nullable = false)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "creator_nm", length = 64)
    private String creatorNm;

    @Column(name = "creator_nm_ct", nullable = false)
    private byte[] creatorNmCt;

    @Column(name = "creator_nm_iv")
    private byte[] creatorNmIv;

    @Column(name = "creator_nm_key_id")
    private Short creatorNmKeyId;

    public String getId() { return id; }

    public String getResId() { return resId; }

    public String getSupportId() { return supportId; }

    public String getLcId() { return lcId; }

    public String getContent() { return content; }

    public Boolean getIsDeleted() { return isDeleted; }

    public String getCreatorNm() { return creatorNm; }

    public byte[] getCreatorNmCt() { return creatorNmCt; }

    public byte[] getCreatorNmIv() { return creatorNmIv; }

    public Short getCreatorNmKeyId() { return creatorNmKeyId; }

    public void setResId(String resId) { this.resId = resId; }

    public void setSupportId(String supportId) { this.supportId = supportId; }

    public void setLcId(String lcId) { this.lcId = lcId; }

    public void setContent(String content) { this.content = content; }

    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public void setCreatorNm(String creatorNm) { this.creatorNm = creatorNm; }

    public void setId(String id) { this.id = id; }

    public void setCreatorNmCt(byte[] creatorNmCt) { this.creatorNmCt = creatorNmCt; }

    public void setCreatorNmIv(byte[] creatorNmIv) { this.creatorNmIv = creatorNmIv; }

    public void setCreatorNmKeyId(Short creatorNmKeyId) { this.creatorNmKeyId = creatorNmKeyId; }

    @Override
    public void beforeCreate() {
        // res_id 자동 생성
        if (this.resId == null && this.lcId != null) {
            String upperLcId = ValueUtil.toUpperCaseHead(this.lcId);
            String ts = DateUtil.dateTimeStr(xyz.elidom.sys.util.DateUtil.getDate(), "yyyyMMddHHmmss");
            String rand = String.format("%03d", java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 10000));
            this.setResId(upperLcId + "-" + ts + "-" + rand);
        }

        super.beforeCreate();
    }
}