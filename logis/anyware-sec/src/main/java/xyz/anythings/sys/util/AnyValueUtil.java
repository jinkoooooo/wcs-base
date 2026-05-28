package xyz.anythings.sys.util;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import net.sf.common.util.ReflectionUtils;
import net.sf.common.util.ValueUtils;
import xyz.anythings.sys.AnyConstants;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.ClassUtil;

/**
 * AnyValueUtil
 * 
 * @author shortstop
 */
public class AnyValueUtil extends SysValueUtil {
	
	/**
	 * 32 바이트 길이의 GUID를 생성하여 리턴
	 * 
	 * @return
	 */
	public static String newUuid32() {
		return UUID.randomUUID().toString().replace(SysConstants.DASH, SysConstants.EMPTY_STRING);
	}
	
	/**
	 * 36 바이트 길이의 GUID를 생성하여 리턴
	 * 
	 * @return
	 */
	public static String newUuid36() {
		return UUID.randomUUID().toString();
	}
		
	/**
	 * objList를 traversing 하면서 fieldName이 value인 객체 리스트를 추출
	 * 
	 * @param objList
	 * @param fieldName
	 * @param value
	 * @return
	 */
	public static <T> List<T> filterListBy(List<T> objList, String fieldName, Object value) {
		List<T> filteredList = new ArrayList<T>();
		
		for(T item : objList) {
			if(SysValueUtil.isEqual(ClassUtil.getFieldValue(item, fieldName), value)) {
				filteredList.add(item);
			}
		}
		
		return filteredList;
	}
	
	/**
	 * objList를 traversing 하면서 fieldName이 value인 객체 찾아 리턴
	 * 
	 * @param objList
	 * @param fieldName
	 * @param value
	 * @return
	 */
	public static <T> T filterBy(List<T> objList, String fieldName, Object value) {
		for(T item : objList) {
			if(SysValueUtil.isEqual(ClassUtil.getFieldValue(item, fieldName), value)) {
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * objList에서 항목의 fieldName값이 중복되지 않은 항목을 골라 리스트로 리턴
	 * 
	 * @param objList
	 * @param fieldName
	 * @return
	 */
	public static <T> List<?> filterListBy(List<T> objList, String fieldName) {
		List<Object> filteredList = new ArrayList<Object>();
		
		for(T item : objList) {
			Object value = ClassUtil.getFieldValue(item, fieldName);
			if(!filteredList.contains(value)) {
				filteredList.add(value);
			}
		}
		
		return filteredList;
	}
	
	/**
	 * objList에서 항목의 fieldName값이 중복되지 않은 것을 골라 리스트로 리턴
	 * 
	 * @param objList
	 * @param fieldName
	 * @return
	 */
	public static <T> List<String> filterValueListBy(List<T> objList, String fieldName) {
		List<String> filteredList = new ArrayList<String>();
		
		for(T item : objList) {
			Object value = ClassUtil.getFieldValue(item, fieldName);
			if(value != null && !filteredList.contains(value)) {
				filteredList.add(SysValueUtil.toString(value));
			}
		}
		
		return filteredList;
	}

	/**
	 * map 재구성 - removeFields 제거, addKeys, values로 추가 
	 * 
	 * @param map
	 * @param removeKeys
	 * @param addKeys
	 * @param values
	 */
	public static void reconfigureMap(Map<String, Object> map, String removeKeys, String addKeys, Object ... values) {
		if(SysValueUtil.isNotEmpty(addKeys)) {
			String[] keyArr = addKeys.split(SysConstants.COMMA);

			if (keyArr.length != values.length) {
				throw ThrowUtil.newMismatchMapKeyValue();
			}

			for (int i = 0; i < keyArr.length; i++) {
				map.put(keyArr[i], values[i]);
			}
		}
		
		if(SysValueUtil.isNotEmpty(removeKeys)) {
			String[] keyArr = removeKeys.split(SysConstants.COMMA);
			for(int i = 0 ; i < keyArr.length ; i++) {
				map.remove(keyArr[i]);
			}
		}		
	}
	
	/**
	 * 대소문자 구분없이 data에서 fieldName을 추출하여 정수값으로 변환
	 * 
	 * @param data
	 * @param fieldName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Integer parseInteger(Map data, String fieldName) {
		if(data == null || fieldName == null) {
			return 0;
		}
		
		fieldName = AnyValueUtil.checkKeyField(data, fieldName);
		return SysValueUtil.toInteger(data.get(fieldName));
	}
	
	/**
	 * 대소문자 구분없이 data에서 fieldName을 추출하여 Float값으로 변환
	 * 
	 * @param data
	 * @param fieldName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Float parseFloat(Map data, String fieldName) {
		if(data == null || fieldName == null) {
			return 0F;
		}
		
		fieldName = AnyValueUtil.checkKeyField(data, fieldName);
		return SysValueUtil.toFloat(data.get(fieldName));
	}
	
	/**
	 * 대소문자 구분없이 data에서 fieldName을 추출하여 문자열로 변환
	 * 
	 * @param data
	 * @param fieldName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static String parseString(Map data, String fieldName) {
		if(data == null || fieldName == null) {
			return null;
		}
		
		fieldName = AnyValueUtil.checkKeyField(data, fieldName);
		return SysValueUtil.toString(data.get(fieldName));
	}
	
	/**
	 * query내 Filter List 중에 filterName에 해당하는 Filter를 찾아 filter의 값을 리턴  
	 * 
	 * @param query
	 * @param filterName
	 * @return
	 */
	public static String getFilterValue(Query query, String filterName) {
		return (query == null || SysValueUtil.isEmpty(filterName)) ? null : getFilterValue(query.getFilter(), filterName);
	}
	
	/**
	 * Filter List 중에 filterName에 해당하는 Filter를 찾아 filter의 값을 리턴
	 * 
	 * @param filters
	 * @param filterName
	 * @return
	 */
	public static String getFilterValue(List<Filter> filters, String filterName) {
		if(SysValueUtil.isNotEmpty(filters)) {
			for(Filter filter : filters) {
				if(SysValueUtil.isEqualIgnoreCase(filter.getName(), filterName)) {
					if(SysValueUtil.isEqualIgnoreCase(filter.getOperator(), "is blank")) {
						return filter.getOperator();
					} else {
						return filter.getValue().toString();
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * data 키를 toLowerCase에 따라 변환하여 리턴 
	 * 
	 * @param data
	 * @param toLowerCase - true : 소문자로 변경, false : 대문자로 변경  
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map convertKey(Map data, boolean toLowerCase) {
		Map newObj = new HashMap(data.size());
		
		Iterator iter = data.keySet().iterator();
		while(iter.hasNext()) {
			String key = iter.next().toString();
			String newKey = toLowerCase ? key.toLowerCase() : key.toUpperCase();
			newObj.put(newKey, data.get(key));
		}
		
		return newObj;
	}
	
	/**
	 * 문자열 배열을 Float형 리스트로 변환 
	 * 
	 * @param values
	 * @return
	 */
	public static List<Float> toFloatList(String[] values) {
		List<Float> floatList = new ArrayList<Float>();
		
		for(String value : values) {
			floatList.add(SysValueUtil.toFloat(value));
		}
		
		return floatList;
	}
	
	/**
	 * Map 형식의 데이터 리스트를 toClazz 형태의 리스트로 복사 
	 * 
	 * @param fromList
	 * @param toClazz
	 * @param fieldNames
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> List<T> populate(List<Map> fromList, Class<T> toClazz, String ... fieldNames) {
		if(SysValueUtil.isEmpty(fromList)) {
			return new ArrayList<T>(1);
		}
		
		List<T> toList = new ArrayList<T>(fromList.size());
		Map<String, Field> fromFields = ReflectionUtils.getFieldByNameMap(ClassUtil.newInstance(toClazz), true);
		int count = fromList.size();
		
		for(int i = 0 ; i < count ; i++) {
			Map item = fromList.get(i);
			T toInstance = (T)ClassUtil.newInstance(toClazz);
			
			for (String fieldName : fieldNames) {
				Object value = item.get(fieldName);
				
				if(value != null) {
					String fieldKey = SysValueUtil.toCamelCase(fieldName, SysConstants.CHAR_UNDER_SCORE);
					Field toField = fromFields.get(fieldKey);
					value = ValueUtils.toRequiredType(value, toField.getType());
					ClassUtil.setFieldValue(toInstance, fieldKey, value);
				}
			}
			
			toList.add(i, toInstance);
		}
		
		return toList;
	}
	
	/**
	 * Map 형식의 데이터 리스트를 toClazz 형태의 리스트로 복사 
	 * 
	 * @param fromList
	 * @param toClazz
	 * @param fromFieldNames
	 * @param toFieldNames
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> List<T> populate(List<Map> fromList, Class<T> toClazz, String[] fromFieldNames, String[] toFieldNames) {
		if(SysValueUtil.isEmpty(fromList)) {
			return new ArrayList<T>(1);
		}
		
		List<T> toList = new ArrayList<T>(fromList.size());
		Map<String, Field> toFields = ReflectionUtils.getFieldByNameMap(ClassUtil.newInstance(toClazz), true);
		int count = fromList.size();
		
		for(int i = 0 ; i < count ; i++) {
			Map item = fromList.get(i);
			T toInstance = (T)ClassUtil.newInstance(toClazz);
			
			for (int j = 0 ; j < fromFieldNames.length ; j++) {
				String fromFieldName = fromFieldNames[j];
				String toFieldName = toFieldNames[j];
				
				Object value = AnyValueUtil.getMapData(item, fromFieldName);
				
				if(value != null) {
					toFieldName = SysValueUtil.toCamelCase(toFieldName, SysConstants.CHAR_UNDER_SCORE);
					Field toField = toFields.get(toFieldName);
					Object fromValue = ValueUtils.toRequiredType(value, toField.getType());
					ClassUtil.setFieldValue(toInstance, toFieldName, fromValue);
				}
			}
			
			toList.add(i, toInstance);
		}
		
		return toList;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T populate(Map fromObj, Class<T> toClazz, String[] fromFieldNames, String[] toFieldNames) {
		if(SysValueUtil.isEmpty(fromObj)) {
			return null;
		}
		
		Map<String, Field> toFields = ReflectionUtils.getFieldByNameMap(ClassUtil.newInstance(toClazz), true);
		T toInstance = (T)ClassUtil.newInstance(toClazz);
			
		for (int j = 0 ; j < fromFieldNames.length ; j++) {
			String fromFieldName = fromFieldNames[j];
			String toFieldName = toFieldNames[j];
			
			Object value = AnyValueUtil.getMapData(fromObj, fromFieldName);
			
			if(value != null) {
				toFieldName = SysValueUtil.toCamelCase(toFieldName, SysConstants.CHAR_UNDER_SCORE);
				Field toField = toFields.get(toFieldName);
				Object fromValue = ValueUtils.toRequiredType(value, toField.getType());
				ClassUtil.setFieldValue(toInstance, toFieldName, fromValue);
			}
		}
			
		return toInstance;
	}
	
	/**
	 * fromObj를 Map으로 변환
	 * 
	 * @param fromObj
	 * @return
	 */
	public static <T> Map<String, ?> populate(T fromObj) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		if(SysValueUtil.isNotEmpty(fromObj)) {
			Map<String, Field> toFields = ReflectionUtils.getFieldByNameMap(fromObj, true);
			Iterator<Field> fieldIter = toFields.values().iterator();
			while(fieldIter.hasNext()) {
				Field field = fieldIter.next();
				Object fieldValue = ClassUtil.getFieldValue(fromObj, field);
				String fieldName = SysValueUtil.toDelimited(field.getName(), SysConstants.CHAR_UNDER_SCORE);
				map.put(fieldName, fieldValue);
			}
		}
		
		return map;
	}
	
	/**
	 * request의 client URL를 체크하여 Sub Domain을 구한다 
	 * 
	 * @param req
	 * @return
	 */
	public static String getClientRequestSubDomain(HttpServletRequest req) {
		String subDomain = AnyValueUtil.getSubDomainFromClientRequest(req);
		
		// subdomain 이 없는 상태 (ex: http://111.111.111.111) 로 접속하면 시스템 도메인으로 판단함.
		if(SysValueUtil.isEmpty(subDomain)) {
			return "_ROOT_";
		} else {
			return subDomain;
		}
	}
	
	/**
	 * 클라이언트 RequestUri를 체크하여 사이트 코드를 추출
	 * 
	 * @param req
	 * @return
	 */
	public static String getSubDomainFromClientRequest(HttpServletRequest req) {
		// TABLET, PDA 환경에서는 client URI 가 없어서 _ROOT_로 생각 함. 
		if(req.getHeader("referer") == null) {
			return AnyConstants.EMPTY_STRING;
		}
		
		String domainId = (String)req.getHeader("x-domain-id");
		Long domainIdLong = SysValueUtil.toLong(domainId, -1L); 
		
		if(SysValueUtil.isNotEmpty(domainId) && domainIdLong == -1) {
			if(SysValueUtil.isEmpty(domainId)) return AnyConstants.EMPTY_STRING;
			return domainId;
		}
		
		
		String requestUri = req.getHeader("referer");
		return URI.create(requestUri).getPath().replaceAll(AnyConstants.SLASH, AnyConstants.EMPTY_STRING);
	}
	
	/**
	 * 표준 날짜 형식(yyyy-MM-dd)을 짧은 형식(yyyyMMdd)으로 변환
	 * 
	 * @param stdDateStr yyyy-MM-dd 형식의 날짜
	 * @return yyyyMMdd 형식의 날짜
	 */
	public static String stdDateFormatToShort(String stdDateStr) {
		return SysValueUtil.isNotEmpty(stdDateStr) ? stdDateStr.replaceAll(OrmConstants.DASH, OrmConstants.EMPTY_STRING) : stdDateStr;
	}
	
	/**
	 * 짧은 날짜 형식(yyyyMMdd)을 표준 날짜 형식(yyyy-MM-dd)으로 변환
	 * 
	 * @param shortDateStr yyyyMMdd 형식의 날짜
	 * @return
	 */
	public static String shortDateFormatToStd(String shortDateStr) {
		if(SysValueUtil.isEmpty(shortDateStr)) {
			return shortDateStr;
		} else {
			return shortDateStr.substring(0, 4) + OrmConstants.DASH + shortDateStr.substring(4, 6) + OrmConstants.DASH + shortDateStr.substring(6, 8);
		}
	}
	
	/**
	 * 리스트를 구분자로 연결한 문자열로 변환
	 * 
	 * @param list
	 * @param delimiter
	 * @return
	 */
	public static String toDelimiteredString(List<String> list, String delimiter) {
		StringJoiner sj = new StringJoiner(delimiter);
		for(String item : list) {
			sj.add(item);
		}
		return sj.toString();
	}
	
	/**
	 * "na" 문자열이면 null로 치환
	 * 
	 * @param str
	 * @return
	 */
	public static String convertNaStrToNull(String str) {
		return SysValueUtil.isEqualIgnoreCase(AnyConstants.NOT_AVAILABLE_STRING, str) ? null : str;
	}
	
	/**
	 * str이 숫자형인지 체크
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNumber(String str) {
		return str.matches("^[0-9]*$");
	}
	
	/**
	 * time 시간이 현재 시간에서 seconds 초 만큼 이후 시간인 지 체크 
	 * 
	 * @param date
	 * @param second
	 * @return
	 */
	public static boolean isTimePassed(Date time, int seconds) {
		long diff = new Date().getTime() - time.getTime();
		long gap = diff / 1000;
		return gap > seconds;
	}
	
	/**
	 * 전화번호를 000-0000-0000 형식으로 변환
	 * 
	 * @param phoneNo
	 * @param separator
	 * @return
	 */
	public static String formatMobilePhoneNumber(String phoneNo, String separator) {
		if(SysValueUtil.isEmpty(phoneNo)) return SysValueUtil.toNotNull(phoneNo);
		
		int phoneNoLen = phoneNo.length();
		separator = SysValueUtil.isEmpty(separator) ? AnyConstants.DASH : separator;
		
		// 010-123-4567
		if(phoneNoLen == 10) {
			return phoneNo.substring(0, 3) + separator + phoneNo.substring(3, 6) + separator + phoneNo.substring(6);
		// 010-1234-5678
		} else if(phoneNoLen == 11) {
			return phoneNo.substring(0, 3) + separator + phoneNo.substring(3, 7) + separator + phoneNo.substring(7);
		} else {
			return phoneNo;
		}
	}
	
	/**
	 * 문자열에서 설정한 자리 수 이상에 대해서 마스킹 처리  
	 * 
	 * @param data
	 * @param maskChar
	 * @param maskingFromIndex
	 * @return
	 */
	public static String maskingAfterIndx(String data, String maskChar, int maskingFromIndex) {
		if(SysValueUtil.isEmpty(data) || data.length() <= maskingFromIndex) {
			return data;
		} else {
			maskChar = SysValueUtil.isEmpty(maskChar) ? AnyConstants.STAR : maskChar;
			String nonMaskingData = data.substring(0, maskingFromIndex);
			return nonMaskingData + AnyConstants.STAR + AnyConstants.STAR + AnyConstants.STAR + AnyConstants.STAR;
		}
	}
	
	/**
	 * 문자열의 maskingIndex번째 문자를 마스킹 처리
	 * 
	 * @param data
	 * @param maskChar 기본값은 *
	 * @param maskingIndex
	 * @return
	 */
	public static String maskingCharByIndex(String data, String maskChar, int maskingIndex) {
		if(SysValueUtil.isEmpty(data)) return data;
		
		int nameLen = data.length();
		if(maskingIndex > nameLen) return data;
		
		StringBuffer buf = new StringBuffer();
		maskChar = SysValueUtil.isEmpty(maskChar) ? AnyConstants.STAR : maskChar;
		
		for(int i = 0 ; i < nameLen ; i++) {
			if(i == maskingIndex) {
				buf.append(maskChar);
			} else {
				buf.append(data.charAt(i));
			}
		}
			
		return buf.toString();
	}
	
	/**
	 * 전화번호의 마지막 네 자리를 maskChar로 마스킹
	 * 
	 * @param phoneNo
	 * @param maskChar 기본값은 *
	 * @return
	 */
	public static String maskingPhoneNo(String phoneNo, String maskChar) {
		if(SysValueUtil.isEmpty(phoneNo)) return phoneNo;
		
		maskChar = SysValueUtil.isEmpty(maskChar) ? AnyConstants.STAR : maskChar;
		int phoneNoLen = phoneNo.length();
		int maskCount = phoneNoLen >= 4 ? 4 : phoneNoLen;
		
		StringBuffer strBuf = new StringBuffer();
		for(int i = 0 ; i < maskCount ; i++) {
			strBuf.append(maskChar);
		}
		String maskStr = strBuf.toString();
		
		if(phoneNoLen <= 4) {
			return maskStr;
		} else {
			return phoneNo.substring(0, phoneNo.length() - 4) + maskStr;
		}
	}

	/**
	 * checkCode가 regExpr룰에 맞는지 체크
	 *  
	 * @param regExpr
	 * @param checkCode
	 * @return
	 */
	public static boolean checkValidateByRegExpr(String regExpr, String checkCode) {
		Pattern p = Pattern.compile("(" + regExpr + ")");
		Matcher m = p.matcher(checkCode);
		return m.find();
	}

}
