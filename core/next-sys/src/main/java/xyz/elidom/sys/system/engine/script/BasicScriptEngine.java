/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.engine.script;

import java.util.Iterator;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.codehaus.groovy.runtime.GStringImpl;
import org.springframework.stereotype.Component;

import xyz.elidom.exception.server.ElidomScriptRuntimeException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.engine.IScriptEngine;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 기본 스크립트 엔진
 * 
 * @author shortstop
 */
@Component
public class BasicScriptEngine implements IScriptEngine {

	/**
	 * RUBY Debug Print Function
	 */
	private static String RUBY_DEBUG_PRINT_FUNC = "def\n debug_print(title, content)\n if(title)\n puts '=' * 20;\n puts title;\n puts '=' * 20;\n end\n if(content)\n puts content;\n puts '=' * 20;\n end\n end";

	/**
	 * ScriptEngineManager
	 */
	private static final ScriptEngineManager scriptManager = new ScriptEngineManager();

	@Override
	public Object runScript(String scriptType, String script, Map<String, Object> variables) throws ElidomScriptRuntimeException {
		ScriptEngine engine = scriptManager.getEngineByName(scriptType);
		this.putVariables(engine, variables);

		try {
			if (scriptType.equalsIgnoreCase("ruby")) {
				this.preProcessRubyEngine(engine);

			} else if (scriptType.equalsIgnoreCase("groovy")) {
				this.preProcessGroovyEngine(engine);
			}

			Object returnValue = engine.eval(script);

			if (scriptType.equalsIgnoreCase("ruby")) {
				return this.postProcessRubyEngine(engine, returnValue);

			} else if (scriptType.equalsIgnoreCase("groovy")) {
				return this.postProcessGroovyEngine(engine, returnValue);

			} else {
				return returnValue;
			}
		} catch (ScriptException se) {
			Throwable e = se;
			while (e instanceof ScriptException) {
				e = e.getCause();
			}

			if (e instanceof ElidomValidationException)
				throw new ElidomValidationException(null, e.getMessage(), se);
			else
				throw new ElidomServiceException(null, e.getMessage(), se);
		}
	}

	/**
	 * put variables to engine
	 * 
	 * @param engine
	 * @param variables
	 */
	private void putVariables(ScriptEngine engine, Map<String, Object> variables) {
		if (SysValueUtil.isNotEmpty(variables)) {
			Iterator<String> keyIter = variables.keySet().iterator();
			while (keyIter.hasNext()) {
				String key = keyIter.next();
				Object value = variables.get(key);
				if (value != null) {
					engine.put(key, value);
				}
			}
		}
	}

	/**
	 * ruby engine - pre process
	 * 
	 * @param engine
	 * @throws
	 */
	private void preProcessRubyEngine(ScriptEngine engine) throws ScriptException {
		ScriptContext context = engine.getContext();
		context.setAttribute("domain", Domain.currentDomain(), ScriptContext.ENGINE_SCOPE);
		context.setAttribute("currentUser", User.currentUser(), ScriptContext.ENGINE_SCOPE);
		context.setAttribute("queryDsl", new xyz.elidom.sys.system.dsl.ruby.QueryDsl(), ScriptContext.ENGINE_SCOPE);
		engine.eval(RUBY_DEBUG_PRINT_FUNC);
	}

	/**
	 * groovy engine - pre process
	 * 
	 * @param engine
	 * @throws ScriptException
	 */
	private void preProcessGroovyEngine(ScriptEngine engine) throws ScriptException {
		ScriptContext context = engine.getContext();
		context.setAttribute("domain", Domain.currentDomain(), ScriptContext.ENGINE_SCOPE);
		context.setAttribute("currentUser", User.currentUser(), ScriptContext.ENGINE_SCOPE);
		context.setAttribute("queryDsl", new xyz.elidom.sys.system.dsl.groovy.QueryDsl(), ScriptContext.ENGINE_SCOPE);
		// engine.eval(GROOVY_DEBUG_PRINT_FUNC);
	}

	/**
	 * ruby engine - post process
	 * 
	 * @param engine
	 * @param returnValue
	 * @return
	 * @throws ScriptException
	 */
	private Object postProcessRubyEngine(ScriptEngine engine, Object returnValue) throws ScriptException {
		return returnValue;
	}

	/**
	 * groovy engine - post process
	 * 
	 * @param engine
	 * @param returnValue
	 * @return
	 * @throws ScriptException
	 */
	private Object postProcessGroovyEngine(ScriptEngine engine, Object returnValue) throws ScriptException {
		if (returnValue instanceof GStringImpl) {
			GStringImpl gStringImpl = (GStringImpl) returnValue;
			return gStringImpl.getValues();
		} else {
			return returnValue;
		}
	}

}