/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util.converter.msg;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

/**
 * 기본 JSON Serializer
 * 
 * @author shortstop
 */
@Component
@Qualifier("basic")
public class BasicJsonSerializer implements IJsonSerializer {

	/**
	 * Gson
	 */
	private Gson gson = new Gson();
	
	@Override
	public String serialize(Object item) {
		return gson.toJson(item);
	}

}
