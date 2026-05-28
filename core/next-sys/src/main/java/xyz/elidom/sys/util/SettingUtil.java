/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.sys.util;

import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Setting;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.rest.SettingController;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 설정 정보 핸들링을 위한 유틸리티
 * 
 * @author Minu.Kim
 */
public class SettingUtil {
	/**
	 * 엔티티 저장 전에 유효성 체크
	 */
	private static boolean ENTITY_VALIDATE_BEFORE_SAVE = true;
	
	/**
	 * 현재 사용자의 로케일을 리턴한다.
	 * 
	 * @return
	 */
	public static String getUserLocale() {
		String locale = User.currentUser() != null ? User.currentUser().getLocale() : null;
		return getLocale(locale);
	}
	
	/**
	 * locale 값이 비었다면 기본 로케일을 리턴한다.
	 * 
	 * @param locale
	 * @return
	 */
	public static String getLocale(String locale) {
		if(ValueUtil.isEmpty(locale)) {
			locale = ValueUtil.checkValue(locale, SettingUtil.getValue(SysConstants.DEFAULT_LOCALE_KEY, SysConstants.EN_US));
		}
		
		return locale;
	}
	
	/**
	 * 엔티티 저장 전에 Validation Check (Not Null Field Check, Unique Field Check) 할 지 여부
	 * 
	 * @return
	 */
	public static boolean isEntityValidateBeforeSave() {
		return ENTITY_VALIDATE_BEFORE_SAVE;
	}
	
	/**
	 * 엔티티 저장 전에 Validation Check 할 지 여부 설정
	 * 
	 * @return
	 */
	public static void setEntityValidateBeforeSave(boolean entityValidateBeforeSave) {
		ENTITY_VALIDATE_BEFORE_SAVE = entityValidateBeforeSave;
	}
	
	/**
	 * name으로 설정값을 조회하여 리턴
	 * 
	 * @param name
	 * @return
	 */
	public static String getValue(String name) {
		return getValue(name, null);
	}

	/**
	 * name으로 설정값을 조회하여 리턴, 없으면 defaultValue 리턴
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getValue(String name, String defaultValue) {
		// Cache 적용된 Find API
		SettingController ctrl = BeanUtil.get(SettingController.class);
		Setting setting = ctrl.findOne(SysConstants.SHOW_BY_NAME_METHOD, name);

		if (ValueUtil.isEmpty(setting)) {
			return defaultValue;
		}

		return ValueUtil.checkValue(setting.getValue(), defaultValue);
	}
	
	/**
	 * 수정1. domainId, name으로 설정값을 조회하여 리턴
	 * 
	 * @param domainId
	 * @param name
	 * @return
	 */
	public static String getValue(Long domainId, String name) {
		return getValue(domainId, name, null);
	}
	
	/**
	 * 수정2. domainId, name으로 설정값을 조회하여 리턴, 없으면 defaultValue 리턴
	 * 
	 * @param domainId
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getValue(Long domainId, String name, String defaultValue) {
		// Cache 적용된 Find API
		SettingController ctrl = BeanUtil.get(SettingController.class);
		Setting setting = ctrl.findByName(domainId, name);

		if (ValueUtil.isEmpty(setting)) {
			return defaultValue;
		}

		return ValueUtil.checkValue(setting.getValue(), defaultValue);
	}
}