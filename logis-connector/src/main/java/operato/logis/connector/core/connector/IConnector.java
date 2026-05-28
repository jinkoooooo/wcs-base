package operato.logis.connector.core.connector;

import operato.logis.connector.core.event.EventDirection;
import operato.logis.connector.core.event.IntegrationEvent;
import reactor.core.publisher.Mono;
import xyz.elidom.sys.model.BaseResponse;

public interface IConnector<T> {
	boolean supports(String system, EventDirection dir);
	Mono<BaseResponse> handle(IntegrationEvent<T> event);
}