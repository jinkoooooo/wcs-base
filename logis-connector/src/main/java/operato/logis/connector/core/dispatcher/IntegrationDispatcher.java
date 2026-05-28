package operato.logis.connector.core.dispatcher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import operato.logis.connector.core.connector.IConnector;
import operato.logis.connector.core.event.IntegrationEvent;
import reactor.core.scheduler.Schedulers;
import xyz.elidom.sys.model.BaseResponse;

@Component
public class IntegrationDispatcher {

	private final List<IConnector<?>> connectors;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public IntegrationDispatcher(List<IConnector<?>> connectors) {
		this.connectors = connectors;
	}

	/** 모든 IntegrationEvent<T> 수신 */
	@SuppressWarnings("unchecked")
	@EventListener
	public void dispatch(IntegrationEvent<?> event) {
		connectors.stream()
			.filter(connector -> connector.supports(event.getSystem().name(), event.getDirection()))
			.findFirst()
			.ifPresentOrElse(conn -> {
				IConnector<Object> connectorCasted = (IConnector<Object>) conn;

				// 블로킹 I/O 가능성이 있는 커넥터 실행은 반드시 boundedElastic에서!
				connectorCasted.handle((IntegrationEvent<Object>) event)
					.subscribeOn(Schedulers.boundedElastic())
					.subscribe(
						event.getFutureResponse()::complete,
						event.getFutureResponse()::completeExceptionally
					);
			}, () -> {
				log.warn("지원 커넥터 없음 system={}, direction={}", event.getSystem(), event.getDirection());
				event.getFutureResponse().complete(new BaseResponse(false, "No connector found"));
			});
	}
}