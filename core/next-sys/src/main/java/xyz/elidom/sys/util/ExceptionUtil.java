/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.util;

import java.nio.file.AccessDeniedException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;

import javax.security.auth.login.AccountExpiredException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.UnexpectedRollbackException;

import xyz.elidom.exception.ElidomException;
import xyz.elidom.exception.client.ElidomUnauthorizedException;
import xyz.elidom.exception.server.ElidomAlreadyExistException;
import xyz.elidom.exception.server.ElidomDatabaseException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.exception.server.ElidomScriptRuntimeException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.sys.SysMessageConstants;

/**
 * Exception 핸들링을 위한 유틸리티 클래스 
 * 
 * @author Minu.Kim
 */
public class ExceptionUtil extends xyz.elidom.util.ExceptionUtil {
	
	/**
	 * wrap exception
	 *  
	 * @param ex
	 * @return
	 */
	public static ElidomException wrapElidomException(Throwable ex) {
		ElidomException elidomException = null;
		
		// ElidomScriptRuntimeException
		if(ex instanceof ElidomScriptRuntimeException) {
			elidomException = wrapElidomScriptRuntimeException((ElidomScriptRuntimeException)ex);
			
		// Elidom Exception
		} else if (ex instanceof ElidomException) {
			elidomException = (ElidomException) ex;
			
		// Service Exception
		} else if (ex instanceof DuplicateKeyException) {
			elidomException = new ElidomAlreadyExistException(SysMessageConstants.ALREADY_EXIST, "Data aleady exist.", ex);
		
		// Null Pointer Exception
		} else if (ex instanceof NullPointerException) {
			elidomException = new ElidomValidationException(SysMessageConstants.NULL_OBJECT_NOT_ALLOWED, "Null Object is not allowed.", ex);
			
		// Class Cast Exception
		} else if (ex instanceof ClassCastException || ex instanceof IndexOutOfBoundsException) {
			elidomException = new ElidomValidationException(SysMessageConstants.VALIDATION_ERROR, ex.getMessage(), ex);
			
		// DB Program Bug Exception
		} else if (ex instanceof BadSqlGrammarException) {
			elidomException = new ElidomDatabaseException(SysMessageConstants.BAD_SQL_GRAMMAR, "Bad SQL Grammar", ex);
			
		// Data Integrity Violation - unique data violation
		} else if (ex instanceof DataIntegrityViolationException) {
			elidomException = new ElidomDatabaseException(SysMessageConstants.INTEGRITY_VIOLATION, "Data Integrity Violation", ex);
			
		// 기타 Database Exception
		} else if (ex instanceof InvalidDataAccessApiUsageException || ex instanceof UnexpectedRollbackException) {
			elidomException = new ElidomDatabaseException(SysMessageConstants.DATA_BASE_ERROR, ex.getMessage(), ex);
			
		// DB Server Exception
		} else if (ex instanceof SQLRecoverableException) {
			elidomException = new ElidomRuntimeException(SysMessageConstants.NETWORK_UNREACHABLE, "Network is unreachable.", ex);
			
		// Database unreachable
		} else if (ex instanceof CannotCreateTransactionException) {
			elidomException = new ElidomRuntimeException(SysMessageConstants.DATABASE_UNREACHABLE, "Database is unreachable.", ex);
			
		// Transaction Exception
		} else if (ex instanceof TransactionTimedOutException || ex instanceof TransientDataAccessResourceException || ex instanceof SQLTimeoutException) {
			elidomException = new ElidomServiceException(SysMessageConstants.TRANSACTION_TIME_OUT, "Transaction timed out.", ex);
			
		// Authentication Exception
		} else if (SysValueUtil.isEqual("InsufficientAuthenticationException", ex.getClass().getSimpleName()) || ex instanceof AccessDeniedException) {
			elidomException = new ElidomServiceException(SysMessageConstants.INCORRECT_AUTHENTICATION, "Authentication is incorrect.", ex);
		
		// 사용자 계정의 유효 기간이 만료 되었습니다.
		} else if (ex instanceof AccountExpiredException) {
			elidomException = new ElidomServiceException(SysMessageConstants.INCORRECT_AUTHENTICATION, "Authentication is incorrect.", ex);

		// Bad Credential Exception
		} else if (SysValueUtil.isEqual("BadCredentialsException", ex.getClass().getSimpleName())) {
			elidomException = new ElidomUnauthorizedException(SysMessageConstants.USER_INVALID_ID_OR_PASS, "ID or Password is incorrect.", ex);

		// Unauthorized user.
		} else if (SysValueUtil.isEqual("DisabledException", ex.getClass().getSimpleName())) {
			elidomException = new ElidomUnauthorizedException(SysMessageConstants.INACTIVE_ACCOUNT, "Inactive Account.", ex);
		
		// Account Locked
		} else if (SysValueUtil.isEqual("LockedException", ex.getClass().getSimpleName())) {
			elidomException = new ElidomUnauthorizedException(SysMessageConstants.ACCOUNT_LOCKED, "Account is Locked. Please, try again later.", ex);

		// Else
		} else {
			elidomException = new ElidomServiceException(ex.getMessage(), ex);
		}

		return elidomException;
	}
	
	/**
	 * ElidomScriptRuntimeException을 wrapping
	 * 
	 * @param sre
	 * @return
	 */
	public static ElidomScriptRuntimeException wrapElidomScriptRuntimeException(ElidomScriptRuntimeException sre) {
		Throwable cause = sre.getCause();
		
		if(cause == null) {
			return sre;
		
		} else {
			Throwable oriCause = cause.getCause();
			if(oriCause == null) {
				if(cause instanceof ElidomScriptRuntimeException) {
					return (ElidomScriptRuntimeException)cause;
				} else {
					return new ElidomScriptRuntimeException(cause.getMessage(), cause);
				}
			} else {
				if(oriCause instanceof ElidomScriptRuntimeException) {
					return (ElidomScriptRuntimeException)oriCause;
				} else {
					return new ElidomScriptRuntimeException(oriCause.getMessage(), oriCause);
				}
			}
		}
	}
}