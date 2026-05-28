package xyz.elidom.sys.system.service;

/**
 * 데이터소스의 데이터베이스 종류에 따라 쿼리 관리 
 * 
 * @author shortstop
 */
public interface IQueryPathHelper {
	
	/**
	 * 쿼리 스토어를 초기화
	 * 
	 * @param databaseType
	 */
	public void initQueryStore(String databaseType);
	
	/**
	 * 데이터베이스 유형
	 * 
	 * @return
	 */
	public String getDatabaseType();
	
	/**
	 * 기본 쿼리 경로
	 * 
	 * @return
	 */
	public String getBasePath();
	
	/**
	 * queryPath 경로로 부터 쿼리를 찾아 리턴
	 * 
	 * @param queryPath
	 * @return
	 */
	public String getQueryByPath(String queryPath);
}
