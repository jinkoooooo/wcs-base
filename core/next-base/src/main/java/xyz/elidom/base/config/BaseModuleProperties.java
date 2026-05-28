/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.FormatUtil;

/**
 * Base 모듈 정보 파일
 * 
 * @author shortstop
 */
@Component
@EnableConfigurationProperties
@PropertySource("classpath:/properties/base.properties")
public class BaseModuleProperties implements IModuleProperties {
	
	/**
	 * 모듈명
	 */
	@Value("${elings.base.name}")
	private String name;
	
	/**
	 * 버전
	 */
	@Value("${elings.base.version}")
	private String version;
	
	/**
	 * Module Built Time 
	 */
	@Value("${elings.base.built.at}")
	private String builtAt;
	
	/**
	 * 모듈 설명
	 */
	@Value("${elings.base.description}")
	private String description;
	
	/**
	 * 부모 모듈
	 */
	@Value("${elings.base.parentModule}")
	private String parentModule;
	
	/**
	 * 모듈 Base Package
	 */
	@Value("${elings.base.basePackage}")
	private String basePackage;
	
	/**
	 * Scan Service Path
	 */
	@Value("${elings.base.scanServicePackage}")
	private String scanServicePackage;
	
	/**
	 * Scan Entity Path
	 */
	@Value("${elings.base.scanEntityPackage}")
	private String scanEntityPackage;
	
	/**
	 * Project Name
	 * @return
	 */
	@Value("${elings.base.projectName}")
	private String projectName;
	
	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}
	
	public String getBuiltAt() {
		return builtAt;
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