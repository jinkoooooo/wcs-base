/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.util;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.BeanUtil;

/**
 * Bundle 관련 Utility
 * 
 * @author shortstop
 */
public class BundleUtil {

	/**
	 * bundle, entitySimpleName으로 부터 Entity Class Full Name을 리턴한다. 
	 * 
	 * @param bundle
	 * @param entitySimpleName
	 * @return
	 */
	public static String getEntityClassName(String bundle, String entitySimpleName) {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		IModuleProperties moduleProp = configSet.getConfig(bundle);
		String entityPkg = moduleProp.getScanEntityPackage();
		String entityClsName = entityPkg + OrmConstants.DOT + entitySimpleName;
		return checkEntityClassName(entityClsName);
	}
	
	/**
	 * Entity Class명을 체크 
	 * 
	 * @param entityClassName
	 * @return
	 */
	private static String checkEntityClassName(String entityClassName) {
		if(entityClassName.endsWith(".Entity")) {
			return entityClassName.replace(".Entity", ".Resource");
		} else {
			return entityClassName;
		}
	}
}
