/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.engine;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * TemplateEngine 관리자 
 * 
 * @author shortstop
 */
@Component
public class TemplateEngineManager {

	/**
	 * Template Engine 관리 Map
	 */
	private Map<String, ITemplateEngine> templateEngineMgr = new HashMap<String, ITemplateEngine>();
	
	/**
	 * engineName으로 템플릿 엔진 engine을 추가한다. (key : engineName, value : engine)
	 * 
	 * @param engineName
	 * @param engine
	 */
	public void addTemplateEngine(String engineName, ITemplateEngine engine) {
		this.templateEngineMgr.put(engineName, engine);
	}
	
	/**
	 * engineName으로 템플릿엔진을 찾아 리턴 
	 * 
	 * @param engineName
	 * @return
	 */
	public ITemplateEngine getTemplateEngine(String engineName) {
		return this.templateEngineMgr.containsKey(engineName) ? this.templateEngineMgr.get(engineName) : null;
 	}
}
