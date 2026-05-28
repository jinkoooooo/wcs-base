package operato.logis.lms.entity.hist;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;

import java.io.Serial;
import java.time.LocalDate;

@Table(name = "lms_access_session_log_p", idStrategy = GenerationRule.NONE, notnullFields = "sessionId,userId,accessIp,sessionStatus")
public class AccessSessionLog extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @Serial
    private static final long serialVersionUID = -5481196870555850521L;

    // log_id와 복합키
    @PrimaryKey
    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    @Column(name = "access_ip", nullable = false, length = 32)
    private String accessIp;

    @Column(name = "user_agent", length = OrmConstants.FIELD_SIZE_VALUE_1000)
    private String userAgent;

    @Column(name = "device_info", length = OrmConstants.FIELD_SIZE_VALUE_255)
    private String deviceInfo;

    @Column(name = "session_status", nullable = false, length = 1)
    private Integer sessionStatus;

    @Column(name = "sign_out_type", length = OrmConstants.FIELD_SIZE_STATUS)
    private String signOutType;

    public LocalDate getLogDate() { return logDate; }

    public String getSessionId() { return sessionId; }

    public String getUserId() { return userId; }

    public String getAccessIp() { return accessIp; }

    public String getUserAgent() { return userAgent; }

    public String getDeviceInfo() { return deviceInfo; }

    public Integer getSessionStatus() { return sessionStatus; }

    public String getSignOutType() { return signOutType; }

    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setAccessIp(String accessIp) { this.accessIp = accessIp; }

    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public void setSessionStatus(Integer sessionStatus) { this.sessionStatus = sessionStatus; }

    public void setSignOutType(String signOutType) { this.signOutType = signOutType; }
}
