/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.msg.system.msg;

import java.text.MessageFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import xyz.elidom.msg.rest.TerminologyController;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.msg.I18nTermTranslator;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 다국어 용어 번역기 - 용어 번역을 위한 소스는 없고 기본 언어로만 번역
 * TODO domainId가 들어간 API 추가 필요
 * 
 * @author shortstop
 */
@Component
public class MsgTermTranslator implements I18nTermTranslator {

	/**
	 * Terminology Controller
	 */
	@Autowired
	private TerminologyController termCtrl;
	
	/**
	 * locale, termKey를 기반으로 용어를 조회 
	 *  
	 * @param locale
	 * @param termKey
	 * @return
	 */
	private String findTermByKey(String locale, String termKey) {
		// 1. terms.label.로 시작 : terms.label.name --> 'terms.' 제외하고 label.name
		if (termKey.startsWith(SysConstants.TERM_LABEL_PREFIX)) { 
			String terms = termKey.substring(termKey.indexOf(OrmConstants.DOT) + 1, termKey.length());
			String label = this.termCtrl.findBy(locale, terms);
			return (label != null) ? label : termKey;
			
		// 2. label.으로 시작 : label.name --> 그대로 'label.name'
		} else if (termKey.startsWith(SysConstants.TERM_LABELS)) {
			String label = this.termCtrl.findBy(locale, termKey);
			return (label != null) ? label : termKey;
		
		// 3. terms. 으로 시작 
		// 	case-1) terms.menu.name 즉 '.'이 두 개 이상인 경우 --> 'terms.' 제외하고 'menu.name'
		// 	case-2) terms.name --> 'terms.' 제외하고 label.name
		} else if (termKey.startsWith(SysConstants.TERM_TERMS)) {
			int dotOccurance = StringUtils.countOccurrencesOf(termKey, SysConstants.DOT);
			String terms = (dotOccurance > 1) ? termKey.substring(termKey.indexOf(SysConstants.DOT) + 1, termKey.length()) : 
				SysConstants.TERM_LABEL_PREFIX + termKey.substring(termKey.indexOf(SysConstants.DOT) + 1, termKey.length());
			String label = this.termCtrl.findBy(locale, terms);
			return (label != null) ? label : termKey;
						
		} else {
			return termKey;
		}		
	}
	
	@Override
	public String translate(String locale, String termKey) {
		// 1. 로케일 체크 
		locale = SettingUtil.getLocale(locale);
		// 2. 용어 번역 
		String termValue = this.findTermByKey(locale, termKey);
		// 3. 찾는 값이 없다면 termKey를 리턴
		return SysValueUtil.checkValue(termValue, termKey);
	}

	@Override
	public String translate(String locale, String termKey, String defaultValue) {
		// 1. 로케일 체크 
		locale = SettingUtil.getLocale(locale);
		// 2. 용어 번역 
		String termValue = this.findTermByKey(locale, termKey);
		// 3. 찾는 값이 없다면 defaultValue를 리턴
		return SysValueUtil.checkValue(termValue, SysValueUtil.checkValue(defaultValue, termKey));
	}

	@Override
	public String translate(String locale, String termKey, String defaultValue, List<String> params) {
		// 1. 로케일 체크 
		locale = SettingUtil.getLocale(locale);
		// 2. 메시지 번역 
		String termValue = this.findTermByKey(locale, termKey);
		// 3. 찾는 값이 없다면 defaultValue를 리턴, defaultValue가 없다면 msgCode를 리턴
		termValue = SysValueUtil.checkValue(termValue, SysValueUtil.checkValue(defaultValue, termKey));
		
		// 4. 파라미터가 없다면 값 리턴 
		if(SysValueUtil.isEmpty(params)) {
			return termValue;
		
		// 5. 파라미터가 있다면 번역해서 값 리턴 
		} else {
			Object[] paramStrs = StringUtils.toStringArray(params);
			return MessageFormat.format(termValue, paramStrs);
		}
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
			String paramStr = paramStrs[i].indexOf(OrmConstants.DOT) > 0 ? paramStrs[i] : this.getLabelTermKey(paramStrs[i]);
			transParamStrs[i] = this.findTermByKey(locale, paramStr);
		}
		
		return SysValueUtil.isEmpty(transParamStrs) ? null : SysValueUtil.newStringList(transParamStrs);
	}

	@Override
	public String getMenuTermKey(String key) {
		return SysConstants.TERM_MENU_PREFIX + key;
	}

	@Override
	public String getButtonTermKey(String key) {
		return SysConstants.TERM_BUTTON_PREFIX + key;
	}

	@Override
	public String getLabelTermKey(String key) {
		return SysConstants.TERM_LABEL_PREFIX + key;
	}

	@Override
	public String getSettingKey(String key) {
		return SysConstants.TERM_SETTING_PREFIX + key;
	}
	
}
