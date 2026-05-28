/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.system.controller.handler.file.download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.server.ElidomServiceException;

/**
 * File 경로 기반의 파일 다운로드 핸들러 
 * 
 * @author Minu.Kim
 */
public interface IPathBasedDownloadHandler {
	
	/**
	 * ValidationCheck
	 * 
	 * @param req
	 * @param res
	 * @throws ElidomClientException 요청이 유효하지 않다고 판단되었을 때 발생하는 예외
	 */
	public void validationCheck(HttpServletRequest req, HttpServletResponse res) throws ElidomClientException;
		
	/**
	 * File Download를 실행한다. 
	 * 
	 * @param req
	 * @param res
	 * @param filePath
	 * @return
	 * @throws ElidomServiceException
	 */
	public boolean handleRequest(HttpServletRequest req, HttpServletResponse res, String filePath, String fileName) throws ElidomServiceException;	
}