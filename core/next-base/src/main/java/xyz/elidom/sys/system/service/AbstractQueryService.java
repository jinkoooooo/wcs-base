package xyz.elidom.sys.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.manager.DataSourceManager;
import xyz.elidom.util.BeanUtil;

/**
 * Anythings 기본 최상위 쿼리 서비스
 *  
 * @author shortstop
 */
public class AbstractQueryService {
	/**
	 * Logger
	 */
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	/**
	 * Query Manager
	 */
	@Autowired
	protected IQueryManager queryManager;
	
	/**
	 * 엔티티에서 쿼리 매니저 가져오기 
	 */
	protected IQueryManager getDataSourceQueryManager(Class<?> entity) {
		return BeanUtil.get(DataSourceManager.class).getQueryManager(entity);
	}
}
