package operato.logis.posco.service;

import jakarta.servlet.http.HttpServletRequest;
import operato.logis.connector.api.util.ApiUtil;
import operato.logis.posco.entity.TbMcsApiRequestLog;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Service
public class TbMcsApiRequestLogService extends AbstractQueryService {

    public void create(HttpServletRequest request, String deviceId, String code, String message) {
        String ip = ApiUtil.getClientIp(request);
        String body = ApiUtil.getRequestBody(request);
        String uri = request.getRequestURI();

        TbMcsApiRequestLog log = new TbMcsApiRequestLog();
        log.setDeviceId(deviceId);
        log.setIp(ip);
        log.setUrl(uri);
        log.setParam(body);
        log.setCode(code);
        log.setMessage(message);
        this.queryManager.insert(log);
    }
}