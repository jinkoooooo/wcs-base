/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util.converter.msg;

/**
 * Object to JSON String Converter
 * 
 * @author shortstop
 */
public interface IJsonSerializer {

	/**
	 * Convert Object to JSON String
	 * 
	 * @param item
	 * @return
	 */
	public String serialize(Object item);
	
}
