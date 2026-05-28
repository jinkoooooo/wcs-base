/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;

import xyz.elidom.dbist.annotation.DataSourceType;
import xyz.elidom.dbist.ddl.impl.DdlJdbc;
import xyz.elidom.dbist.dml.impl.DmlJdbc2;
import xyz.elidom.dbist.processor.Preprocessor;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.orm.IDataSourceManager;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 시스템 기본 데이터소스외에 다른 데이터소스를 관리하기 위한 매니저
 * 
 * @author shortstop
 */
@Component
public class DataSourceManager implements IDataSourceManager {

	/**
	 * IQueryManager Pool - key : datasource name, value : IQueryManager
	 */
	private Map<String, IQueryManager> QUERY_MANAGER_POOL = new ConcurrentHashMap<String, IQueryManager>(3);

	@Override
	public boolean isExistDataSource(String dataSourceName) {
		return QUERY_MANAGER_POOL.containsKey(dataSourceName);
	}
	
	@Override
	public DataSource getDataSource(String dataSourceName) {
		IQueryManager queryManager = QUERY_MANAGER_POOL.get(dataSourceName);
		if (queryManager == null) {
			throw new ElidomValidationException("DataSource [" + dataSourceName + "] Not Found!");
		}

		return queryManager.getDml().getDataSource();
	}
	
	@Override
	public IQueryManager getQueryManager(String dataSourceName) {
		IQueryManager queryManager = QUERY_MANAGER_POOL.get(dataSourceName);
		if (queryManager == null) {
			throw new ElidomValidationException("DataSource [" + dataSourceName + "] Not Found!");
		}

		return queryManager;
	}

	@Override
	public IQueryManager getQueryManager(Class<?> entityClass) {
		
		xyz.elidom.dbist.annotation.Table tableAnn = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);
		
		// 1. ann 이 null  
		if(tableAnn == null) {
			return this.getDefaultDataSource();
		}
		
		// 2. ann 에서 sourceType 정보 가져오기 
		String sourceType = tableAnn.dataSourceType();
		
		// 3. linkType == SELF or DB_LINK 이면 default ;
		if(ValueUtil.isEqualIgnoreCase(sourceType, DataSourceType.SELF)) {
			return this.getDefaultDataSource();
		}
		
		// 4. settings 에서 xyz.elings.db.ref.name 조회 
		String dbRefName = SettingUtil.getValue(Domain.currentDomainId(), "xyz.elings.db.ref.name." + tableAnn.name());
		if(ValueUtil.isEmpty(dbRefName)) {
			dbRefName = SettingUtil.getValue(Domain.currentDomainId(), "xyz.elings.db.ref.name", "self");
		}
		
		// 5. 최종 setting 이 self 이면 retun default 
		if(ValueUtil.isEqualIgnoreCase(dbRefName, "self")) {
			return this.getDefaultDataSource();
		}
		
		// 6. POOL 검색 
		IQueryManager queryManager = QUERY_MANAGER_POOL.get(dbRefName);
		if (queryManager == null) {
			throw new ElidomValidationException("DataSource [" + dbRefName + "] Not Found!");
		}

		return queryManager;
	}
	
	@Override
	public void initializeDataSource(String dataSourceName, String driverClassName, String url, String user, String passwd, int minIdle, int maxIdle, int maxActive, long maxWait,
			long evictTime) {
		this.initializeDataSource(dataSourceName, driverClassName, url, OrmConstants.DEFAULT_DOMAIN, user, passwd, minIdle, maxIdle, maxActive, maxWait, evictTime);
	}

	@Override
	public void initializeDataSource(String dataSourceName, String driverClassName, String url, String domain, String user, String passwd, int minIdle, int maxIdle, int maxActive,
			long maxWait, long evictTime) {
		if (QUERY_MANAGER_POOL.containsKey(dataSourceName)) {
			this.destroyDataSource(dataSourceName);
		}

		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName(driverClassName);
		ds.setUrl(url);
		ds.setUsername(user);
		ds.setPassword(passwd);
		ds.setMaxTotal(maxActive);
		ds.setMinIdle(minIdle);
		ds.setMaxIdle(maxIdle);
		ds.setMaxWait(Duration.ofMillis(maxWait));
		ds.setDurationBetweenEvictionRuns(Duration.ofMillis(evictTime));
		ds.setValidationQueryTimeout(Duration.ofSeconds(60)); // 60 초 마다 검사 
		// connection 연결 검사 
		// oracle 은 dual 사용 
		if(url.toLowerCase().indexOf(":oracle") != -1) {
			ds.setValidationQuery("select 1 from dual ");
		} else {
			// 나머지는 dual 없음 
			ds.setValidationQuery("select 1 ");
			
			if(url.toLowerCase().indexOf("mysql") > -1 ) {
				ds.setTestWhileIdle(true); // 컨넥션이 놀고 있을때, validationQuery 를 이용해서 유효성 검사를 할지 여부.
			}
		}
		
		try {
			Connection conn = ds.getConnection();
			conn.close();
			
			DataSourceQueryManager queryManager = new DataSourceQueryManager();
			DmlJdbc2 dml = new DmlJdbc2();
			dml.setDomain(domain);
			dml.setDataSource(ds);
			
			JdbcOperations jsbcOperations = new org.springframework.jdbc.core.JdbcTemplate(ds);
			dml.setJdbcOperations(jsbcOperations);
			
			NamedParameterJdbcOperations namedParameterJdbcOperations = new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(ds);
			dml.setNamedParameterJdbcOperations(namedParameterJdbcOperations);
			
			Preprocessor preprocessor = new xyz.elidom.dbist.processor.impl.VelocityPreprocessor();
			dml.setPreprocessor(preprocessor);
			
			DdlJdbc ddl = new DdlJdbc();
			ddl.setDml(dml);
			
			queryManager.setDml(dml);
			queryManager.setDdl(ddl);
			dml.afterPropertiesSet();

			QUERY_MANAGER_POOL.put(dataSourceName, queryManager);

		} catch (Exception e) {
			throw new ElidomServiceException("Failed to initialize datasource!", e.getCause());
		}

	}

	@Override
	public void destroyDataSource(String dataSourceName) {
		DataSource ds = this.getDataSource(dataSourceName);

		if (ds instanceof BasicDataSource) {
			BasicDataSource dataSource = (BasicDataSource) ds;
			if (!dataSource.isClosed()) {
				try {
					dataSource.close();
				} catch (SQLException e) {
				}
			}
		}

		QUERY_MANAGER_POOL.remove(dataSourceName);
	}
	
	@Override
	public Set<String> getDataSourceNames() {
		return QUERY_MANAGER_POOL.keySet();
	}
	
	private IQueryManager getDefaultDataSource() {
		return BeanUtil.get(IQueryManager.class);
	}

}