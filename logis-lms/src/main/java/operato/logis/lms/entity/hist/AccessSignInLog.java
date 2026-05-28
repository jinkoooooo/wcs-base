package operato.logis.lms.entity.hist;


import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;

import java.io.Serial;
import java.time.LocalDate;

@Table(name = "lms_access_sign_in_log_p", idStrategy = GenerationRule.NONE, notnullFields = "userId,accessIp,signInResult,httpStatusCd")
public class AccessSignInLog extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @Serial
    private static final long serialVersionUID = -2835757033466279564L;

    // log_id와 복합키
    @PrimaryKey
    @Column(name = "log_date", nullable = false, type = ColumnType.DATETIME)
    private LocalDate logDate;

    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "access_ip", nullable = false, length = 32)
    private String accessIp;

    @Column(name = "user_agent", length = OrmConstants.FIELD_SIZE_VALUE_1000)
    private String userAgent;

    @Column(name = "device_info", length = OrmConstants.FIELD_SIZE_VALUE_255)
    private String deviceInfo;

    @Column(name = "sign_in_result", nullable = false, length = OrmConstants.FIELD_SIZE_STATUS)
    private Integer signInResult;

    @Column(name = "http_status_cd", nullable = false, length = OrmConstants.FIELD_SIZE_STATUS)
    private Integer httpStatusCd;

    @Column(name = "fail_reason", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
    private String failReason;

    @Column(name = "sign_out_type", length = OrmConstants.FIELD_SIZE_STATUS)
    private String signOutType;

    public LocalDate getLogDate() { return logDate; }

    public String getUserId() { return userId; }

    public String getSessionId() { return sessionId; }

    public String getAccessIp() { return accessIp; }

    public String getUserAgent() { return userAgent; }

    public String getDeviceInfo() { return deviceInfo; }

    public Integer getSignInResult() { return signInResult; }

    public Integer getHttpStatusCd() { return httpStatusCd; }

    public String getFailReason() { return failReason; }

    public String getSignOutType() { return signOutType; }

    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public void setAccessIp(String accessIp) { this.accessIp = accessIp; }

    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public void setSignInResult(Integer signInResult) { this.signInResult = signInResult; }

    public void setHttpStatusCd(Integer httpStatusCd) { this.httpStatusCd = httpStatusCd; }

    public void setFailReason(String failReason) { this.failReason = failReason; }

    public void setSignOutType(String signOutType) { this.signOutType = signOutType; }
}


