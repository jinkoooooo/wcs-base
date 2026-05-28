package xyz.anythings.sys.service;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.sys.entity.ErrorLog;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.event.ErrorEvent;
import xyz.elidom.sys.rest.ErrorLogController;
import xyz.elidom.sys.util.ExceptionUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.DateUtil;

/**
 * 비동기 예외 처리를 위한 핸들러
 * 
 * @author shortstop
 */
@Component
public class AsyncExceptionHandler {

	/**
	 * logger
	 */
	private Logger logger = LoggerFactory.getLogger(AsyncExceptionHandler.class);
	/**
	 * 에러 로그 컨트롤러
	 */
	@Autowired
	private ErrorLogController errLogCtrl;
	
	/**
	 * 에러 메시지 처리
	 * 
	 * @param domainId
	 * @param errorType
	 * @param exception
	 */
	@Transactional(propagation=Propagation.REQUIRES_NEW) 
	public void handleException(ErrorEvent errorEvent) {
		// 1. 예외 추출
		Exception ex = errorEvent.getException();

		// 2. 파일 로깅
		if(errorEvent.isFileLoggingFlag()) {
			this.logger.error(ex.getMessage(), ex);
		}
		
		// 3. DB 로깅 여부 확인
		if(!errorEvent.isDbLoggingFlag()) {
			return;
		}
		
		ElidomException ee = ExceptionUtil.wrapElidomException(ex);
		
		if (ee == null || !ee.isWritable()) {
			return;
		}
		
		ErrorLog errLog = new ErrorLog();
		String code = ee.getCode();
		
		if(SysValueUtil.isNotEmpty(code)) {
			Field field = ClassUtil.getField(ErrorLog.class, "code");
			
			if (SysValueUtil.isNotEmpty(field)) {
				int codeSize = field.getAnnotation(Column.class).length();
				if(code.length() > codeSize) {
					errLog.setHeader(code);
					code = null;
				}
			}
		}
		
		errLog.setDomainId(errorEvent.getDomainId());
		errLog.setCode(code);
		errLog.setStatus(String.valueOf(ee.getStatus()));
		errLog.setErrorType(errorEvent.getErrorType());
		errLog.setMessage(ee.getMessage());
		errLog.setParams(errorEvent.getParameters());
		errLog.setIssueDate(DateUtil.currentTimeStr());
		String traceStr = ExceptionUtil.getErrorStackTraceToString(ee.getCause() != null ? ee.getCause() : ee);
		errLog.setStackTrace(traceStr);
		
		String userId = User.currentUser() != null ? User.currentUser().getId() : null;
		errLog.setCreatorId(userId);
		errLog.setUpdaterId(userId);
		this.errLogCtrl.create(errLog);
	}
	
	/**
	 * 에러 메시지 처리
	 * 
	 * @param domainId
	 * @param errorType
	 * @param exception
	 * @param fileLoggingFlag
	 * @param dbLoggingFlag
	 */
	@Transactional(propagation=Propagation.REQUIRES_NEW) 
	public void handleException(Long domainId, String errorType, Exception exception, boolean fileLoggingFlag, boolean dbLoggingFlag) {
		// 1. 파일 로깅
		if(fileLoggingFlag) {
			this.logger.error(exception.getMessage(), exception);
		}
		
		// 2. DB 로깅 여부 확인
		if(!dbLoggingFlag) {
			return;
		}
		
		ElidomException ee = ExceptionUtil.wrapElidomException(exception);
		
		if (ee == null || !ee.isWritable()) {
			return;
		}
		
		ErrorLog errLog = new ErrorLog();
		String code = ee.getCode();
		
		if(SysValueUtil.isNotEmpty(code)) {
			Field field = ClassUtil.getField(ErrorLog.class, "code");
			
			if (SysValueUtil.isNotEmpty(field)) {
				int codeSize = field.getAnnotation(Column.class).length();
				if(code.length() > codeSize) {
					errLog.setHeader(code);
					code = null;
				}
			}
		}
		
		errLog.setDomainId(domainId);
		errLog.setCode(code);
		errLog.setStatus(String.valueOf(ee.getStatus()));
		errLog.setErrorType(errorType);
		errLog.setMessage(ee.getMessage());
		errLog.setIssueDate(DateUtil.currentTimeStr());
		String traceStr = ExceptionUtil.getErrorStackTraceToString(ee.getCause() != null ? ee.getCause() : ee);
		errLog.setStackTrace(traceStr);
		
		String userId = User.currentUser() != null ? User.currentUser().getId() : null;
		errLog.setCreatorId(userId);
		errLog.setUpdaterId(userId);
		this.errLogCtrl.create(errLog);
	}
	
}
