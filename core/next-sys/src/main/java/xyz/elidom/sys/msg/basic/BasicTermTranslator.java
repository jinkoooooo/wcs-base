/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.msg.basic;

import java.util.List;

import org.springframework.stereotype.Component;

import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.msg.I18nTermTranslator;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 기본 용어 번역기 - 용어 번역을 위한 소스는 없고 기본 언어로만 번역 
 * 
 * @author shortstop
 */
@Component
public class BasicTermTranslator implements I18nTermTranslator {

	@Override
	public String translate(String locale, String termKey) {
		return SysValueUtil.isEmpty(termKey) ? termKey : SysValueUtil.toCamelCase(termKey, SysConstants.CHAR_UNDER_SCORE);
	}

	@Override
	public String translate(String locale, String termKey, String defaultValue) {
		return defaultValue;
	}

	@Override
	public String translate(String locale, String termKey, String defaultValue, List<String> params) {
		return defaultValue;
	}

	@Override
	public List<String> translateParams(String... paramStrs) {
		String locale = SettingUtil.getUserLocale();
		return this.translateLocaleParams(locale, paramStrs);
	}
	
	@Override
	public List<String> translateLocaleParams(String locale, String... paramStrs) {
		if(SysValueUtil.isEmpty(paramStrs)) {
			return null;
		}
		
		locale = SettingUtil.getLocale(locale);
		String[] transParamStrs = new String[paramStrs.length];
		
		for(int i = 0 ; i < paramStrs.length ; i++) {
			transParamStrs[i] = this.translate(locale, paramStrs[i]);
		}
		
		return SysValueUtil.isEmpty(transParamStrs) ? null : SysValueUtil.newStringList(transParamStrs);
	}
	
	@Override
	public String getMenuTermKey(String key) {
		return this.translate(null, key);
	}

	@Override
	public String getButtonTermKey(String key) {
		return this.translate(null, key);
	}

	@Override
	public String getLabelTermKey(String key) {
		return this.translate(null, key);
	}

	@Override
	public String getSettingKey(String key) {
		return this.translate(null, key);
	}

}
