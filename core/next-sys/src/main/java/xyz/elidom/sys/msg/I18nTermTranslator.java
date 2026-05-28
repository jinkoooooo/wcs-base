/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.msg;

import java.util.List;

/**
 * 용어 다국어 지원을 위한 번역 기능 인터페이스 
 * 
 * @author shortstop
 */
public interface I18nTermTranslator {
	
	/**
	 * locale, termKey로 메시지를 번역한다. 
	 * 
	 * @param locale
	 * @param termKey
	 * @return termKey를 번역할 수 없다면 null을 리턴한다.
	 */
	public String translate(String locale, String termKey);
	
	/**
	 * locale, termKey로 메시지를 번역한다. 
	 * 
	 * @param locale
	 * @param termKey
	 * @param defaultValue
	 * @return termKey를 번역할 수 없다면 defaultValue를 리턴한다. 
	 */
	public String translate(String locale, String termKey, String defaultValue);
	
	/**
	 * locale, termKey로 메시지를 번역한다. 
	 * termKey에 파라미터가 포함되어 있어서 params까지 번역한다. 
	 * 
	 * @param locale
	 * @param termKey
	 * @param defaultValue
	 * @param params
	 * @return termKey를 번역할 수 없다면 defaultValue를 리턴한다.
	 */
	public String translate(String locale, String termKey, String defaultValue, List<String> params);
	
	/**
	 * paramStrs을 번역한 후 리스트로 변환하여 리턴 
	 * 
	 * @param paramStrs
	 * @return 번역할 수 없다면 받은 값을 리스트로 변환하여 리턴한다.
	 */
	public List<String> translateParams(String... paramStrs);
	
	/**
	 * locale 기반으로 paramStrs을 번역한 후 리스트로 변환하여 리턴 
	 * 
	 * @param locale
	 * @param paramStrs
	 * @return 번역할 수 없다면 받은 값을 리스트로 변환하여 리턴한다.
	 */
	public List<String> translateLocaleParams(String locale, String... paramStrs);	
	
	/**
	 * 전달받은 key로 번역을 위한 메뉴 키를 리턴한다. 'terms.menu.' + key
	 *  
	 * @param key
	 * @return
	 */
	public String getMenuTermKey(String key);
	
	/**
	 * 전달받은 key로 번역을 위한 버튼 키를 리턴한다. 'terms.button.' + key
	 *  
	 * @param key
	 * @return
	 */
	public String getButtonTermKey(String key);
	
	/**
	 * 전달받은 key로 번역을 위한 라벨 키를 리턴한다. 'terms.label.' + key
	 *  
	 * @param key
	 * @return
	 */
	public String getLabelTermKey(String key);
	
	/**
	 * 전달받은 key로 번역을 위한 설정 키를 리턴한다. 'terms.setting.' + key
	 *  
	 * @param key
	 * @return
	 */
	public String getSettingKey(String key);	
	
}
