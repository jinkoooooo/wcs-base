/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.aop;

import java.lang.reflect.Field;
import java.util.Arrays;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.exception.server.ElidomAlreadyExistException;
import xyz.elidom.exception.server.ElidomLicenseException;
import xyz.elidom.exception.server.ElidomScriptRuntimeException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.ErrorLog;
import xyz.elidom.sys.rest.ErrorLogController;
import xyz.elidom.sys.system.controller.handler.filter.IExceptionHandlerFilterChain;
import xyz.elidom.sys.system.service.params.ErrorOutput;
import xyz.elidom.sys.util.ExceptionUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.DateUtil;
import xyz.elidom.util.HttpUtil;

/**
 * RestController에 대한 Controller Advice (ExceptionHandler)
 * 
 * @author shortstop
 */
@ControllerAdvice(annotations=RestController.class)
public class RestExceptionHandlerAspect {
	
	/**
	 * logger
	 */
	private Logger logger = LoggerFactory.getLogger(RestExceptionHandlerAspect.class);
	
	@Resource
	private Environment env;
	
	@Autowired
	private IExceptionHandlerFilterChain exceptionFilterChain;
	
	@ExceptionHandler(value={ Throwable.class })
	public ResponseEntity<Object> handleGeneralException(HttpServletRequest req, HttpServletResponse res, Throwable ex) {
		ElidomException ee = ExceptionUtil.wrapElidomException(ex);
		Object errorOutput = this.handleElidomException(req, res, ee);
		return new ResponseEntity<Object>(errorOutput, HttpStatus.valueOf(ee.getStatus()));
	}
	
	@ExceptionHandler(value={ ElidomException.class })
	public Object handleElidomException(HttpServletRequest req, HttpServletResponse res, ElidomException ex) {		
		ErrorOutput errorOutput = this.handleDefaultException(req, res, ex);
		
		// Exception Handler가 있으면 핸들러가 처리 
		if(this.exceptionFilterChain.isExistFilter()) {
			this.exceptionFilterChain.doFilters(req, res, ex);
		} 
		
		return new ResponseEntity<Object>(errorOutput, HttpStatus.valueOf(ex.getStatus()));
	}
	
	/**
	 * handle exception
	 * 
	 * @param req
	 * @param res
	 * @param ex
	 * @return
	 */
	public ErrorOutput handleDefaultException(HttpServletRequest req, HttpServletResponse res, ElidomException ex) {
		ex = this.setDefaultMessage(ex);
		this.setResponse(res, ex);
		this.setExceptionMessage(ex);
		this.setExceptionWritable(ex);

		String errorId = null;
		try {
			// Print error.
			boolean printErrLog = SysValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.ERROR_PRINT_STACK_TRACE, SysConstants.TRUE_STRING));
			if (printErrLog) {
				if (!(ex instanceof ElidomLicenseException) && ex.isWritable())
					this.logger.error(ex.getMessage(), ex);
			}
			
			// Save Error
			boolean saveErrLogEnable = SysValueUtil.toBoolean(env.getProperty(SysConfigConstants.ERROR_SAVE_ENABLE, SysConstants.TRUE_STRING));
			if (saveErrLogEnable) {
				String errorSaveStatusList = SettingUtil.getValue(SysConfigConstants.ERROR_SAVE_STATUS_LIST, OrmConstants.EMPTY_STRING);
				String[] errorStatusArr = StringUtils.tokenizeToStringArray(errorSaveStatusList, OrmConstants.COMMA);
				
				if (SysValueUtil.isEmpty(errorStatusArr) || Arrays.asList(errorStatusArr).contains(String.valueOf(ex.getStatus()))) {
					errorId = this.logException(req, ex);
				}
			}
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

		return this.getOutput(errorId, ex);
	}

	/**
	 * Get Response Output
	 * 
	 * @param errorId
	 * @param ex
	 * @return
	 */
	private ErrorOutput getOutput(String errorId, ElidomException ex) {
		String detail = (ex.getCause() != null ? ex.getCause() : ex).getMessage();
		return new ErrorOutput(ex.getStatus(), errorId, ex.getTitle(), ex.getUserMessage(), detail);
	}
	
	/**
	 * Response에 status code 설정 
	 * 
	 * @param res
	 * @param ex
	 */
	private void setResponse(HttpServletResponse res, ElidomException ex) {
		res.setStatus(ex.getStatus());
	}
	
	/**
	 * exception의 error code로 다국어 정보를 찾아서 exception의 message에 설정한다.
	 * 
	 * @param ex
	 */
	private void setExceptionMessage(ElidomException ex) {
		String title = MessageUtil.getMessage(ex.getTitle(), null, ex.getParams());
		if (SysValueUtil.isNotEmpty(title)) {
			ex.setTitle(title);
		}

		// Message 다국어 처리
		String message = MessageUtil.getMessage(ex.getCode(), ex.getMessage(), ex.getParams());
		ex.setUserMessage(SysValueUtil.isNotEmpty(message) ? message : ex.getCode());
	}

	/**
	 * 설정 정보로 부터 Exception이 writable인지를 읽어서 설정한다.
	 * 
	 * @param ex
	 */
	private void setExceptionWritable(ElidomException ex) {
		// 커스터마이징 포인트 - 이미 isWritable을 false로 설정했다면 이 설정을 존중하여 변경하지 않는다. 
		if(ex.isWritable()) {
			boolean isWritable = true;
			String simpleName = ex.getClass().getSimpleName();
			String value = SettingUtil.getValue(SysConfigConstants.ERROR_UNWRITABLE_EXCEPTION_TYPES);
			String[] exceptionNames = StringUtils.tokenizeToStringArray(value, OrmConstants.COMMA);
	
			if (SysValueUtil.isNotEmpty(exceptionNames)) {
				for (String name : exceptionNames) {
					if (name.equalsIgnoreCase(simpleName)) {
						isWritable = false;
						break;
					}
				}
			}
			
			ex.setWritable(isWritable);
		}
	}
	
	/**
	 * Save Error Log.
	 * 
	 * @param request
	 * @param exception
	 * @return
	 */
	private String logException(HttpServletRequest request, ElidomException exception) {
		if (!exception.isWritable()) {
			return SysConstants.EMPTY_STRING;
		}

		/*
		 * Code의 값이 기준보다 클경우, 메시지라고 간주하고 Code 정보를 null 처리.
		 */
		String code = exception.getCode();
		Field field = ClassUtil.getField(ErrorLog.class, "code");
		if (code != null && SysValueUtil.isNotEmpty(field)) {
			int size = field.getAnnotation(Column.class).length();
			code = code.length() > size ? null : code;
		}
		
		String currentDate = DateUtil.currentTimeStr();
		ErrorLog errLog = new ErrorLog();
		errLog.setDomainId(Domain.currentDomain().getId());
		errLog.setCode(code);
		errLog.setMessage(exception.getUserMessage());
		errLog.setStatus(String.valueOf(exception.getStatus()));
		errLog.setErrorType(exception.getTitle());
		errLog.setUri(request.getRequestURI());
		errLog.setMethod(request.getMethod());
		errLog.setHeader(HttpUtil.getAllHeader(request));
		errLog.setIssueDate(currentDate);

		if (null != exception.getCause()) {
			String traceStr = ExceptionUtil.getErrorStackTraceToString(exception.getCause());
			errLog.setStackTrace(traceStr);
		}

		return BeanUtil.get(ErrorLogController.class).create(errLog).getId();
	}
	
	/**
	 * 빈번하게 발생하는 에러에 대한 Default Message 처리
	 * 
	 * @param ex
	 */
	private ElidomException setDefaultMessage(ElidomException ex) {
		if (ex instanceof ElidomScriptRuntimeException) {
			return ExceptionUtil.wrapElidomScriptRuntimeException((ElidomScriptRuntimeException) ex);
		}

		Throwable t = ex.getCause();
		if (t == null || !(t instanceof ElidomException)) {
			return ex;
		}

		while (t.getCause() != null && t.getCause() instanceof ElidomException) {
			t = t.getCause();
		}
		
		if (t instanceof DuplicateKeyException) {
			t = new ElidomAlreadyExistException("Already exists.", t);

		} else if (t instanceof DataIntegrityViolationException) {
			t = new ElidomValidationException("Violation of an integrity constraint.", t);

		} else if (t instanceof NumberFormatException) {
			t = new ElidomServiceException("The string does not have the appropriate format.", t);
		}

		return (ElidomException) t;
	}
	
}