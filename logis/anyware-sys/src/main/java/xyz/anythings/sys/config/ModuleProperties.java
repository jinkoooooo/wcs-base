/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.anythings.sys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.FormatUtil;

/**
 * Anythings Sys 모듈 정보 파일
 * 
 * @author shortstop
 */
@Component("anythingsSysModuleProperties")
@EnableConfigurationProperties
@PropertySource("classpath:/properties/anyware-sys.properties")
public class ModuleProperties implements IModuleProperties {

	/**
	 * 모듈명
	 */
	@Value("${anythings.sys.name}")
	private String name;

	/**
	 * 버전
	 */
	@Value("${anythings.sys.version}")
	private String version;

	/**
	 * Module Built Time
	 */
	@Value("${anythings.sys.built.at}")
	private String builtAt;

	/**
	 * 모듈 설명
	 */
	@Value("${anythings.sys.description}")
	private String description;

	/**
	 * 부모 모듈
	 */
	@Value("${anythings.sys.parentModule}")
	private String parentModule;

	/**
	 * 모듈 Scada Package
	 */
	@Value("${anythings.sys.basePackage}")
	private String basePackage;

	/**
	 * Scan Service Path
	 */
	@Value("${anythings.sys.scanServicePackage}")
	private String scanServicePackage;

	/**
	 * Scan Entity Path
	 */
	@Value("${anythings.sys.scanEntityPackage}")
	private String scanEntityPackage;

	/**
	 * Project Name
	 * 
	 * @return
	 */
	@Value("${anythings.sys.projectName}")
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