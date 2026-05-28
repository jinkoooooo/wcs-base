/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang.math.NumberUtils;

import net.sf.common.util.ValueUtils;

/**
 * Value 빈값 체크, 검증, 변환 등의 데이터 처리를 위한 유틸리티 클래스
 * 
 * @author Minu.Kim
 */
@SuppressWarnings("unchecked")
public class ValueUtil extends ValueUtils {

	/**
	 * Data의 빈값 여부 Check. 값이 비어있을 경우 DefaultValue를 Return
	 * 
	 * @param data
	 * @param defaultValue
	 * @return
	 */
	public static <T> T checkValue(Object data, T defaultValue) {
		return !isEmpty(data) ? (T) data : defaultValue;
	}

	/**
	 * data가 빈 값인지 체크
	 * 
	 * @param data
	 * @return
	 */
	public static boolean isEmpty(Object data) {
		if (data == null) {
			return true;

		} else if (data instanceof String || data instanceof StringBuffer) {
			String str = data.toString().trim();
			return str.isEmpty() || str.equalsIgnoreCase("null");

		} else if (data instanceof Object[]) {
			return ((Object[]) data).length == 0;

		} else if (data instanceof Collection<?>) {
			return ((Collection<?>) data).isEmpty();

		} else if (data instanceof Map<?, ?>) {
			return ((Map<?, ?>) data).isEmpty();
		}

		return false;
	}

	/**
	 * data가 빈 값이 아닌지 체크
	 * 
	 * @param data
	 * @return
	 */
	public static boolean isNotEmpty(Object data) {
		return !isEmpty(data);
	}

	/**
	 * a와 b가 동일한 지 확인.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isEqual(Object a, Object b) {
		if (a == null && b == null) {
			return true;

		} else if (a == null || b == null) {
			return false;

		} else if (a.equals(b)) {
			return true;

		} else if (a instanceof List && b instanceof List) {
			List<Object> aList = (List<Object>) a;
			List<Object> bList = (List<Object>) b;

			if (aList.isEmpty() && bList.isEmpty()) {
				return true;
			} else if (aList.size() != bList.size()) {
				return false;
			}

			int i = 0;
			for (Object aObj : aList) {
				if (isNotEqual(aObj, bList.get(i++))) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * a와 b가 동일한 지 확인.
	 * a, b가 문자열인 경우 ignoreCase 적용
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isEqualIgnoreCase(String a, String b) {
		if (a == null && b == null) {
			return true;

		} else if (a == null || b == null) {
			return false;

		} else if (a.equalsIgnoreCase(b)) {
			return true;

		} else {
			return false;
		}
	}

	/**
	 * a와 b가 동일하지 않은지 확인.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isNotEqual(Object a, Object b) {
		return !isEqual(a, b);
	}

	/**
	 * value가 checkValues에 포함되어 있는지 체크
	 * 
	 * @param value
	 * @param checkValues
	 * @return
	 */
	public static boolean isInclude(Object value, Object... checkValues) {
		for (Object checkValue : checkValues) {
			if (ValueUtil.isEqual(value, checkValue)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * value가 checkValues에 포함되어 있는지 체크
	 * 
	 * @param value
	 * @param checkValues
	 * @return
	 */
	public static boolean isInclude(Object value, String... checkValues) {
		for (Object checkValue : checkValues) {
			if (ValueUtil.isEqual(value, checkValue)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * keys, values로 map을 생성
	 * 
	 * @param keys separated value ex) id,name,description
	 * @param values object value array
	 * @return
	 */
	public static Map<String, Object> newMap(String keys, Object... values) {
		Map<String, Object> map = new HashMap<String, Object>(values == null ? 3 : values.length);

		if (!isEmpty(keys) && !isEmpty(values)) {
			String[] keyArr = keys.split(",");

			if (keyArr.length != values.length) {
				throw new IllegalArgumentException("keys count and values count mismatch!");
			}

			for (int i = 0; i < keyArr.length; i++) {
				map.put(keyArr[i], values[i]);
			}
		}

		return map;
	}

	/**
	 * keys, values로 map을 생성
	 * 
	 * @param keys separated value ex) id,name,description
	 * @param values string value array
	 * @return
	 */
	public static Map<String, String> newStringMap(String keys, String... values) {
		Map<String, String> map = new HashMap<String, String>(values == null ? 3 : values.length);

		if (!isEmpty(keys) && !isEmpty(values)) {
			String[] keyArr = keys.split(",");

			if (keyArr.length != values.length) {
				throw new IllegalArgumentException("keys count and values count mismatch!");
			}

			for (int i = 0; i < keyArr.length; i++) {
				map.put(keyArr[i], values[i]);
			}
		}

		return map;
	}

	/**
	 * List 생성 후 values를 담아서 리턴
	 * 
	 * @param values
	 * @return
	 */
	public static List<Object> newList(Object... values) {
		List<Object> list = new ArrayList<Object>(values == null ? 3 : values.length);
		for (Object value : values) {
			list.add(value);
		}

		return list;
	}

	/**
	 * List 생성 후 values를 담아서 리턴
	 * 
	 * @param values
	 * @return
	 */
	public static List<String> newStringList(String... values) {
		List<String> list = new ArrayList<String>(values == null ? 3 : values.length);
		for (String value : values) {
			list.add(value);
		}

		return list;
	}

	/**
	 * a, b 두 개의 Map을 merge하여 하나의 Map으로 리턴
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
		if (a == null && b == null) {
			return null;
		} else if (a == null && b != null) {
			return b;
		} else if (a != null && b == null) {
			return a;
		} else {
			a.putAll(b);
			return a;
		}
	}

	/**
	 * source의 각 null이 아닌 필드값을 target에 복사한다.
	 * 
	 * @param source
	 * @param target
	 * @param excludeFields
	 * @return
	 */
	@SuppressWarnings("all")
	public static <S, T> T cloneObject(S source, T target, String... excludeFields) {
		Class<?> sourceClass = source.getClass();
		Class<?> targetClass = target.getClass();
		Field[] sourceFields = sourceClass.getDeclaredFields();

		for (Field sourceField : sourceFields) {
			// source class의 private이고 static이 아니고 excludeFields에 포함되지 않은 필드에 대해서만 target으로 필드값 복사
			if (sourceField.getModifiers() == Modifier.PRIVATE && sourceField.getModifiers() != Modifier.STATIC && !isInclude(sourceField.getName(), excludeFields)) {
				Object value = ClassUtil.getFieldValue(source, sourceField);

				if (value != null) {
					// TODO 수정 sourceField가 Reference Field인지 체크
					if (value instanceof Long && value.equals(0L) && sourceField.getName().endsWith("Id")) {
						value = null;
					}

					ClassUtil.setFieldValue(target, sourceField.getName(), value);
				}
			}
		}

		return target;
	}

	/**
	 * value의 첫 글자를 대문자로 변경.
	 * 
	 * @param value
	 * @return
	 */
	public static String toUpperCaseHead(String value) {
		char[] charArr = value.toCharArray();
		StringBuilder sb = new StringBuilder();
		sb.append(String.valueOf(charArr[0]).toUpperCase());

		for (int i = 1; i < charArr.length; i++) {
			sb.append(String.valueOf(charArr[i]));
		}

		return sb.toString();
	}

	/**
	 * Json String 내의 Key 형식을 UnderScore 에서 CamelCase 형식으로 변경.
	 * 
	 * @param listJsonStr
	 * @return
	 */
	public static String parseCamelCasekey(String listJsonStr) {
		List<Map<String, Object>> parseKeyMapList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> examResultMapList = FormatUtil.jsonToObject(listJsonStr, List.class);

		for (Map<String, Object> map : examResultMapList) {
			Map<String, Object> transMap = new HashMap<String, Object>();

			Set<String> keys = map.keySet();
			for (String key : keys) {
				transMap.put(ValueUtil.toCamelCase(key, ValueUtil.toCharacter('_')), map.get(key));
			}
			parseKeyMapList.add(transMap);
		}

		return FormatUtil.toJsonString(parseKeyMapList);
	}

	/**
	 * Remove empty value
	 * 
	 * @param map
	 * @return
	 */
	public static Map<String, Object> removeEmptyValues(Map<String, Object> map) {
		if (ValueUtil.isEmpty(map)) {
			return map;
		}

		Iterator<String> keys = map.keySet().iterator();
		List<String> emptyKeys = new ArrayList<String>();

		while (keys.hasNext()) {
			String key = keys.next();
			if (ValueUtils.isEmpty(map.get(key))) {
				emptyKeys.add(key);
			}
		}

		for (String key : emptyKeys) {
			map.remove(key);
		}

		return map;
	}

	/**
	 * size 만큼의 난수 생성.(정수)
	 * 
	 * @param size
	 * @return
	 */
	public static String generateUid(int size) {
		StringBuilder value = new StringBuilder();

		int firstNo = (int) (Math.random() * 10);
		value.append(firstNo == 0 ? ++firstNo : firstNo);

		for (int i = 1; i < size; i++) {
			value.append((int) (Math.random() * 10));
		}

		return value.toString();
	}

	/**
	 * List 객체를 String Type으로 변환 (구분자 ',')
	 * 
	 * @param list
	 * @return
	 */
	public static String listToString(List<String> list) {
		return listToString(list, ",");
	}

	/**
	 * list내의 엘리먼트를 delimiter를 붙여서 문자열로 리턴하되 중복을 제거하지는 않음
	 * 
	 * @param list
	 * @param delimiter
	 * @return
	 */
	public static String listToString(List<String> list, String delimiter) {
		return listToString(list, delimiter, false);
	}

	/**
	 * list내의 엘리먼트를 delimiter를 붙여서 문자열로 리턴하되 중복을 제거할 지 여부는 removeDuplication값에 따라 변경됨
	 * 
	 * @param list
	 * @param delimiter
	 * @param removeDuplication
	 * @return
	 */
	public static String listToString(List<String> list, String delimiter, boolean removeDuplication) {
		StringJoiner value = new StringJoiner(delimiter);

		if (removeDuplication) {
			Set<String> set = new HashSet<String>();
			for (String str : list)
				set.add(str);

			for (String str : set)
				value.add(str);
		} else {
			for (String str : list)
				value.add(str);
		}

		return value.toString();
	}

	/**
	 * true로 판단될 문자열 셋
	 */
	private final static Set<String> TRUE_STRING_SET = toSet("PASS", "Pass", "pass", "true", "TRUE", "t", "T", "yes", "YES", "Yes", "y", "Y", "P", "p", "on", "1");

	/**
	 * value를 Boolean으로 변환
	 * value가 "PASS", "Pass", "pass", "true", "TRUE", "t", "T", "yes", "YES", "Yes", "y", "Y", "P", "p","on", "1" 인 경우는 true로 리턴
	 * 
	 * @param value
	 * @return
	 */
	public static Boolean toBoolean(Object value) {
		return toBoolean(value, null);
	}
	
	/**
	 * Value를 Type 형태로 변환.
	 * 
	 * @param value
	 * @param type
	 * @return
	 */
	public static <T> T parseType(Object value, Class<T> type) {
		if (type.isAssignableFrom(String.class))
			return (T) ValueUtil.toString(value);
		else if (type.isAssignableFrom(Integer.class))
			return (T) ValueUtil.toString(value);
		else if (type.isAssignableFrom(Long.class))
			return (T) ValueUtil.toString(value);
		else if (type.isAssignableFrom(Float.class))
			return (T) ValueUtil.toString(value);
		else if (type.isAssignableFrom(Double.class))
			return (T) ValueUtil.toString(value);
		else if (type.isAssignableFrom(Date.class))
			return (T) ValueUtil.toString(value);
		else if (type.isAssignableFrom(Boolean.class))
			return (T) ValueUtil.toString(value);
		else
			return null;
	}

	/**
	 * value를 Boolean으로 변환하된 value가 null인 경우 defaultValue를 리턴
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static Boolean toBoolean(Object value, Boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		String str = value.toString().trim();
		if (NumberUtils.isNumber(str)) {
			return NumberUtils.toInt(str) > 0;
		}

		return TRUE_STRING_SET.contains(str);
	}

	/**
	 * PC Host Name 가져오기
	 * 
	 * @return
	 */
	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "Unknown";
		}
	}
}