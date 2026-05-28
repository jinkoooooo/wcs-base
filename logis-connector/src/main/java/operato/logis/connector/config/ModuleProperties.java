/* Copyright © Nearsolution Inc. All rights reserved. */
package operato.logis.connector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.FormatUtil;

/**
 * logis-connector 모듈 정보 파일
 * 
 * @author yang
 */
@Component("operatoLogisConnectorModuleProperties")
@EnableConfigurationProperties
@PropertySource("classpath:/properties/logis-connector.properties")
public class ModuleProperties implements IModuleProperties {

	/**
	 * 모듈명
	 */
	@Value("${operato.logis.connector.name}")
	private String name;

	/**
	 * 버전
	 */
	@Value("${operato.logis.connector.version}")
	private String version;

	/**
	 * Module Built Time
	 */
	@Value("${operato.logis.connector.built.at}")
	private String builtAt;

	/**
	 * 모듈 설명
	 */
	@Value("${operato.logis.connector.description}")
	private String description;

	/**
	 * 부모 모듈
	 */
	@Value("${operato.logis.connector.parentModule}")
	private String parentModule;

	/**
	 * 모듈 Base Package
	 */
	@Value("${operato.logis.connector.basePackage}")
	private String basePackage;

	/**
	 * Scan Service Path
	 */
	@Value("${operato.logis.connector.scanServicePackage}")
	private String scanServicePackage;

	/**
	 * Scan Entity Path
	 */
	@Value("${operato.logis.connector.scanEntityPackage}")
	private String scanEntityPackage;

	/**
	 * Project Name
	 * 
	 * @return
	 */
	@Value("${operato.logis.connector.projectName}")
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