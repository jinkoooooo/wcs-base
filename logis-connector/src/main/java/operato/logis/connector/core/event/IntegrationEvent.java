package operato.logis.connector.core.event;

import java.util.concurrent.CompletableFuture;

import xyz.elidom.sys.model.BaseResponse;

public class IntegrationEvent<T> {
	private long domainId;
	private SystemType system; // SAP, ECS, RCS 등
	private EventDirection direction; // SEND, RECEIVE
	private String endpoint; // 함수명 or URL
	private T payload;
	private CompletableFuture<BaseResponse> futureResponse = new CompletableFuture<>();
    
	public IntegrationEvent(long domainId, SystemType system, EventDirection direction, String endpoint, T payload) {
        this.domainId = domainId;
        this.system = system;
        this.direction = direction;
        this.endpoint = endpoint;
        this.payload = payload;
    }

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
	}

	public SystemType getSystem() {
		return system;
	}

	public void setSystem(SystemType system) {
		this.system = system;
	}

	public EventDirection getDirection() {
		return direction;
	}

	public void setDirection(EventDirection direction) {
		this.direction = direction;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public T getPayload() {
		return payload;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	public CompletableFuture<BaseResponse> getFutureResponse() {
		return futureResponse;
	}

	public void setFutureResponse(CompletableFuture<BaseResponse> futureResponse) {
		this.futureResponse = futureResponse;
	}
}