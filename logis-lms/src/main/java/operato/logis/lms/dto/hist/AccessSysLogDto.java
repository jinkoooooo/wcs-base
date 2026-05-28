package operato.logis.lms.dto.hist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.LmsUserActivityLog;

@Getter
@Setter
@AllArgsConstructor
public class AccessSysLogDto {

    private LmsUserActivityLog log;
    private String userId;
    private String sessionId;
    private String requestUri;
    private String actType;
    private String requestJson;
    private String responseJson;
    private Integer responseStatus;
    private Integer serverPort;
    private Long duration;
    private String accessIp;
    private String userAgent;
}