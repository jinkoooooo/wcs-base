/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.system.controller.handler.file.upload;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import xyz.elidom.core.system.controller.handler.IRequestHandler;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.client.ElidomInputException;
import xyz.elidom.exception.client.ElidomServiceNotFoundException;

/**
 * 파일 업로드를 위한 기본 인터페이스 정의 
 * 
 * @author shortstop
 */
public interface IFileUploadHandler extends IRequestHandler {
	/**
	 * File Size를 체크한다.
	 * 
	 * @param req
	 * @throws ElidomClientException 요청이 유효하지 않다고 판단되었을 때 발생하는 예외
	 */
	public void validationCheck(HttpServletRequest req, MultipartFile[] files) throws ElidomClientException;
	
	/**
	 * File을 저장 할, Directory Path를 가져온다. 
	 * 
	 * @param req
	 * @return
	 * @throws ElidomServiceNotFoundException 요청 URL로 부터 서비스 API를 찾지 못했을 경우 발생하는 예외 
	 */
	public String getUploadDirectory(HttpServletRequest req, Map<String, String> fileInfoMap) throws ElidomServiceNotFoundException;
	
	/**
	 * File Upload를 실행한다.
	 * 
	 * @param req
	 * @param api
	 * @return
	 * @throws ElidomInputException 요청 Parameter 혹은 Body를 Input Object로 변환하던 도중 발생한 예외
	 */
	public Map<String, Object> uploadFile(Map<String, String> uuidMap, String savePath, MultipartFile[] files) throws ElidomInputException;
	
	/**
	 * File 정보를 DB에 저장한다.
	 * 
	 * @param req
	 * @param api
	 * @return
	 * @throws ElidomInputException 요청 Parameter 혹은 Body를 Input Object로 변환하던 도중 발생한 예외
	 */
	public Map<String, String> saveFileInfo(String savePath, Map<String, String> fileInfoMap, MultipartFile[] files) throws ElidomInputException;
}