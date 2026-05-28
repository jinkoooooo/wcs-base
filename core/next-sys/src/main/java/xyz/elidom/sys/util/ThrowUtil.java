/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

//import xyz.anythings.sys.ConfigConstants;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.exception.client.ElidomBadRequestException;
import xyz.elidom.exception.client.ElidomInvalidParamsException;
import xyz.elidom.exception.client.ElidomRecordNotFoundException;
import xyz.elidom.exception.client.ElidomServiceNotFoundException;
import xyz.elidom.exception.client.ElidomUnauthorizedException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.exception.server.ElidomScriptRuntimeException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;

/**
 * 메시지가 적용된 Exception 생성을 위한 공통 유틸리티 
 * 
 * @author shortstop
 */
public class ThrowUtil {
	
	/**
	 * 데이터가 존재하지 않는 경우 메시지
	 * 
	 * @param code
	 * @param params
	 * @return
	 */
	public static String translateMessage(String code, String ... params) {
		List<String> list = MessageUtil.params(params);
		return MessageUtil.getMessage(code, code, list);
	}

	/**
	 * 데이터가 존재하지 않는 경우 메시지
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static String notFoundRecordMsg(String type, String data) {
		List<String> params = ThrowUtil.toTypeDataParams(type, data);
		return MessageUtil.getMessage(SysMessageConstants.NOT_FOUND, "{0}({1}) not found.", params);
	}
	
	/**
	 * 데이터가 존재하지 않는 경우 메시지
	 * 
	 * @param type
	 * @return
	 */
	public static String notFoundRecordMsg(String type) {
		String termType = MessageUtil.getTerm(type, type);
		List<String> params = SysValueUtil.toList(SysConstants.EMPTY_STRING, termType);
		return MessageUtil.getMessage(SysMessageConstants.NOT_FOUND, "{0}({1}) not found.", params);
	}
	
	/**
	 * key가 존재하지 않는 경우 발생하는 예외
	 * 	- 키 파라미터에서 빈 키를 발견했습니다.
	 * 	- Empty key found at key parameters
	 * 
	 * @return
	 */
	public static ElidomInvalidParamsException newNotFoundKey() {
		return new ElidomInvalidParamsException(SysMessageConstants.NOT_FOUND_KEYS, "Empty key found at key parameters");
	}
	
	/**
	 * key가 유효하지 않은 경우 발생하는 예외
	 * 	- 엔티티의 키가 올바르지 않습니다.
	 * 	- Invalid keys of entity!
	 * 
	 * @param keys
	 * @return
	 */
	public static ElidomInvalidParamsException newInvalidKey(Object... keys) {
		String[] keyStrs = new String[keys.length];
		for(int i = 0 ; i < keys.length ; i++) {
			keyStrs[i] = keys[i].toString();
		}
		
		return new ElidomInvalidParamsException(SysMessageConstants.INVALID_KEYS, "Invalid keys of entity!", MessageUtil.params(keyStrs));
	}
	
	/**
	 * 파일이 유효하지 않은 경우 발생하는 예외 
	 * 	- [{0}] (은)는 유효한 파일이 아닙니다.
	 * 	- Invalid file [{0}]
	 * 
	 * @param filename
	 * @param th
	 * @return
	 */
	public static ElidomInvalidParamsException newInvalidFile(String filename, Throwable th) {
		return new ElidomInvalidParamsException(SysMessageConstants.INVALID_FILE, "Invalid file [{0}]", SysValueUtil.toList(filename), th);
	}
	
	/**
	 * 엔티티에 cudFlag 메소드가 없는 경우 발생하는 예외 
	 * 	- Entity[{0}] 중 'cudFlag가 존재하지 않습니다.
	 * 	- There is no 'cudFlag' in the entity class [{0}]
	 * 
	 * @param className
	 * @return
	 */
	public static ElidomInvalidParamsException newNotFoundCudFlagMethod(String className) {
		return new ElidomInvalidParamsException(SysMessageConstants.NOT_FOUND_CUD_FLAG, "There is no 'cudFlag' in the entity class [{0}]", MessageUtil.params(className));
	}
	
	/**
	 * 엔티티에 cudFlag 값이 없는 경우 발생하는 예외 
	 * 	- cudFlag의 값을 가져오는데 실패하였습니다.
	 * 	- Failed to get cudFlag value
	 * 
	 * @return
	 */
	public static ElidomInvalidParamsException newNotFoundCudFlagValue() {
		return new ElidomInvalidParamsException(SysMessageConstants.EMPTY_CUD_FLAG_VALUE, "Failed to get cudFlag value");
	}
	
	/**
	 * ClassName이 존재하지 않는 경우 발생하는 예외 
	 * 	- 클래스 ({1}) 을(를) 찾을수 없습니다.
	 * 	- Class ({1}) not found.
	 * 
	 * @param className
	 * @return
	 */
	public static ElidomValidationException newNotFoundClass(String className) {
		return new ElidomValidationException(SysMessageConstants.NOT_FOUND, "{0} ({1}) not found.", MessageUtil.params("terms.label.class", className));
	}
	
	/**
	 * Class를 instantiate 할 때 실패한 경우 발생하는 예외 
	 * 	- {0}은(는) 인스턴스화 할수 없습니다. ({1})
	 * 	- {0} could not be instantiated ({1})
	 * 
	 * @param className
	 * @return
	 */
	public static ElidomValidationException newNotInstantiated(String className, Throwable th) {
		return new ElidomValidationException(SysMessageConstants.COULD_NOT_INSTANTIATE, "{0} could not be instantiated ({1})", MessageUtil.params("terms.label.class", className), th);
	}
	
	/**
	 * class가 mustBeClassName이 아닌 경우 발생하는 예외 
	 * 	- 유효하지 않은 {0} 클래스 ({1}) - 클래스가 {2}의 구현체가 아닙니다.
	 * 	- Invalid {0} Class ({1}) - Class is not instance of {2}.
	 * 
	 * @param className
	 * @param mustBeClassName
	 * @return
	 */
	public static ElidomValidationException newHandlerIsNotAInstanceOf(String className, String mustBeClassName) {
		return new ElidomValidationException(SysMessageConstants.IS_NOT_INSTANCE_OF, "Invalid {0} Class ({1}) - Class is not a instance of {2}.", MessageUtil.params("terms.label.handler", className, mustBeClassName));
	}
	
	/**
	 * setFieldValue 메소드 호출 실패시 발생하는 예외
	 * 	- 클래스 ({0})의 필드({1})에 값 설정 실패
	 * 	- Failed to assign value to field({1}) of class ({0})
	 * 
	 * @param className
	 * @param fieldName
	 * @return
	 */
	public static ElidomServiceException newFailToSetFieldValue(String className, String fieldName) {
		return new ElidomServiceException(SysMessageConstants.FAIL_TO_REFLECT_SET, "Failed to assign value to field({1}) of class ({0})", SysValueUtil.toList(className, fieldName));
	}
	
	/**
	 * getFieldValue 메소드 호출 실패시 발생하는 예외
	 * 	- 클래스 ({0})의 필드({1})에 값 추출 실패
	 * 	- Failed to get value from field({1}) of class ({0})
	 * 
	 * @param className
	 * @param fieldName
	 * @return
	 */
	public static ElidomServiceException newFailToGetFieldValue(String className, String fieldName) {
		return new ElidomServiceException(SysMessageConstants.FAIL_TO_REFLECT_GET, "Failed to assign value from field({1}) of class ({0})", SysValueUtil.toList(className, fieldName));
	}
	
	/**
	 * 엔티티의 ID Type이 유효하지 않은 경우 발생하는 예외 
	 * 	- 엔티티 ({0})의 ID Type ({1})이 유효하지 않습니다.
	 * 	- Invalid ID Type ({1}) of entity ({0})
	 * 
	 * @param entityName
	 * @param idType
	 * @return
	 */
	public static ElidomValidationException newInvalidEntityIdType(String entityName, String idType) {
		return new ElidomValidationException(SysMessageConstants.INVALID_ENTITY_ID_TYPE, "Invalid ID Type ({1}) of entity ({0})", SysValueUtil.toList(entityName, idType));
	}
	
	/**
	 * 엔티티의 ID Type의 데이터 타입이 유효하지 않은 경우 발생하는 예외 
	 * 	- 엔티티 ({0})의 ID Type ({1})의 데이터 타입은 반드시 ({2}) 이어야 합니다.
	 * 	- Data type must be ({2}) of ID Type ({1}) of entity ({0})
	 * 
	 * @param entityName
	 * @param idType
	 * @param dataType
	 * @return
	 */
	public static ElidomValidationException newInvalidDataTypeOfEntityIdType(String entityName, String idType, String dataType) {
		return new ElidomValidationException(SysMessageConstants.INVALID_DATA_TYPE_OF_ENTITY_ID_TYPE, "Data type must be ({2}) of ID Type ({1}) of entity ({0})", SysValueUtil.toList(entityName, idType, dataType));
	}
	
	/**
	 * 엔티티에 유니크 필드 정보가 존재하지 않은 경우 발생하는 예외 
	 * 	- 엔티티 ({0})에 uniqueFields 정보가 존재하지 않습니다.
	 * 	- Not found uniqueFields in entity ({0})
	 * 
	 * @param entityName
	 * @return
	 */
	public static ElidomValidationException newNotFoundUniqueFieldInEntity(String entityName) {
		return new ElidomValidationException(SysMessageConstants.NOT_FOUND_UNIQUE_FIELDS, "Not found uniqueFields in entity ({0})", SysValueUtil.toList(entityName));
	}
	
	/**
	 * 엔티티에 Detail Object가 존재하여 삭제할 수가 없는 경우 발생하는 예외 
	 * 	- 엔티티 ({0})에 Detail Data가 존재합니다. Detail 데이터 삭제 후 다시 시도해주세요.
	 * 	- There are detail data in entity ({0}), Please re-try after deleting detail data.
	 * 
	 * @param entityName
	 * @return
	 */
	public static ElidomServiceException newCannotDeleteCauseDetailExist(String entityName) {
		return new ElidomServiceException(SysMessageConstants.HAS_DETAIL_DATA, "There are detail data in entity ({0}), Please re-try after deleting detail data.", SysValueUtil.toList(entityName));
	}
	
	/**
	 * Detail data 삭제시에 발생하는 예외 
	 * 
	 * @param detailEntityName
	 * @param e
	 * @return
	 */
	public static ElidomServiceException newFailToDeleteDetailEntityData(String detailEntityName, Exception e) {
		// TODO 다국어 적용
		return new ElidomServiceException("Failed to delete detail entity [" + detailEntityName + "] - " + e.getMessage());
	}
	
	/**
	 * 비활성화된 계정을 가진 사용자가 로그인 시도를 했을 때의 예외 
	 * 	- 비활성화된 계정입니다.
	 * 	- Deactivated account.
	 * 
	 * @return
	 */
	public static ElidomValidationException newDeactivatedAccount() {
		return new ElidomValidationException(SysMessageConstants.USER_INACTIVATED_ACCOUNT, "Inactive account.");
	}
	
	/**
	 * 요청한 서버 정보로 부터 도메인 정보를 찾을 수 없는 경우 예외 
	 * 	- 요청한 서버 정보 ({0})로 Domain 정보를 찾을 수 없습니다.
	 * 	- Cannot find Domain information by request server ({0})
	 * 
	 * @param requestServerName
	 * @return
	 */
	public static ElidomValidationException newDomainNotExist(String requestServerName) {
		return new ElidomValidationException(SysMessageConstants.DOMAIN_NOT_EXIST, SysValueUtil.toList(requestServerName));
	}
	
	/**
	 * 시스템 도메인 삭제 시도시 예외 발생 
	 * 	- 도메인 ({0})는 시스템 도메인이므로 삭제할 수 없습니다.
	 * 	- Domain ({0}) is System Domain, so it's not possible to delete!
	 * 
	 * @param domainName
	 * @return
	 */
	public static ElidomValidationException newCannotDeleteSystemDomain(String domainName) {
		return new ElidomValidationException(SysMessageConstants.SYSTEM_DOMAIN_CANNOT_BE_DELETED, MessageUtil.params(domainName));
	}
	
	/**
	 * 개발 모드가 아닌 경우에 위험한 실행 요청을 받은 경우 예외   
	 * 	- 개발 모드가 아닌 경우 이 요청은 실행될 수 없습니다. 
	 * 	- This request is allowed only Development Mode.
	 * 
	 * @return
	 */
	public static ElidomValidationException newNotSupportFunctionOnlyDevMode() {
		return new ElidomValidationException(SysMessageConstants.DEV_MODE_FUNCTION_NOT_SUPPORTED);
	}
	
	/**
	 * 중복된 데이터가 존재하는 경우 예외 
	 *  - ({0}) 데이터 ({1})은 이미 존재 합니다.
	 *  - ({0)) Data ({1}) already exist.
	 * 
	 * @return
	 */
	public static ElidomValidationException newDataDuplicated(String type, String data) {
		List<String> params = ThrowUtil.toTypeDataParams(type, data);
		return new ElidomValidationException(SysMessageConstants.DATA_DUPLICATED, params);
	}
	
	/**
	 * 데이터가 존재하지 않는 경우 예외 
	 * 	- ({1}) 을(를) 찾을수 없습니다.
	 * 	- ({1}) not found.
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static ElidomRuntimeException newNotFoundRuntimeException(String type) {
		String termType = MessageUtil.getTerm(type, type);
		List<String> params = SysValueUtil.toList(SysConstants.EMPTY_STRING, termType);
		String msg = MessageUtil.getMessage(SysMessageConstants.NOT_FOUND, "{0}({1}) not found.", params);
		return new ElidomRuntimeException(msg);
	}
	
	/**
	 * 데이터가 존재하지 않는 경우 예외 
	 * 	- {0}({1}) 을(를) 찾을수 없습니다.
	 * 	- {0}({1}) not found.
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static ElidomRuntimeException newNotFoundRuntimeException(String type, String data) {
		List<String> params = ThrowUtil.toTypeDataParams(type, data);
		String msg = MessageUtil.getMessage(SysMessageConstants.NOT_FOUND, "{0}({1}) not found.", params);
		return new ElidomRuntimeException(msg);
	}
	
	/**
	 * 데이터가 존재하지 않는 경우 예외 
	 * 	- ({1}) 을(를) 찾을수 없습니다.
	 * 	- ({1}) not found.
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static ElidomRecordNotFoundException newNotFoundRecord(String type) {
		String termType = MessageUtil.getTerm(type, type);
		List<String> params = SysValueUtil.toList(SysConstants.EMPTY_STRING, termType);
		String msg = MessageUtil.getMessage(SysMessageConstants.NOT_FOUND, "{0}({1}) not found.", params);
		return new ElidomRecordNotFoundException(msg);
	}
	
	/**
	 * 데이터가 존재하지 않는 경우 예외 
	 * 	- {0}({1}) 을(를) 찾을수 없습니다.
	 * 	- {0}({1}) not found.
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public static ElidomRecordNotFoundException newNotFoundRecord(String type, String data) {
		List<String> params = ThrowUtil.toTypeDataParams(type, data);
		String msg = MessageUtil.getMessage(SysMessageConstants.NOT_FOUND, "{0}({1}) not found.", params);
		return new ElidomRecordNotFoundException(msg);
	}
	
	/**
	 * 데이터가 존재하지 않는 경우 예외 
	 * 	- 스캔한 {0}로 {1}(을)를 찾을 수 없습니다.
	 * 	- Can't find {1} by scanned {0}
	 * 
	 * @param type
	 * @param barcodeType
	 * @return
	 */
	public static ElidomRecordNotFoundException newNotFoundByScanBarcode(String type, String barcodeType) {
		List<String> params = ThrowUtil.toParamsByTranslation(type, barcodeType);
		String msg = MessageUtil.getMessage("NOT_FOUND_BY_SCAN_BARCD", "Cannot find {1} by scanned {0}", params);
		return new ElidomRecordNotFoundException(msg);
	}
	
	/**
	 * 첨부파일 Root Path가 존재하지 않는 경우 예외 
	 * 	- 파라미터 {0}은 빈 값을 허용하지 않습니다. 
	 * 	- Empty {0} is not allowed!
	 * 
	 * @return
	 */
	public static ElidomServiceException newEmptyRootPath() {
		return new ElidomServiceException(SysMessageConstants.EMPTY_PARAM, MessageUtil.params("terms.label.storage_root"));
	}
	
	/**
	 * 빈 {info} 값은 허용하지 않습니다. 
	 * 	- 파라미터 {info}은 빈 값을 허용하지 않습니다. 
	 * 	- Empty {info} is not allowed!
	 * 
	 * @return
	 */
	public static ElidomValidationException newNotAllowedEmptyInfo(String info) {
		return new ElidomValidationException(SysMessageConstants.EMPTY_PARAM, MessageUtil.params(info));
	}
	
	/**
	 * 첨부파일 Root Path가 올바르지 않는 경우 예외 
	 * 	- Storage Root의 데이터({rootPath})가 유효하지 않습니다.
	 * 	- Data ({rootPath}) of Storage Root Path is invalid.
	 * 
	 * @param rootPath
	 * @return
	 */
	public static ElidomServiceException newInvalidRootPath(String rootPath) {
		List<String> params = MessageUtil.params("terms.label.storage_root");
		params.add(rootPath);
		return new ElidomServiceException(SysMessageConstants.INVALID_DATA, params);
	}
	
	/**
	 * Map 생성시 Key와 Value쌍의 개수가 맞지 않은 경우 예외 
	 * 	- keys count and values count mismatch!
	 * 	- 키 카운트와 벨류 카운트가 일치하지 않습니다.
	 * 
	 * @return
	 */
	public static ElidomServiceException newMismatchMapKeyValue() {
		return new ElidomServiceException(SysMessageConstants.KEY_AND_VALUE_MISMATCH, "keys count and values count mismatch!");
	}
	
	/**
	 * Data 복사에 실패한 경우 예외
	 * 	- Data 복사 하는 중 에러가 발생했습니다.
	 * 	- Failed to clone data.
	 * 
	 * @return
	 */
	public static ElidomServiceException newFailToCloneData() {
		return new ElidomServiceException(SysMessageConstants.FAIL_TO_CLONE_DATA, "Failed to clone data.");
	}
	
	/**
	 * 파일 읽기 도중 에러가 발생하는 경우 예외 
	 * 	- File {filename} 읽기 중 에러가 발생하였습니다.
	 * 	- Failed to read file {filename}.
	 * 
	 * @param filename
	 * @return
	 */
	public static ElidomServiceException newFailToReadFileContent(String filename) {
		return new ElidomServiceException(SysMessageConstants.READ_FILE_ERROR, SysValueUtil.toList(filename));
	}
	
	/**
	 * 파일에 존재하지 않는 경우 예외 
	 * 	- 파일({filename})을 찾을수가 없습니다.
	 * 	- File({filename}) Not Found.
	 * 
	 * @param filename
	 * @return
	 */	
	public static ElidomServiceException newNotFoundFile(String filename) {
		return new ElidomServiceException(SysMessageConstants.FILE_NOT_FOUND, SysValueUtil.toList(filename));
	}
	
	/**
	 * 파일 생성 중 발생하는 예외 
	 * 	- File 생성 중 에러가 발생하였습니다.
	 * 	- Error occured during file creating.
	 * 
	 * @return
	 */
	public static ElidomServiceException newFailToCreateFile() {
		return new ElidomServiceException(SysMessageConstants.CREATE_FILE_ERROR, "An error occurred while creating the file!");
	}
	
	/**
	 * 파일 복사 중 발생하는 예외 
	 * 	- 파일 복사에 실패하였습니다.
	 * 	- Failed to copy file!
	 * 
	 * @return
	 */
	public static ElidomServiceException newFailToCopyFile() {
		return new ElidomServiceException(SysMessageConstants.FILE_COPY_FAILED, "Failed to copy file!");
	}
	
	/**
	 * 데이터소스 이름으로 데이터소스를 찾지 못한 경우 예외   
	 * 	- Datasource ({datasourceName}) 을(를) 찾을수 없습니다.
	 * 	- Datasource ({datasourceName}) not found.
	 * 
	 * @return
	 */
	public static ElidomValidationException newNotFoundDatasource(String datasourceName) {
		List<String> params = MessageUtil.params("terms.label.datasource");
		params.add(datasourceName);
		return new ElidomValidationException(SysMessageConstants.NOT_FOUND, params);
	}
	
	/**
	 * 서비스 URL이 유효하지 못한 경우 예외 
	 * 	- Restful Service URL({serviceUrl}) 이 올바르지 않습니다.
	 * 	- Invalid Restful Service URL({serviceUrl}).
	 * 
	 * @param serviceUrl
	 * @return
	 */
	public static ElidomServiceNotFoundException newInvalidServiceUrl(String serviceUrl) {
		return new ElidomServiceNotFoundException(SysMessageConstants.INVALID_SERVICE_URL, "Invalid Restful Service URL({0}).", SysValueUtil.toList(serviceUrl));
	}
	
	/**
	 * 서비스를 찾지 못한 경우 예외 
	 * 	- URL({serviceUrl})에서 서비스를 찾을 수 없습니다.
	 * 	- Service Not Found By URL({serviceUrl}).
	 * 
	 * @param serviceUrl
	 * @return
	 */
	public static ElidomServiceNotFoundException newNotFoundService(String serviceUrl) {
		return new ElidomServiceNotFoundException(SysMessageConstants.NOT_FOUND_URL, "Service Not Found By URL({0}).", SysValueUtil.toList(serviceUrl));
	}
	
	/**
	 * 현재 지원하지 않은 메소드인 경우 예외 
	 * 	- 지원되지 않는 기능입니다.
	 * 	- Not support method
	 * 
	 * @return
	 */
	public static ElidomServiceNotFoundException newNotSupportedMethodYet() {
		return new ElidomServiceNotFoundException(SysMessageConstants.NOT_SUPPORTED_METHOD, "Not supported method");
	}
	
	/**
	 * 템플릿 실행 중 발생하는 예외
	 * 	- Failed to Process {templateName} Template
	 * 	- {templateName} 템플릿 프로세스를 실패하였습니다.
	 * 
	 * @return
	 */	
	public static ElidomServiceException newFailToProcessTemplate(String templateName, Throwable th) {
		return new ElidomServiceException(SysMessageConstants.FAIL_TO_PROCESS_TEMPLATE, "Failed to Process {0} Template.", SysValueUtil.toList(templateName), th);
	}
	
	/**
	 * 코드 컴파일 중 발생하는 예외
	 * 	- Failed to Compile {code} Code
	 * 	- {code} 코드 컴파일 중 에러가 발생했습니다.
	 * 
	 * @return
	 */	
	public static ElidomServiceException newFailToCompileCode(String code, Throwable th) {
		return new ElidomServiceException(SysMessageConstants.FAIL_TO_COMPILE, "Failed to Compile {0} Code.", SysValueUtil.toList(code), th);
	}
	
	/**
	 * 데이터 파싱 중 발생하는 예외
	 * 	- Failed to parse {code} code
	 * 	- {code} 데이터 파싱 중 에러가 발생했습니다.
	 * 
	 * @return
	 */	
	public static ElidomServiceException newFailToParseCode(String code, Throwable th) {
		return new ElidomServiceException(SysMessageConstants.FAIL_TO_PARSE, "Failed to parse {0} data.", SysValueUtil.toList(code), th);
	}
	
	/**
	 * 뭔가 하는 도중에 어떤 예외가 발생하는 경우  
	 * 	- {errorType} Error occurred when doing {doSomething}
	 * 	- {doSomething} 하던 도중에 {errorType} 에러가 발생했습니다.
	 * 
	 * @return
	 */	
	public static ElidomServiceException newErrorWhenDoingSomething(String errorType, String doSomething, Throwable th) {
		return new ElidomServiceException(SysMessageConstants.ERROR_WHEN_DO_SOMETHING, "{0} Error occurred when doing {1}", MessageUtil.params(errorType, doSomething), th);
	}
	
	/**
	 * 스크립트 엔진 실행시 발생하는 예외  
	 * 	- Script Error occurred when doing Logic
	 * 	- Logic 실행 도중에 Script 에러가 발생했습니다.
	 * 
	 * @return
	 */	
	public static ElidomScriptRuntimeException newFailToRunScript(Throwable th) {
		return new ElidomScriptRuntimeException(SysMessageConstants.ERROR_WHEN_DO_SOMETHING, "{0} Error occurred when doing {1}", MessageUtil.params("terms.label.logic", "Script"), th);
	}
	
	/**
	 * 현재 지원하지 않은 {0}입니다.
	 * 	- 지원하지 않는 {0}입니다.
	 * 	- Not Supported {0}
	 * 
	 * @param type
	 * @return
	 */
	public static ElidomValidationException newNotSupportedSomething(String type) {
		return new ElidomValidationException("NOT_SUPPORTED_A", "Not Supported {0}", MessageUtil.params(type));
	}
	
	/**
	 * 지원하지 않는 기능입니다.
	 * 	- 지원하지 않는 기능입니다.
	 * 	- Not supported method.
	 * 
	 * @param type
	 * @return
	 */
	public static ElidomValidationException newNotSupportedMethod() {
		return new ElidomValidationException("NOT_SUPPORTED_METHOD", "Not supported method.");
	}

	/**
	 * 상태가 유효하지 않은 경우 발생하는 예외  
	 * 	- {type} ({data})은(는) [{status}] 상태가 아닙니다.
	 * 	- The {0} ({1}) is not [{2}] Status.
	 * 
	 * @param type
	 * @param data
	 * @param status
	 * @return
	 */
	public static ElidomValidationException newInvalidStatus(String type, String data, String status) {
		List<String> params = MessageUtil.params(type);
		params.add(data);
		params.add(status);
		return new ElidomValidationException(SysMessageConstants.MISMATCH_STATUS, "The {0} ({1}) is not [{2}] Status.", params);
	}
	
	/**
	 * 에러 로깅 없이 상태가 유효하지 않은 경우 발생하는 예외 
	 * 
	 * @param errorMsg
	 * @return
	 */
	public static ElidomValidationException newInvalidStatusWithNoLog(String type, String data, String status) {
		// 에러 로깅을 하지 않게 Exception 생성 - Setting에서 설정할 수 있게 변경
		//TODO
//		boolean withLogging = ValueUtil.toBoolean(SettingUtil.getValue(ConfigConstants.LOG_VALIDATION_ERROR_ENABLED, SysConstants.TRUE_STRING));
		ElidomValidationException eve = newInvalidStatus(type,data,status);
//		eve.setWritable(withLogging);
		throw eve;
	}
	
	/**
	 * A가 유효하지 않습니다.  
	 * 	- {0}이(가) 유효하지 않습니다.
	 * 	- {0} is invalid.
	 * 
	 * @param type
	 * @return
	 */
	public static ElidomValidationException newAIsInvalid(String type) {
		List<String> params = MessageUtil.params(type);
		return new ElidomValidationException("A_IS_INVALID", "{0} is invalid.", params);
	}
	
	/**
	 * {0} 은(는) 이미 {1} 상태입니다.
	 * 	- {0} 상태가 {1} 입니다.
	 * 	- The {0} is already {1} status.
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public static ElidomValidationException newAlreadyBstatus(String type, String status) {
		return new ElidomValidationException("A_ALREADY_B_STATUS", "{0} is already {1} status.", MessageUtil.params(type, status));
	}
	
	/**
	 * A 상태가 B 입니다. 
	 * 	- {0} 상태가 {1} 입니다.
	 * 	- Status of {0} is {1}.
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public static ElidomValidationException newAStatusIsB(String type, String status) {
		return new ElidomValidationException("A_STATUS_IS_B", "Status of {0} is {1}.", MessageUtil.params(type, status));
	}
	
	/**
	 * A 상태가 비활성화 상태입니다.
	 * 	- {0}이(가) 비활성화 상태입니다.
	 * 	- {0} is not active.
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public static ElidomValidationException newAIsNotActive(String type) {
		return new ElidomValidationException("A_IS_NOT_ACTIVE", "{0} is not active.", MessageUtil.params(type));
	}
	
	/**
	 * A 가 충분치 않습니다.
	 * 	- {0}(이)가 충분치 않습니다.
	 * 	- {0} is not sufficient
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public static ElidomValidationException newAIsNotSufficient(String type) {
		return new ElidomValidationException("A_IS_NOT_SUFFICIENT", "{0} is not sufficient", MessageUtil.params(type));
	}
	
	/**
	 * a는 b가 불가능합니다. 
	 * 	- {0}는 {1}이(가) 불가능합니다.
	 * 	- {0} is not possible to {1}
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public static ElidomValidationException newNotPossibleTo(String subject, String verb) {
		return new ElidomValidationException("A_IS_NOT_POSSIBLE_TO", "{0} is not possible to {1}", MessageUtil.params(subject, verb));
	}
	
	/**
	 * a는 b가 불가능합니다. 
	 * 	- {0}상태인 경우에만 {1}이 가능합니다.
	 * 	- Only {1} is possible in {0} state.
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public static ElidomValidationException newOnlyPossibleState(String subject, String verb) {
		return new ElidomValidationException("A_IS_POSSIBLE_ONLY_B_STATUS", "Only {1} is possible in {0} state.", MessageUtil.params(subject, verb));
	}
	
	/**
	 * a와 b가 일치하지 않는 경우 발생하는 예외 
	 * 	- {0}와(과) {1}이(가) 맞지 않습니다.
	 * 	- {0} is not equal {1}.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static ElidomValidationException newAIsNotEqualB(String a, String b) {
		return new ElidomValidationException(SysMessageConstants.A_IS_NOT_EQUAL_B, "{0} is not equal {1}.", MessageUtil.params(a, b));
	}
	
	/**
	 * 상태가 진행 중 이 아닌 경우 발생하는 예외 
	 *  - 진행 중인 {type}이(가) 아닙니다.
	 *  - {type} is not on the process.
	 * 
	 * @param type
	 * @return
	 */
	public static ElidomValidationException newStatusIsNotIng(String type) {
		return new ElidomValidationException(SysMessageConstants.DOES_NOT_PROCEED, "{0} is not on the process.", MessageUtil.params(type));
	}
	
	/**
	 * 이미 사용되었는데 다시 사용하려고 할 때 발생하는 예외.
	 * 	- {0}이(가) 이미 사용되었습니다.
	 * 	- {0} is already used.
	 * 
	 * @param request
	 * @return
	 */
	public static ElidomValidationException newAlreadyUsed(String type) {
		return new ElidomValidationException("ALREADY_USED", "{0} is already used.", MessageUtil.params(type));
	}
	
	/**
	 * 요청 접수가 이미 되어 있는데 다시 요청하는 경우 발생하는 예외 
	 * 	- 이미 접수된 요청입니다.
	 * 	- This request already has been received.
	 * 
	 * @return
	 */
	public static ElidomBadRequestException newAlreadyReceivedRequest() {
		return new ElidomBadRequestException(SysMessageConstants.REQUEST_ALREADY_RECEIVED, "This request already has been received.");
	}
	
	/**
	 * 인증되지 않은 사용자입니다.
	 * 
	 * @return
	 */
	public static ElidomUnauthorizedException unAuthorizedAccount() {
		return new ElidomUnauthorizedException(SysMessageConstants.NOT_AUTHORIZED_USER, "Unauthorized User.");
	}
	
	/**
	 * 계정 또는 비밀번호가 올바르지 않습니다.
	 * 
	 * @return
	 */
	public static ElidomUnauthorizedException invalidIdOrPass() {
		return new ElidomUnauthorizedException(SysMessageConstants.USER_INVALID_ID_OR_PASS, "ID or Password is incorrect.");
	}
	
	/**
	 * 권한이 이미 있는데 다시 요청하는 경우 발생하는 예외  
	 * 	- 요청하신 권한은 이미 신청자님께서 가지고 있습니다.
	 * 	- You already have the authorization. 
	 * 
	 * @return
	 */
	public static ElidomBadRequestException newAlreadyHaveAuthroization() {
		return new ElidomBadRequestException(SysMessageConstants.ALREADY_HAVE_AUTHROIZATION, "You already have the authorization.");
	}
	
	/**
	 * 요청이 이미 처리되었는데 다시 처리하려고 할 때 발생하는 예외.
	 * 	- {request}은 이미 처리되었습니다.
	 * 	- {request} has been already finished.
	 * 
	 * @param request
	 * @return
	 */
	public static ElidomBadRequestException newAlreadyProcessedRequest(String request) {
		return new ElidomBadRequestException(SysMessageConstants.ALREADY_FINISHED, "{0} has already finished.", MessageUtil.params(request));
	}

	/**
	 * 이미 활성화되어 있는 사용자입니다.
	 * 	- 이미 활성화되어 있는 사용자입니다.
	 * 	- Already activated account.
	 * 
	 * @return
	 */
	public static ElidomBadRequestException newAlreadyActivatedAccount() {
		return new ElidomBadRequestException(SysMessageConstants.USER_ALREADY_ACTIVATED, "Already activated account.");
	}
	
	/**
	 * 이미 비활성화되어 있는 사용자입니다.
	 * 	- 비활성화된 계정입니다.
	 * 	- Deactivated account.
	 * 
	 * @return
	 */
	public static ElidomBadRequestException newAlreadyDeactivatedAccount() {
		return new ElidomBadRequestException(SysMessageConstants.USER_INACTIVATED_ACCOUNT, "Already deactivated account.");
	}
	
	/**
	 * 사용자께서는 {0}-{1}에 대한 접근 권한이 없습니다.
	 * 	- 사용자께서는 {0}-{1}에 대한 접근 권한이 없습니다.
	 * 	- You have no authroity to access {0}-{1}
	 * 
	 * @return
	 */
	public static ElidomBadRequestException newHasNoAuthority(String resourceType, String resourceName) {
		List<String> params = MessageUtil.params(resourceType);
		params.add(resourceName);
		return new ElidomBadRequestException(SysMessageConstants.HAS_NO_AUTHORITY, "You have no authroity to access {0}-{1}", params);
	}
	
	/**
	 * 지원하지 않은 작업 유형입니다.
	 * Not Supported Job Type
	 * 
	 * @return
	 */
	public static ElidomValidationException newJobTypeNotSupported() {
		String msg = MessageUtil.getMessage("NOT_SUPPORTED_A", "Not Supported {0}", MessageUtil.params("terms.label.job_type"));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 투입한 상품으로 처리할 작업이 없습니다
	 * No jobs to process by input sku.
	 * 
	 * @return
	 */
	public static ElidomValidationException newNoJobsWithInput() {
		throw new ElidomValidationException(MessageUtil.getMessage("NO_JOBS_TO_PROCESS_BY_INPUT", "No jobs to process with input sku."));
	}
	
	/**
	 * {0} 단위 상품 투입이 불가능합니다
	 * Can not input items by {0} unit.
	 * 
	 * @param unit
	 * @return
	 */
	public static ElidomValidationException newNotAllowedInputByUnit(String unit) {
		String msg = MessageUtil.getMessage("NOT_ALLOWED_INPUT_BY_UNIT", "Can not input items by {0} unit.", MessageUtil.params(unit));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 유효하지 않은 파라미터 에러. 
	 * 
	 * @return
	 */
	public static ElidomValidationException newInvalidParameters() {
		String msg = MessageUtil.getMessage("TITLE_INVALID_PARAM", "Invalid parameter error.");
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 배치에 작업 설정 프로파일이 설정되지 않은 에러
	 * 
	 * @return
	 */
	public static ElidomValidationException newJobConfigNotSet() {
		String msg = MessageUtil.getMessage("BATCH_JOB_CONFIG_IS_NOT_SET", "The job config set is not set in the batch.");
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 배치에 표시기 설정 프로파일이 설정되지 않은 에러
	 * 
	 * @return
	 */
	public static ElidomValidationException newIndConfigNotSet() {
		String msg = MessageUtil.getMessage("BATCH_IND_CONFIG_IS_NOT_SET", "The indicator config set is not set in the batch.");
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 작업 설정 프로파일이 설정되지 않은 에러
	 * 
	 * @param configKey
	 * @return
	 */
	public static ElidomValidationException newJobConfigNotSet(String configSetCd, String configKey) {
		String msg = MessageUtil.getMessage("JOB_CONFIG_IS_NOT_SET", "The setting [{1}] is not set in the job config set [{0}].", SysValueUtil.toList(configSetCd, configKey));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 표시기 설정 프로파일이 설정되지 않은 에러
	 * 
	 * @param configKey
	 * @return
	 */
	public static ElidomValidationException newIndConfigNotSet(String configKey) {
		String msg = MessageUtil.getMessage("IND_CONFIG_IS_NOT_SET", "The setting [{1}] is not set in the indicator config set [{0}].", SysValueUtil.toList("", configKey));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 장비 설정 프로파일이 설정되지 않은 에러
	 * 
	 * @param configKey
	 * @return
	 */
	public static ElidomValidationException newDeviceConfigNotSet(String configKey) {
		String msg = MessageUtil.getMessage("DEVICE_CONFIG_IS_NOT_SET", "The setting [{1}] is not set in the device config set [{0}].", SysValueUtil.toList("", configKey));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 작업 설정이 설정되지 않은 에러
	 * 
	 * @param configKey
	 * @return
	 */
	public static ElidomValidationException newJobConfigNotSet(String configKey) {
		String msg = MessageUtil.getMessage("JOB_CONFIG_IS_NOT_SET", "The setting [{1}] is not set in the job config profile [{0}].", SysValueUtil.toList("", configKey));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 표시기 설정이 설정되지 않은 에러
	 * 
	 * @param configSetName
	 * @param configKey
	 * @return
	 */
	public static ElidomValidationException newIndConfigNotSet(String configSetName, String configKey) {
		String msg = MessageUtil.getMessage("IND_CONFIG_IS_NOT_SET", "The setting [{1}] is not set in the indicator config profile [{0}].", SysValueUtil.toList(configSetName, configKey));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 장비 설정 프로파일이 설정되지 않은 에러
	 * 
	 * @param configSetName
	 * @param configKey
	 * @return
	 */
	public static ElidomValidationException newDeviceConfigNotSet(String configSetName, String configKey) {
		String msg = MessageUtil.getMessage("DEVICE_CONFIG_IS_NOT_SET", "The setting [{1}] is not set in the device config profile [{0}].", SysValueUtil.toList(configSetName, configKey));
		throw new ElidomValidationException(msg);
	}
	
	/**
	 * 에러 로깅을 하지 않고 ValidationException을 생성
	 * 
	 * @param errorMsg
	 * @return
	 */
	public static ElidomValidationException newValidationErrorWithNoLog(String errorMsg) {
		// 에러 로깅을 하지 않게 Exception 생성 - Setting에서 설정할 수 있게 변경
		//TODO
//		boolean withLogging = ValueUtil.toBoolean(SettingUtil.getValue(ConfigConstants.LOG_VALIDATION_ERROR_ENABLED, SysConstants.TRUE_STRING));
		ElidomValidationException eve = new ElidomValidationException(errorMsg);
//		eve.setWritable(withLogging);
		throw eve;
	}
	
	/**
	 * 에러 로깅을 하지 않고 ValidationException을 생성
	 * 
	 * @param errorMsg
	 * @param params
	 * @return
	 */
	public static ElidomValidationException newValidationErrorWithNoLog(String errorMsg, Object ... params) {
		// 에러 로깅을 하지 않게 Exception 생성 - Setting에서 설정할 수 있게 변경
	    throw ThrowUtil.newValidationErrorWithNoLog(MessageFormat.format(errorMsg, params));
	}
	
	/**
	 * 에러 로깅을 하지 않고 ValidationException을 생성하는데 translationFlag에 따라서 msgCode를 번역해서 에러 메시지를 생성한다.
	 * 
	 * @param translationFlag
	 * @param msgCode
	 * @return
	 */
	public static ElidomValidationException newValidationErrorWithNoLog(boolean translationFlag, String errorMsg) {
		// 에러 로깅을 하지 않게 Exception 생성 - Setting에서 설정할 수 있게 변경
		errorMsg = translationFlag ? MessageUtil.getMessage(errorMsg, errorMsg) : errorMsg;
		throw ThrowUtil.newValidationErrorWithNoLog(errorMsg);
	}
	
	/**
	 * 에러 로깅을 하지 않고 ValidationException을 생성
	 * 
	 * @param translationFlag
	 * @param msgCode
	 * @return
	 */
	public static ElidomValidationException newValidationErrorWithNoLog(ElidomException eve) {
		// 에러 로깅을 하지 않게 Exception 생성 - Setting에서 설정할 수 있게 변경
		//TODO
//		boolean withLogging = ValueUtil.toBoolean(SettingUtil.getValue(ConfigConstants.LOG_VALIDATION_ERROR_ENABLED, SysConstants.TRUE_STRING));
//		eve.setWritable(withLogging);
		throw (ElidomValidationException) eve;
	}
	
	/**
	 * 에러 로깅을 하지 않고 ValidationException을 생성하는데 msgCode를 번역해서 에러 메시지를 생성한다.
	 * 
	 * @param msgCode
	 * @param params
	 * @return
	 */
	public static ElidomValidationException newValidationErrorWithNoLog(boolean translationFlag, String errorMsg, String ... params) {
		// 에러 로깅을 하지 않게 Exception 생성 - Setting에서 설정할 수 있게 변경
		List<String> list = MessageUtil.params(params);
		errorMsg = translationFlag ? MessageUtil.getMessage(errorMsg, errorMsg) : errorMsg;
		errorMsg = MessageUtil.getMessage(errorMsg, errorMsg, list);
		throw ThrowUtil.newValidationErrorWithNoLog(errorMsg);
	}
	
	/**
	 * 에러 로깅을 하지 않고 ValidationException을 생성하는데 msgCode를 번역해서 에러 메시지를 생성한다.
	 * 
	 * @param translationFlag
	 * @param errorMsg
	 * @param params
	 * @return
	 */
	public static ElidomValidationException newValidationErrorWithNoLog(boolean translationFlag, String errorMsg, List<String> params) {
		// 에러 로깅을 하지 않게 Exception 생성 - Setting에서 설정할 수 있게 변경
		errorMsg = translationFlag ? MessageUtil.getMessage(errorMsg, errorMsg) : errorMsg;
		errorMsg = MessageUtil.getMessage(errorMsg, errorMsg, params);
		throw ThrowUtil.newValidationErrorWithNoLog(errorMsg);
	}
	
	/**
	 * type, datum으로 파라미터를 추가
	 * 
	 * @param type
	 * @param datum
	 * @return
	 */
	public static List<String> toTypeDataParams(String type, String ... datum) {
		String termType = MessageUtil.getTerm(type, type);
		List<String> params = SysValueUtil.toList(termType);
		
		if(datum != null && datum.length > 0) {
			for(String data : datum) {
				params.add(data);
			}
		}
		
		return params;
	}
	
	/**
	 * type, datum으로 파라미터를 추가
	 * 
	 * @param type
	 * @param datum
	 * @return
	 */
	public static List<String> toParamsByTranslation(String ... msgParams) {
		List<String> params = new ArrayList<String>(msgParams.length);
		
		for(String data : msgParams) {
			params.add(MessageUtil.getTerm(data, data));
		}
		
		return params;
	}
}
