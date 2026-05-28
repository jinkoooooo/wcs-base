/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.dev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.FormatUtil;

/**
 * Dev 모듈 정보 파일
 * 
 * @author shortstop
 */
@Component
@EnableConfigurationProperties
@PropertySource("classpath:/properties/dev.properties")
public class DevModuleProperties implements IModuleProperties {
	
	/**
	 * 모듈명
	 */
	@Value("${elings.dev.name}")
	private String name;
	
	/**
	 * 버전
	 */
	@Value("${elings.dev.version}")
	private String version;
	
	/**
	 * Module Built Time 
	 */
	@Value("${elings.dev.built.at}")
	private String builtAt;
	
	/**
	 * 모듈 설명
	 */
	@Value("${elings.dev.description}")
	private String description;
	
	/**
	 * 부모 모듈
	 */
	@Value("${elings.dev.parentModule}")
	private String parentModule;
	
	/**
	 * 모듈 Base Package
	 */
	@Value("${elings.dev.basePackage}")
	private String basePackage;
	
	/**
	 * Scan Service Path
	 */
	@Value("${elings.dev.scanServicePackage}")
	private String scanServicePackage;
	
	/**
	 * Scan Entity Path
	 */
	@Value("${elings.dev.scanEntityPackage}")
	private String scanEntityPackage;
	
	/**
	 * Project Name
	 * @return
	 */
	@Value("${elings.dev.projectName}")
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