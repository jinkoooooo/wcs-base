package xyz.elidom.sys.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.config.ModuleConfigSet;

/**
 * 이벤트 Publisher
 * 
 * @author shortstop
 */
@Component
public class EventPublisher {

	/**
	 * Event Publisher
	 */
	@Autowired
	protected ApplicationEventPublisher eventPublisher;
	
	@Autowired
	private ModuleConfigSet configSet;


	/**
	 * 이벤트 publish
	 * @param event
	 * @return 처리 결과 
	 */
	public SysEvent publishEvent(SysEvent event) {
		this.eventPublisher.publishEvent(event);
		return event;
	}
	
	/**
	 * 이벤트 publish
	 * @param event
	 * @return 처리 결과 
	 */
	public SysOutEvent publishOutEvent(SysOutEvent event) {
		if(configSet.containsModuleName(event.getTo())){
			this.eventPublisher.publishEvent(event);
			return event;
		} else {
			event.setExecuted(true);
			event.setSendMessage(true);
			
			// TODO : RabbitMQ 모듈을 사용한 전송 
			// 전송 큐 , 수신 큐 설정 필요 어떻게 ? 
			return event;
		}
	}
}
