/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.config.module;

/**
 * Module별 설정 인터페이스 
 *  
 * @author shortstop
 */
public interface IModuleProperties {

	/**
	 * 모듈명 
	 * 
	 * @return
	 */
	public String getName();
	
	/**
	 * Project Name
	 * 
	 * @return
	 */
	public String getProjectName();

	/**
	 * 모듈 설명 
	 * 
	 * @return
	 */
	public String getDescription();
	
	/**
	 * 모듈 버전 
	 * 
	 * @return
	 */
	public String getVersion();
	
	/**
	 * 빌드 시간 
	 * 
	 * @return
	 */
	public String getBuiltAt();
	
	/**
	 * 부모 모듈 
	 * 
	 * @return
	 */
	public String getParentModule();

	/**
	 * 모듈 Base Package 
	 * 
	 * @return
	 */
	public String getBasePackage();
	
	/**
	 * 서비스 스캔 패키지 패스 
	 * 
	 * @return
	 */
	public String getScanServicePackage();
	
	/**
	 * 서비스 스캔 패키지 패스 
	 * 
	 * @return
	 */
	public String getScanEntityPackage();
	
}