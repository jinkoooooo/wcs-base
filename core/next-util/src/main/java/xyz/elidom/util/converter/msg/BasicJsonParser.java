/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util.converter.msg;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

/**
 * 기본 JSON Parser
 * 
 * @author shortstop
 */
@Component
@Qualifier("basic")
public class BasicJsonParser implements IJsonParser {

	/**
	 * Gson
	 */
	private Gson gson = new Gson();
	
	@Override
	public <T> T parse(String jsonStr, Class<T> inputType) {
		return gson.fromJson(jsonStr, inputType);
	}

}
