package operato.logis.lms.entity.hist;


import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;

import java.io.Serial;
import java.time.LocalDate;

@Table(name = "lms_access_sys_log_p", idStrategy = GenerationRule.COMPLEX_KEY, notnullFields = "logContent,userId,sessionId,reqUri,actType,resStatus,serverIp,serverPort")
public class AccessSysLog extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @Serial
    private static final long serialVersionUID = 6983448688220920040L;

    // log_id와 복합키
    @PrimaryKey
    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "id", nullable = true, length = 40)
    private String id;

    // TODO: json type으로 변경
    @Column(name = "log_content", nullable = false)
    private String logContent;

    @Column(name = "act_desc", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
    private String actDesc;

    @Column(name = "req_uri", nullable = false, length = OrmConstants.FIELD_SIZE_MAX_TEXT)
    private String reqUri;

    @Column(name = "act_type", nullable = false, length = OrmConstants.FIELD_SIZE_STATUS)
    private String actType;

    @Column(name = "req_detail", length = OrmConstants.FIELD_SIZE_MAX_TEXT)
    private String reqDetail;

    @Column(name = "res_detail", length = OrmConstants.FIELD_SIZE_MAX_TEXT)
    private String resDetail;

    @Column(name = "res_status")
    private Integer resStatus;

    @Column(name = "res_time")
    private Long resTime;

    @Column(name = "server_ip", nullable = false, length = 32)
    private String serverIp;

    @Column(name = "server_port", nullable = false)
    private Integer serverPort;

    @Column(name = "user_id", length = 32)
    private String userId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "access_ip", length = 32) // todo: 기존 데이터 보완 후 nullable = false 로 변경
    private String accessIp;

    @Column(name = "user_agent", length = OrmConstants.FIELD_SIZE_VALUE_1000)
    private String userAgent;

    public LocalDate getLogDate() { return logDate; }

    public String getLogContent() { return logContent; }

    public String getActDesc() { return actDesc; }

    public String getReqUri() { return reqUri; }

    public String getActType() { return actType; }

    public String getReqDetail() { return reqDetail; }

    public String getResDetail() { return resDetail; }

    public Integer getResStatus() { return resStatus; }

    public Long getResTime() { return resTime; }

    public String getServerIp() { return serverIp; }

    public Integer getServerPort() { return serverPort; }

    public String getUserId() { return userId; }

    public String getSessionId() { return sessionId; }

    public String getAccessIp() { return accessIp; }

    public String getUserAgent() { return userAgent; }

    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public void setLogContent(String logContent) { this.logContent = logContent; }

    public void setActDesc(String actDesc) { this.actDesc = actDesc; }

    public void setReqUri(String reqUri) { this.reqUri = reqUri; }

    public void setActType(String actType) { this.actType = actType; }

    public void setReqDetail(String reqDetail) { this.reqDetail = reqDetail; }

    public void setResDetail(String resDetail) { this.resDetail = resDetail; }

    public void setResStatus(Integer resStatus) { this.resStatus = resStatus; }

    public void setResTime(Long resTime) { this.resTime = resTime; }

    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public void setAccessIp(String accessIp) { this.accessIp = accessIp; }

    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}


