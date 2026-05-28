/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.engine;

import java.util.Map;

import org.springframework.stereotype.Component;

import xyz.elidom.exception.server.ElidomScriptRuntimeException;

/**
 * Script Engine
 * 
 * @author shortstop
 */
@Component
public interface IScriptEngine {
	/**
	 * Script Type - groovy
	 */
	public static final String SCRIPT_GROOVY = "groovy";
	/**
	 * Script Type - ruby
	 */	
	public static final String SCRIPT_RUBY = "ruby";
	/**
	 * Script Type - javascript
	 */
	public static final String SCRIPT_JAVASCRIPT = "javascript";
	
	/**
	 * script source에 variable을 바인딩해서 실행한다.
	 * 
	 * @param scriptType
	 * @param script
	 * @param variables
	 * @return
	 * @throws ElidomScriptRuntimeException
	 */
	public Object runScript(String scriptType, String script, Map<String, Object> variables) throws ElidomScriptRuntimeException;
	
}
