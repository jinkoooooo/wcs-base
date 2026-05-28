/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.FormatUtil;

/**
 * Sys 모듈 정보 파일
 * 
 * @author shortstop
 */
@Component
@EnableConfigurationProperties
@PropertySource("classpath:/properties/sys.properties")
public class SysModuleProperties implements IModuleProperties {
	
	/**
	 * Module Name
	 */
	@Value("${elings.sys.name}")
	private String name;
	
	/**
	 * Module Build Version
	 */
	@Value("${elings.sys.version}")
	private String version;
	
	/**
	 * Module Built Time 
	 */
	@Value("${elings.sys.built.at}")
	private String builtAt;
	
	/**
	 * Description of this module
	 */
	@Value("${elings.sys.description}")
	private String description;
	
	/**
	 * Parent module 
	 */
	@Value("${elings.sys.parentModule}")
	private String parentModule;
	
	/**
	 * Base package of this module
	 */
	@Value("${elings.sys.basePackage}")
	private String basePackage;
	
	/**
	 * Service Controller Package Path To Scan this module
	 */
	@Value("${elings.sys.scanServicePackage}")
	private String scanServicePackage;
	
	/**
	 * Entity Package Path To Scan this module
	 */
	@Value("${elings.sys.scanEntityPackage}")
	private String scanEntityPackage;
	
	/**
	 * Project Name of this module
	 */
	@Value("${elings.sys.projectName}")
	private String projectName;
	
	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}
	
	public String getBuiltAt() {
		return this.builtAt;
	}

	public String getDescription() {
		return this.description;
	}
	
	public String getParentModule() {
		return this.parentModule;
	}

	public String getBasePackage() {
		return this.basePackage;
	}

	public String getScanServicePackage() {
		return this.scanServicePackage;
	}

	public String getScanEntityPackage() {
		return this.scanEntityPackage;
	}
	
	public String getProjectName() {
		return this.projectName;
	}

	@Override
	public String toString() {
		return FormatUtil.toJsonString(this);
	}
}
