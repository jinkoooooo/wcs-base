package xyz.anythings.sys.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import xyz.elidom.util.DateUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DateUtil 확장 유틸리티
 * 
 * @author shortstop
 */
public class AnyDateUtil extends DateUtil {

	/**
	 * 시간 포맷
	 */
	private static String DEFAULT_HOUR_FORMAT = "HH";
	/**
	 * 분 포맷
	 */
	private static String DEFAULT_MIN_FORMAT = "mm";

	/**
	 * date ==> String
	 * 
	 * @param date
	 * @return
	 */
	public static String dateStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat());
		return sdf.format(date);
	}
	
	/**
	 * date + hour
	 * 
	 * @param date
	 * @param addHours
	 * @return
	 */
	public static Date addHours(Date date, int addHours) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		c.add(Calendar.HOUR_OF_DAY, addHours);
		return c.getTime();
	}

	
	/**
	 * currentDate + min
	 * 
	 * @param addMinutes
	 * @return
	 */
	public static Date addMinutes(int addMinutes) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(new Date());
		c.add(Calendar.MINUTE, addMinutes);
		return c.getTime();
	}

	/**
	 * date + min
	 * 
	 * @param date
	 * @param addMinutes
	 * @return
	 */
	public static Date addMinutes(Date date, int addMinutes) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		c.add(Calendar.MINUTE, addMinutes);
		return c.getTime();
	}
	
	/**
	 * get hour string
	 * 
	 * @param date
	 * @return
	 */
	public static String hourStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_HOUR_FORMAT);
		return sdf.format(date);
	}
	
	/**
	 * get hour integer
	 * 
	 * @param date
	 * @return
	 */
	public static int hourInt(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_HOUR_FORMAT);
		return ValueUtil.toInteger(sdf.format(date));
	}
	
	/**
	 * get min integer
	 * 
	 * @param date
	 * @return
	 */
	public static int minInt(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_MIN_FORMAT);
		return ValueUtil.toInteger(sdf.format(date));
	}


	/**
	 * Date set min
	 * 
	 * @param date
	 * @param minutes
	 * @return
	 */
	public static Date setMinutes(Date date, int minutes) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		c.set(Calendar.MINUTE,minutes);
		return c.getTime();
	}

	/**
	 * Date set second
	 * 
	 * @param date
	 * @param seconds
	 * @return
	 */
	public static Date setSeconds(Date date, int seconds) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		c.set(Calendar.SECOND,seconds);
		return c.getTime();
	}

	/**
	 * Date to Year
	 * 
	 * @param date
	 * @return
	 */
	public static String getYear(Date date) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		return ValueUtil.toString(c.get(Calendar.YEAR));
	}
	
	/**
	 * Date to Month
	 * 
	 * @param date
	 * @return
	 */
	public static String getMonth(Date date) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		return ValueUtil.toString(c.get(Calendar.MONTH));
	}
	
	/**
	 * Date to day
	 * 
	 * @param date
	 * @return
	 */
	public static String getDay(Date date) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date);
		return ValueUtil.toString(c.get(Calendar.DAY_OF_MONTH));
	}

}
