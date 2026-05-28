/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util.converter.msg;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * JSON Serializer : Camel Case To Under Score
 * 
 * @author shortstop
 */
@Component
@Qualifier("camel_to_under")
public class CamelToUnderJsonSerializer implements IJsonSerializer {

	/**
	 * Using this naming policy with Gson will modify the Java Field name from
	 * its camel cased form to a lower case field name where each word is
	 * separated by an underscore (_).
	 *
	 * <p>
	 * Here's a few examples of the form "Java Field Name" --->
	 * "JSON Field Name":
	 * </p>
	 * <ul>
	 * <li>someFieldName ---> some_field_name</li>
	 * <li>_someFieldName ---> _some_field_name</li>
	 * <li>aStringField ---> a_string_field</li>
	 * <li>aURL ---> a_u_r_l</li>
	 * </ul>
	 */
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

	/**
	 * Convert Object To JSON String
	 * 
	 * @param item
	 * @return
	 */
	@Override
	public String serialize(Object item) {
		return gson.toJson(item);
	}
}
