/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.msg;

import java.util.List;

/**
 * 다국어 지원을 위한 번역 기능 인터페이스 
 * 
 * @author shortstop
 */
public interface I18nMsgTranslator {

	/**
	 * locale, msgCode로 메시지를 번역한다. 
	 * 
	 * @param locale
	 * @param msgCode
	 * @return msgCode를 번역할 수 없다면 null을 리턴한다.
	 */
	public String translate(String locale, String msgCode);
	
	/**
	 * locale, msgCode로 메시지를 번역한다. 
	 * 
	 * @param locale
	 * @param msgCode
	 * @param defaultValue
	 * @return msgCode를 번역할 수 없다면 defaultValue를 리턴한다. 
	 */
	public String translate(String locale, String msgCode, String defaultValue);
	
	/**
	 * locale, msgCode로 메시지를 번역한다. 
	 * msgCode에 파라미터가 포함되어 있어서 params까지 번역한다. 
	 * 
	 * @param locale
	 * @param msgCode
	 * @param defaultValue
	 * @param params
	 * @return msgCode를 번역할 수 없다면 defaultValue를 리턴한다.
	 */
	public String translate(String locale, String msgCode, String defaultValue, List<String> params);
	
}
