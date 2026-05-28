/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.util;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.DateUtil;
import xyz.elidom.util.FormatUtil;

/**
 * Value 빈값 체크, 검증, 변환 등의  데이터 처리를 위한 유틸리티 클래스 
 *  
 * @author Minu.Kim
 */
public class SysValueUtil extends xyz.elidom.util.ValueUtil {
	
	/**
	 * From Date
	 */
	private static final String FROM_DATE = "from_date";
	/**
	 * To Date
	 */
	private static final String TO_DATE = "to_date";

	/**
	 * Module의 Base Path 리턴.
	 * 
	 * @param module
	 * @return
	 */
	public static String getBasePath(String module) {
		return getModuleProperties(module).getBasePackage();
	}

	/**
	 * Module Property 리턴
	 * 
	 * @param module
	 * @return
	 */
	public static IModuleProperties getModuleProperties(String module) {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		IModuleProperties moduleConfig = configSet.getConfig(module);
		if (SysValueUtil.isEmpty(moduleConfig)) {
			throw ThrowUtil.newNotAllowedEmptyInfo("terms.label.module");
		}
		
		return moduleConfig;
	}

	/**
	 * 검색 날짜 가져오기 실행.
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static Map<String, String> getSearchDate(String fromDate, String toDate) {
		Map<String, String> map = new HashMap<String, String>();
		int defaultPeriod = SysValueUtil.toInteger(SettingUtil.getValue(SysConfigConstants.SEARCH_DEFAULT_PERIOD, "7"));
		
		// From과 To 날짜가 모두 비어있을 경우, 현재 날짜를 기준으로 7일 전 데이터까지 조회
		if (SysValueUtil.isEmpty(fromDate) && SysValueUtil.isEmpty(toDate)) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			calendar.add(Calendar.DAY_OF_MONTH, defaultPeriod);

			fromDate = DateUtil.dateStr(calendar.getTime(), DateUtil.getDateFormat());
			toDate = DateUtil.dateStr(new Date(), DateUtil.getDateFormat());
			
		// To 날짜가 비어있을 경우, From 날짜를 기준으로 7일 이후 데이터까지 조회.(To 날짜가 오늘보다 클 경우, 오늘 날짜를 To로 지정.)
		} else if (SysValueUtil.isNotEmpty(fromDate) && SysValueUtil.isEmpty(toDate)) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(DateUtil.parse(fromDate, DateUtil.getDateFormat()));
			calendar.add(Calendar.DAY_OF_MONTH, defaultPeriod);
			Date end = calendar.getTime();
			Date currentDate = new Date();
			toDate = DateUtil.dateStr(currentDate.compareTo(end) < 0 ? currentDate : end, DateUtil.getDateFormat());

		} else if (SysValueUtil.isEmpty(fromDate) && SysValueUtil.isNotEmpty(toDate)) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(DateUtil.parse(toDate, DateUtil.getDateFormat()));
			calendar.add(Calendar.DAY_OF_MONTH, defaultPeriod * (-1));
			fromDate = DateUtil.dateStr(calendar.getTime(), DateUtil.getDateFormat());
		}

		map.put(FROM_DATE, fromDate);
		map.put(TO_DATE, toDate);
		return map;
	}
	
	/**
	 * Query to ParamMap
	 * 
	 * @param query
	 * @return
	 */
	public static Map<String, Object> queryToParamMap(String query) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		Filter[] filters = FormatUtil.jsonToObject(query, Filter[].class);
		String startDate = null; 
		String endDate = null;

		if (filters != null) {
			for (Filter filter : filters) {
				String name = filter.getName();
				Object value = filter.getValue();

				if (SysValueUtil.isNotEmpty(name) && SysValueUtil.isNotEmpty(value)) {
					paramMap.put(name, value);
				}
			}
		}

		paramMap.putAll(SysValueUtil.getSearchDate(startDate, endDate));
		return paramMap;
	}	
	
	/**
	 * request의 client URL를 체크하여 Sub Domain을 구한다 
	 * 
	 * @param req
	 * @return
	 */
	public static String getClientRequestSubDomain(HttpServletRequest req) {
		String contextPath = SysValueUtil.getClientRequestUriPath(req);
		
		// subdomain 이 없는 상태 (ex: http://111.111.111.111) 로 접속하면 시스템 도메인으로 판단함.
		if(SysValueUtil.isEmpty(contextPath)) {
			return "_ROOT_";
		} else {
			return contextPath;
		}
	}
	
	/**
	 * 클라이언트 RequestUri를 체크하여 사이트 코드를 추출
	 * 
	 * @param req
	 * @return
	 */
	public static String getClientRequestUriPath(HttpServletRequest req) {
		// TABLET, PDA 환경에서는 client URI 가 없어서 _ROOT_로 생각 함. 
		if(req.getHeader("referer") == null) {
			return OrmConstants.EMPTY_STRING;
		}
		
		String requestUri = req.getHeader("referer");
		return URI.create(requestUri).getPath().replaceAll(OrmConstants.SLASH, OrmConstants.EMPTY_STRING);
	}
	
	/**
	 * data에 fieldName을 추출할 수 있는 문자열 (대, 소문자)을 찾아서 리턴
	 * 
	 * @param data
	 * @param fieldName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static String checkKeyField(Map data, String fieldName) {
		if(!data.containsKey(fieldName)) {
			fieldName = fieldName.toLowerCase();
			if(!data.containsKey(fieldName)) {
				fieldName = fieldName.toUpperCase();
			}
		}
		
		return fieldName;
	}
	
	/**
	 * 대소문자 구분없이 data에서 fieldName을 추출하여 값을 반환
	 * 
	 * @param data
	 * @param fieldName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object getMapData(Map data, String fieldName) {
		if(data == null || fieldName == null) {
			return null;
		}
		
		fieldName = SysValueUtil.checkKeyField(data, fieldName);
		return data.get(fieldName);
	}
	

	/**
	 * request의 remote IP를 리턴 
	 * 
	 * @param request
	 * @return
	 */
	public static String getRemoteIp(HttpServletRequest request) {
		String remoteIp = request.getHeader("X-Forwarded-For");
		
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getHeader("Proxy-Client-IP"); 
        } 
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getHeader("WL-Proxy-Client-IP"); 
        } 
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getHeader("HTTP_CLIENT_IP"); 
        } 
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getHeader("HTTP_X_FORWARDED_FOR"); 
        }
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getHeader("X-Real-IP"); 
        }
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getHeader("X-RealIP"); 
        }
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getHeader("REMOTE_ADDR");
        }
        if (remoteIp == null || remoteIp.length() == 0 || "unknown".equalsIgnoreCase(remoteIp)) { 
        	remoteIp = request.getRemoteAddr(); 
        	
        	if (remoteIp.equalsIgnoreCase(SysConstants.LOCALHOST_V6)) {
    			InetAddress inetAddress = null;
    			try {
    				inetAddress = InetAddress.getLocalHost();
    				remoteIp = inetAddress.getHostAddress();
    			} catch (UnknownHostException e) {
    			}
    		}
        }
         
        return remoteIp;
	}
}
