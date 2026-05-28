/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.anythings.sys.util;

import java.text.MessageFormat;
import java.util.List;

import org.springframework.util.StringUtils;

import xyz.elidom.msg.rest.TerminologyController;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * 메시지 혹은 다국어 처리를 위한 Utility
 * 
 * @author shortstop
 */
public class AnyMessageUtil extends MessageUtil {
	
	/*******************************************************
	 * 						추가 API
	 *******************************************************/
	
	/**
	 * termKey로 용어를 찾아 리턴. 없으면 null 리턴
	 * 
	 * @param domainId
	 * @param termKey
	 * @return
	 */
	public static String getTerm(Long domainId, String termKey) {
		String locale = SettingUtil.getUserLocale();
		return getLocaleTerm(domainId, locale, termKey);
	}
	
	/**
	 * termKey로 용어를 찾아 리턴. 없으면 defaultValue 리턴
	 * 
	 * @param domainId
	 * @param termKey
	 * @param defaultValue
	 * @return
	 */
	public static String getTerm(Long domainId, String termKey, String defaultValue) {
		String locale = SettingUtil.getUserLocale();
		return getLocaleTerm(domainId, locale, termKey, defaultValue);
	}
	
	/**
	 * termKey, 파라미터 params로 용어를 찾아 리턴. 없으면 defaultValue 리턴
	 * 
	 * @param domainId
	 * @param termKey
	 * @param defaultValue
	 * @param params
	 * @return
	 */
	public static String getTerm(Long domainId, String termKey, String defaultValue, List<String> params) {
		String locale = SettingUtil.getUserLocale();
		return getLocaleTerm(domainId, locale, termKey, defaultValue, params);
	}
	
	/**
	 * termKey로 용어를 찾아 리턴. 없으면 null 리턴
	 * 
	 * @param domainId
	 * @param locale
	 * @param termKey
	 * @return
	 */
	public static String getLocaleTerm(Long domainId, String locale, String termKey) {
		return translate(domainId, locale, termKey);
	}
	
	/**
	 * termKey로 용어를 찾아 리턴. 없으면 defaultValue 리턴
	 * 
	 * @param domainId
	 * @param locale
	 * @param termKey
	 * @param defaultValue
	 * @return
	 */
	public static String getLocaleTerm(Long domainId, String locale, String termKey, String defaultValue) {
		String termVal = getLocaleTerm(domainId, locale, termKey);
		return SysValueUtil.checkValue(termVal, defaultValue);
	}
	
	/**
	 * termKey, 파라미터 params로 용어를 찾아 리턴. 없으면 defaultValue 리턴
	 * 
	 * @param locale
	 * @param termKey
	 * @param defaultValue
	 * @param params
	 * @return
	 */
	public static String getLocaleTerm(Long domainId, String locale, String termKey, String defaultValue, List<String> params) {
		return translate(domainId, locale, termKey, defaultValue, params);
	}
	
	/**
	 * domainId, locale, termKey를 기반으로 용어를 조회 
	 * 
	 * @param domainId
	 * @param locale
	 * @param termKey
	 * @return
	 */
	public static String translate(Long domainId, String locale, String termKey) {
		// 1. terms.label.로 시작 : terms.label.name --> 'terms.' 제외하고 label.name
		if (termKey.startsWith(SysConstants.TERM_LABEL_PREFIX)) { 
			String terms = termKey.substring(termKey.indexOf(OrmConstants.DOT) + 1, termKey.length());
			String label = BeanUtil.get(TerminologyController.class).findBy(domainId, locale, terms);
			return (label != null) ? label : termKey;
			
		// 2. label.으로 시작 : label.name --> 그대로 'label.name'
		} else if (termKey.startsWith(SysConstants.TERM_LABELS)) {
			String label = BeanUtil.get(TerminologyController.class).findBy(domainId, locale, termKey);
			return (label != null) ? label : termKey;
		
		// 3. terms. 으로 시작 
		// 	case-1) terms.menu.name 즉 '.'이 두 개 이상인 경우 --> 'terms.' 제외하고 'menu.name'
		// 	case-2) terms.name --> 'terms.' 제외하고 label.name
		} else if (termKey.startsWith(SysConstants.TERM_TERMS)) {
			int dotOccurance = StringUtils.countOccurrencesOf(termKey, SysConstants.DOT);
			String terms = (dotOccurance > 1) ? termKey.substring(termKey.indexOf(SysConstants.DOT) + 1, termKey.length()) : 
				SysConstants.TERM_LABEL_PREFIX + termKey.substring(termKey.indexOf(SysConstants.DOT) + 1, termKey.length());
			String label = BeanUtil.get(TerminologyController.class).findBy(domainId, locale, terms);
			return (label != null) ? label : termKey;
						
		} else {
			return termKey;
		}		
	}
	
	/**
	 * domainId, locale, termKey를 기반으로 용어를 조회 
	 * 
	 * @param domainId
	 * @param locale
	 * @param termKey
	 * @param defaultValue
	 * @param params
	 * @return
	 */
	public static String translate(Long domainId, String locale, String termKey, String defaultValue, List<String> params) {
		// 1. 로케일 체크 
		locale = SettingUtil.getLocale(locale);
		// 2. 메시지 번역 
		String termValue = AnyMessageUtil.translate(domainId, locale, termKey);
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
}