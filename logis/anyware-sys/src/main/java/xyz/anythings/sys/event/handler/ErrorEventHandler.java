package xyz.anythings.sys.event.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AsyncExceptionHandler;
import xyz.elidom.sys.event.ErrorEvent;

/**
 * 에러 이벤트 핸들러
 * 
 * @author shortstop
 */
@Component
public class ErrorEventHandler {

	/**
	 * 비동기 예외 핸들러
	 */
	@Autowired
	protected AsyncExceptionHandler asyncErrorHandler;

	/**
	 * 에러 이벤트를 처리
	 * 
	 * @param errorEvent
	 */
	@EventListener(classes = ErrorEvent.class)
	public void handleErrorEvent(ErrorEvent errorEvent) {
		this.asyncErrorHandler.handleException(errorEvent);
	}

}
