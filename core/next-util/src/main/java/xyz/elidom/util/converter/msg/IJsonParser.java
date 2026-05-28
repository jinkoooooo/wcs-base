/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util.converter.msg;

/**
 * JSON String to Object Converter
 * 
 * @author shortstop
 */
public interface IJsonParser {

	/**
	 * Convert JSON String to Object
	 * 
	 * @param jsonStr
	 * @param inputType
	 * @return
	 */
	public <T> T parse(String jsonStr, Class<T> inputType);
}
