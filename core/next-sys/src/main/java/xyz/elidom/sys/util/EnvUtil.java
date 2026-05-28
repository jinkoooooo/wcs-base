package xyz.elidom.sys.util;

import org.springframework.core.env.Environment;

import xyz.elidom.util.BeanUtil;

/**
 * Environment 관련 유틸리티 - 즉 프로퍼티 파일로 부터 값을 읽어서 리턴 
 * 
 * @author shortstop
 */
public class EnvUtil {

	/**
	 * Environment
	 */
	private static Environment env;
	
	/**
	 * 프로퍼티 파일로 부터 값을 읽어서 리턴 
	 * 
	 * @param property
	 * @return
	 */
	public static String getValue(String property) {
		if(env == null) {
			env = BeanUtil.get(Environment.class);
		}
		
		return env.getProperty(property);
	}
	
	/**
	 * 프로퍼티 파일로 부터 값을 읽어서 값이 있다면 리턴하고 없다면 defaultValue를 리턴 
	 * 
	 * @param property
	 * @param defaultValue
	 * @return
	 */
	public static String getValue(String property, String defaultValue) {
		String value = getValue(property);
		return SysValueUtil.isEmpty(value) ? defaultValue : value;
	}
	
}
