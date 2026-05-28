/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * Exception 핸들링을 위한 유틸리티 클래스 
 * 
 * @author Minu.Kim
 */
public class ExceptionUtil {
	
	/**
	 * Stack Trace를 관리할 Limit Count
	 */
	private static int stackTraceLimit = 10;
	
	/**
	 * Stack Trace Limit 라인을 설정한다.
	 * 
	 * @param stackTraceLimit
	 */
	public static void setStackTraceLimit(int stackTraceLimit) {
		ExceptionUtil.stackTraceLimit = stackTraceLimit;
	}
	
	/**
	 * InvocationTargetException 의 경우, TargetException 으로 벗겨 냄.
	 * 
	 * @param e
	 * @return
	 */
	public static Exception unwrapException(Exception e) {
		return unwrapException((Throwable) e);
	}

	/**
	 * InvocationTargetException 의 경우, TargetException 으로 벗겨 냄.
	 * 
	 * @param t
	 * @return
	 */
	public static Exception unwrapException(Throwable t) {
		Throwable e = t;
		int i = 0;
		while (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
			if (i++ > 10) {
				break;
			}
		}
		return e instanceof Exception ? (Exception) e : new Exception(e);
	}

	/**
	 * 지정한 Line 수 많큼의 Error Trace를 String 형태로 변환.(개발시 화면에 표시하기 위함.)
	 * 
	 * @param e
	 * @return
	 */
	public static String getErrorStackTraceToString(Throwable e) {
		if (ValueUtil.isEmpty(e)) {
			return null;
		}

		StackTraceElement[] traces = e.getStackTrace();
		int size = traces.length;
		if (size > 10) {
			size = ExceptionUtil.stackTraceLimit;
		}

		StringBuffer errTrace = new StringBuffer();
		errTrace.append(e.toString()).append("\n");
		
		for (int i = 0; i < size; i++) {
			errTrace.append("\t").append(traces[i].toString()).append("\n");
		}

		return errTrace.toString();
	}

	/**
	 * Error Trace를 String 형태로 변환. (에러 로그에 StackTrace를 보여주기 위해 사용)
	 * 
	 * @param e
	 * @return
	 */
	public static String getAllErrorStackTraceToString(Throwable e) {
		if (ValueUtil.isEmpty(e)) {
			return null;
		}
		
		// create new StringWriter object
		StringWriter sWriter = new StringWriter();

		// create PrintWriter for StringWriter
		PrintWriter pWriter = new PrintWriter(sWriter);

		// now print the stacktrace to PrintWriter we just created
		e.printStackTrace(pWriter);

		// use toString method to get stacktrace to String from StringWriter object
		return sWriter.toString();
	}
}