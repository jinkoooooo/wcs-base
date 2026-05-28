package operato.logis.kmat_2026.biz.ecs.sineva.event;

import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.event.SysEvent;

import java.util.Map;

public class CallbackEvent extends SysEvent {
    private final Map<String, Object> requestData;
    private final Domain domain;

    public CallbackEvent(Map<String, Object> requestData, Domain domain) {
        this.requestData = requestData;
        this.domain = domain;
    }

    public Map<String, Object> getRequestData() {
        return requestData;
    }

    public Domain getDomain() {
        return domain;
    }
}
