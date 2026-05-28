/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Date 관련 유틸리티 클래스 
 * 
 * @author shortstop
 */
public class DateUtil {
	
	private static String LOCALE_UTC = "UTC";
	
	/**
	 * 기본 Default Date Format
	 */
	private static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
	/**
	 * 기본 Default Time Format
	 */
	private static String DEFAULT_TIME_FORMAT = "HH:mm:ss";
	/**
	 * 기본 Default Date Time Format
	 */
	private static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	/**
	 * 기본 Default Date Time Format
	 */
	private static String DEFAULT_DETAIL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * Date Format
	 * 
	 * @return
	 */
	public static String getDateFormat() {
		return DEFAULT_DATE_FORMAT;
	}
	
	/**
	 * Time Format
	 *  
	 * @return
	 */
	public static String getTimeFormat() {
		return DEFAULT_TIME_FORMAT;
	}
	
	/**
	 * Date Time Format
	 * 
	 * @return
	 */
	public static String getDateTimeFormat() {
		return DEFAULT_DATE_TIME_FORMAT;
	}
	
	/**
	 * Date Time Format
	 * 
	 * @return
	 */
	public static String getDetailDateTimeFormat() {
		return DEFAULT_DETAIL_DATE_TIME_FORMAT;
	}
	
	/**
	 * 오늘 날짜를 기본 날짜 포맷으로 변환
	 * 
	 * @return
	 */
	public static String todayStr() {
		return dateStr(new Date(), getDateFormat());
	}
	
	/**
	 * 오늘 날짜를 날짜 포맷으로 변환
	 * 
	 * @return
	 */	
	public static String todayStr(String format) {
		return dateStr(new Date(), format);
	}
	
	/**
	 * 현재 시간을 기본 날짜 포맷으로 변환
	 * 
	 * @return
	 */
	public static String currentTimeStr() {
		return dateStr(new Date(), getDateTimeFormat());
	}
	
	/**
	 * 파라미터로 넘어온 시간을 파라미터로 넘어온 날짜 포맷으로 변환
	 * 
	 * @return
	 */	
	public static String dateTimeStr(Date date, String format) {
		return dateStr(date, format);
	}
	
	/**
	 * 파라미터로 넘어온 시간을 기본 날짜 포맷으로 변환
	 * 
	 * @return
	 */	
	public static String dateTimeStr(Date date) {
		return dateTimeStr(date, getDateTimeFormat());
	}	
	
	/**
	 * 현재 날짜를 기본 날짜 포맷으로 변환
	 * 
	 * @return
	 */
	public static String currentDate() {
		return dateStr(new Date(), DateUtil.getDateFormat());
	}
	
	/**
	 * Date 객체를 기본 날짜 포맷으로 변환
	 * 
	 * @param date
	 * @return
	 */
	public static String defaultDateStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat());
		return sdf.format(date);		
	}
	
	/**
	 * Date 객체를 날짜 포맷으로 변환
	 * 
	 * @param date
	 * @param format
	 * @return
	 */
	public static String dateStr(Date date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);		
	}
	
	/**
	 * Date String을 Date 객체로 변환 
	 * 
	 * @param dateTimeStr
	 * @return
	 */
	public static Date parse(String dateTimeStr) {
		return parse(dateTimeStr, getDateTimeFormat());
	}
	
	/**
	 * Date String을 Date 객체로 변환 
	 * 
	 * @param dateStr
	 * @param format
	 * @return
	 */
	public static Date parse(String dateStr, String format) {
		try {
			return new SimpleDateFormat(format).parse(dateStr);
		} catch(ParseException ex) {
			throw new IllegalArgumentException("Parse date error [" + dateStr + "]", ex);
		}
	}
	
	/**
	 * UTC 기준의 현재 시간 가져오기 실행
	 * 
	 * @return
	 */
	public static Date getCurrentUtcDate() {
		return parseLocaleDate(LOCALE_UTC);
	}

	/**
	 * Locale에 대한 시간 가져오기 실행
	 * 
	 * @param locale
	 * @return
	 */
	public static Date parseLocaleDate(String locale) {
		return DateUtil.parseLocaleDate(new Date(), locale);
	}

	public static Date parseLocaleDate(Date utcDate, String locale) {
		DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DETAIL_DATE_TIME_FORMAT);
		dateFormat.setTimeZone(TimeZone.getTimeZone(locale));
		return DateUtil.parse(dateFormat.format(utcDate), DEFAULT_DETAIL_DATE_TIME_FORMAT);
	}
	
	/**
	 * date에 addDate 만큼 추가한 날짜를 리턴 
	 * 
	 * @param date
	 * @param addDate
	 * @return
	 */
	public static Date addDate(Date date, int addDate) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		c.add(Calendar.DATE, addDate);
		return c.getTime();
	}
	
	/**
	 * date에 addDate 만큼 추가한 날짜를 리턴 
	 * 
	 * @param date
	 * @param addDate
	 * @return
	 */
	public static String addDateToStr(Date date, int addDate) {
		Date value = addDate(date, addDate);
		return new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(value);
	}
		
	/**
	 * 데이트를 timezone을 적용하여 디폴트 포맷으로 문자열로 리턴한다.
	 * 
	 * @param date
	 * @param timezone
	 * @return
	 */
	public static String timezoneStr(Date date, String timezone) {
		return timezoneStr(date, timezone, getDateTimeFormat());
	}

	/**
	 * 데이트를 timezone을 적용하여 포맷팅하여 문자열로 리턴한다.
	 * 
	 * @param date
	 * @param timezone
	 * @param format
	 * @return
	 */
	public static String timezoneStr(Date date, String timezone, String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		TimeZone timeZone = TimeZone.getTimeZone(timezone);
		dateFormat.setTimeZone(timeZone);		
		return dateFormat.format(date);
	}
	
	/**
	 * Customizing timezone returning Date type
	 * 
	 * @param date
	 * @param timezone
	 * @return
	 */
	public static Date timezoneDate(Date date, String timezone) {		
		return parse(timezoneStr(date, timezone));
	}
	
	/**
	 * 현재 년도 가져오기 실행.
	 * 
	 * @return
	 */
	public static String getYear() {
		return dateInfoMap().get(Calendar.YEAR);
	}
	
	/**
	 * 현재 월 리턴.
	 * 
	 * @return
	 */
	public static String getMonth() {
		return dateInfoMap().get(Calendar.MONTH);
	}
	
	/**
	 * 현재 일자 리턴.
	 * 
	 * @return
	 */
	public static String getDay() {
		return dateInfoMap().get(Calendar.DATE);
	}
	
	/**
	 * 현재 시간 리턴.
	 * 
	 * @return
	 */
	public static String getHour() {
		return dateInfoMap().get(Calendar.HOUR_OF_DAY);
	}
	
	/**
	 * 현재 분 리턴.
	 * 
	 * @return
	 */
	public static String getMinute() {
		return dateInfoMap().get(Calendar.MINUTE);
	}
	
	/**
	 * 현재 초 리턴.
	 * 
	 * @return
	 */
	public static String getSecond() {
		return dateInfoMap().get(Calendar.SECOND);
	}
	
	/**
	 * 현재 날짜의 년월 리턴.
	 * 
	 * @return
	 */
	public static String getCurrentMonth() {
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> map = dateInfoMap();
		sb.append(map.get(Calendar.YEAR));
		sb.append(map.get(Calendar.MONTH));
		return sb.toString();
	}
	
	/**
	 * 현재 날짜의 년월일 리턴 
	 * 
	 * @return
	 */
	public static String getCurrentDay() {
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> map = dateInfoMap();
		sb.append(getCurrentMonth());
		sb.append(map.get(Calendar.DATE));
		return sb.toString();
	}
	
	/**
	 * 현재 날짜의 년월일시 리턴 
	 * 
	 * @return
	 */
	public static String getCurrentHour() {
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> map = dateInfoMap();
		sb.append(getCurrentDay());
		sb.append(map.get(Calendar.HOUR_OF_DAY));
		return sb.toString();
	}
	
	/**
	 * 현재 날짜의 년월일시분 리턴 
	 * 
	 * @return
	 */
	public static String getCurrentMinute() {
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> map = dateInfoMap();
		sb.append(getCurrentHour());
		sb.append(map.get(Calendar.MINUTE));
		return sb.toString();
	}
	
	/**
	 * 현재 날짜의 년월일시분초 리턴 
	 * 
	 * @return
	 */
	public static String getCurrentSecond() {
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> map = dateInfoMap();
		sb.append(getCurrentMinute());
		sb.append(map.get(Calendar.SECOND));
		return sb.toString();
	}
	
	/**
	 * 현재 날짜에 대한 년도, 월, 일자 정보를 Map으로 리턴 
	 * 
	 * @return
	 */
	public static Map<Integer, String> dateInfoMap() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());

		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(Calendar.YEAR, Integer.toString(calendar.get(Calendar.YEAR)));
		map.put(Calendar.MONTH, Integer.toString(calendar.get(Calendar.MONTH) + 1));
		map.put(Calendar.DATE, Integer.toString(calendar.get(Calendar.DATE)));
		map.put(Calendar.HOUR_OF_DAY, Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)));
		map.put(Calendar.MINUTE, Integer.toString(calendar.get(Calendar.MINUTE)));
		map.put(Calendar.SECOND, Integer.toString(calendar.get(Calendar.SECOND)));

		for (int key : map.keySet()) {
			String value = map.get(key);
			if (value.length() == 1) {
				map.put(key, "0" + value);
			}
		}
		return map;
	}
	
	/**
	 * From과 To의 경과 시간 추출(초).
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static int elapsedTime(Date fromDate, Date toDate) {
		long fromTime = fromDate.getTime();
		long toTime = toDate.getTime();
		
		if(fromTime >= toTime) {
			return 0;
		}
		
		long elapsedTime = (toTime - fromTime) / 1000;
		return ValueUtil.toInteger(elapsedTime);
	}
	
	/**
	 * From과 To의 경과 시간 추출(분).
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static float elapsedMinute(Date fromDate, Date toDate) {
		int timeGap = elapsedTime(fromDate, toDate);
		if(timeGap == 0) {
			return 0f;
		}
		
		return secondsToMinutes(timeGap);
	}
	
	/**
	 * 초를 분으로 변환 
	 * 
	 * @param seconds
	 * @return
	 */
	public static Float secondsToMinutes(int seconds) {
		return ValueUtil.toFloat((seconds < 60) ? 1.0 : (seconds / 60.0));
	}
	
	/**
	 * Compares base date to current Date for ordering.
	 * 
	 * @param baseTime 기준 날짜
	 * @return
	 */
	public static boolean isBiggerThenCurrentDate(String baseTime) {
		return isBiggerThenTargetDate(baseTime, DateUtil.todayStr(DEFAULT_DATE_FORMAT));
	}

	/**
	 * Compares two Dates for ordering.
	 * (baseDate greater than targetDate : true, baseDate less than equals targetDate : false)
	 * 
	 * @param baseDate 기준 날짜
	 * @param targetDate 대상 날짜
	 * @return
	 */
	public static boolean isBiggerThenTargetDate(String baseDate, String targetDate) {
		try {
			Date baseTimeDate = new SimpleDateFormat(DEFAULT_DATE_FORMAT).parse(baseDate);
			Date targetTimeDate = new SimpleDateFormat(DEFAULT_DATE_FORMAT).parse(targetDate);
			return !(baseTimeDate.compareTo(targetTimeDate) < 1);
		} catch (ParseException e) {
			return false;
		}
	}

	/**
	 * 시작과 종료 날짜를 START, END 키워드로 매핑하여 리턴 
	 * 
	 * @param value
	 * @return
	 */
	/*public static Map<String, String> getWorkDate(String value) {
		Map<String, String> map = new HashMap<String, String>();

		String fromToParam = value;
		if (fromToParam.contains(",")) {
			int index = value.lastIndexOf(",");
			map.put("START", value.substring(0, index));
			map.put("END", value.substring(index + 1, value.length()));
		} else {
			map.put("START", value);
		}
		
		return map;
	}*/
}