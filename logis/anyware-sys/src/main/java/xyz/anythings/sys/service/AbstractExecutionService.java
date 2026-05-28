package xyz.anythings.sys.service;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.elidom.sys.event.EventPublisher;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 실행 가능 기능
 * 
 * @author shortstop
 */
public class AbstractExecutionService extends AbstractQueryService {

	@Autowired
	protected EventPublisher eventPublisher;
}
