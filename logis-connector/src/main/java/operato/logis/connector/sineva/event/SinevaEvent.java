package operato.logis.connector.sineva.event;


import xyz.elidom.sys.event.SysEvent;

import java.util.Map;

/**
 * Sineva 수신 이벤트 객체.
 *
 * callback 수신 시 필요한 최소 정보만 담아서 내부 이벤트로 전달한다.
 */
public class SinevaEvent extends SysEvent {

    /** 인터페이스명 (예: callback) */
    private String interfaceName;

    /** 도메인 ID */
    private Long domainId;

    /** 수신 payload */
    private Map<String, Object> ifData;

    public SinevaEvent(String interfaceName, Long domainId, Map<String, Object> ifData) {
        this.interfaceName = interfaceName;
        this.domainId = domainId;
        this.ifData = ifData;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    @Override
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Map<String, Object> getIfData() {
        return ifData;
    }

    public void setIfData(Map<String, Object> ifData) {
        this.ifData = ifData;
    }
}