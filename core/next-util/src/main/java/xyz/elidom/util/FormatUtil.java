/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Formatting 유틸리티 클래스 
 * 
 * @author shortstop
 */
public class FormatUtil {

	/**
	 * str 문자열이 size보다 작으면 front 혹은 rear에 size만큼 filler를 채워서 리턴한다.  
	 * 
	 * @param str
	 * @param front
	 * @param size
	 * @param filler
	 * @return
	 */
	public static String fixSize(String str, boolean front, int size, String filler) {
		if(str.length() >= size) {
			return str;
			
		} else {
			int fillCount = size - str.length();
			for(int i = 0 ; i < fillCount ; i++) {
				if(front) {
					str = filler + str;
				} else {
					str = str + filler;
				}
			}
			
			return str;
		}
	}
	
	/**
	 * Convert Object To Pretty JSON String
	 * 
	 * @param item
	 * @return
	 */
	public static String toJsonString(Object item) {
		return toJsonString(item, true);
	}
	
	/**
	 * Convert Object To JSON String
	 * 
	 * @param item
	 * @param pretty
	 * @return
	 */
	public static String toJsonString(Object item, boolean pretty) {
		if(pretty) {
			return new GsonBuilder().setPrettyPrinting().create().toJson(item);
		} else {
			return new Gson().toJson(item);
		}
	}	

	/**
	 * Convert JSON String To Object
	 * 
	 * @param jsonStr
	 * @param inputType
	 * @return
	 */
	public static <T> T jsonToObject(String jsonStr, Class<T> inputType) {
		Gson gson = new Gson();
		return gson.fromJson(jsonStr, inputType);
	}
	
	/**
	 * Convert Object To JSON String
	 * 
	 * @param item
	 * @return
	 */
	public static String toUnderScoreJsonString(Object item) {
		Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
		return gson.toJson(item);
	}

	/**
	 * Convert JSON String To Object
	 * 
	 * @param jsonStr
	 * @param inputType
	 * @return
	 */
	public static <T> T underScoreJsonToObject(String jsonStr, Class<T> inputType) {
		Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
		return gson.fromJson(jsonStr, inputType);
	}
	
	/**
	 * json Content를 JSONArray로 파싱 
	 * 
	 * @param content
	 * @return
	 */
	public static JSONArray parseJsonArray(String content) {
		JSONParser jsonParser = new JSONParser();		
		try {
			return (JSONArray) jsonParser.parse(content);
		} catch (org.json.simple.parser.ParseException e) {
			throw new IllegalArgumentException("Failed to parse : " + e.getMessage(), e);
		}
	}
	
	/**
	 * convert from map to object
	 * 
	 * @param mapData
	 * @param entityClass
	 * @return
	 */
	public static Object mapToObject(Map<String, Object> mapData, Class<?> entityClass) {
		Object instance = ClassUtil.newInstance(entityClass);
		Iterator<String> keyIter = mapData.keySet().iterator();
		while(keyIter.hasNext()) {
			String key = keyIter.next();
			Object value = mapData.get(key);
			if(!(value instanceof Map) && ValueUtil.isNotEmpty(value)) {
				if(!ClassUtil.hasField(entityClass, key)) {
					key = FormatUtil.toCamelCase(key);
				}
				
				ClassUtil.setFieldValue(instance, key, value);
			}
		}
		
		return instance;
	}
	
	/**
	 * str substring
	 * 
	 * @param str
	 * @param start
	 * @param end
	 * @return
	 */
	public static String substr(String str, int start, int end) {
		if(str != null && str.length() > (start + end)) {
			str = str.substring(start, end);
		}
		
		return str;
	}
	
	/**
	 * number formatting
	 * 
	 * @param format
	 * @param value
	 * @return
	 */
	public static String numberFormat(String format, Number value) {
		if(ValueUtil.isEmpty(format)) format = "#,###";
		if(value == null) value = 0;
		
	    DecimalFormat df = new DecimalFormat(format);
	    return df.format(value);
	}
	
	/**
	 * camel case 형태의 문자를 under score 형태의 문자로 변환한다.
	 * 
	 * @param camelCaseStr
	 * @return
	 */
	public static String toUnderScore(String camelCaseStr) {
		return camelCaseStr.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
	}
	
	/**
	 * under score 형식의 underScoreStr 문자열을 camel case 형식의 문자열로 변환  
	 * 
	 * @param underScoreStr
	 * @return
	 */
	public static String toCamelCase(String underScoreStr) {
		return ValueUtil.toCamelCase(underScoreStr, '_');
	}
	
	/**
	 * bytes를 KB, MB, GB등으로 변환한다.
	 * 
	 * @param bytes
	 * @param si
	 * @return
	 */
	public static String humanReadableByte(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);		
	}
	
	/**
	 * 파라메터에 값으로 치환하여 메세지를 생성한다.
	 * 
	 * @param message
	 * @param params
	 * @return
	 */
	public static String parseMessge(String message, List<String> params) {
		return (params == null || params.isEmpty()) ? message : MessageFormat.format(message, params.toArray());
	}
}