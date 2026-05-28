/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm;

import java.util.Set;

import javax.sql.DataSource;

/**
 * 시스템 기본 데이터소스외에 다른 데이터소스를 관리하기 위한 매니저 
 * 
 * @author shortstop
 */
public interface IDataSourceManager {
	
	/**
	 * DataSource 명으로 데이터소스가 존재하는지 체크
	 * 
	 * @param dataSourceName
	 * @return
	 */
	public boolean isExistDataSource(String dataSourceName);

	/**
	 * DataSource 명으로 데이터소스를 찾는다.
	 * 
	 * @param dataSourceName
	 * @return
	 */
	public DataSource getDataSource(String dataSourceName);
	
	/**
	 * DataSource 명 목록 가져오기 실행.
	 * 
	 * @return
	 */
	public Set<String> getDataSourceNames();
	
	/**
	 * DataSource 명으로 IQueryManager를 찾는다.
	 * 
	 * @param dataSourceName
	 * @return
	 */
	public IQueryManager getQueryManager(String dataSourceName);
	
	/**
	 * DataSource를 초기화
	 * 
	 * @param dataSourceName
	 * @param driverClassName
	 * @param url
	 * @param user
	 * @param passwd
	 * @param minIdle
	 * @param maxIdle
	 * @param maxActive
	 * @param maxWait
	 * @param evictTime
	 */
	public void initializeDataSource(
			String dataSourceName, 
			String driverClassName, 
			String url, 
			String user, 
			String passwd,
			int minIdle, 
			int maxIdle, 
			int maxActive, 
			long maxWait, 
			long evictTime);
	
	/**
	 * DataSource를 초기화
	 * 
	 * @param dataSourceName
	 * @param driverClassName
	 * @param url
	 * @param user
	 * @param passwd
	 * @param minIdle
	 * @param maxIdle
	 * @param maxActive
	 * @param maxWait
	 * @param evictTime
	 */
	public void initializeDataSource(
			String dataSourceName, 
			String driverClassName, 
			String url,
			String domain,
			String user, 
			String passwd,
			int minIdle, 
			int maxIdle, 
			int maxActive, 
			long maxWait, 
			long evictTime);
	
	/**
	 * DataSource를 삭제 
	 * 
	 * @param dataSourceName
	 */
	public void destroyDataSource(String dataSourceName);
}
