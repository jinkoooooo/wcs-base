/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.system.controller.handler.file.download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.server.ElidomServiceException;

/**
 * 동영상, 음악 등의 Streaming Handler
 * 
 * @author shortstop
 */
public interface IStreamingHandler {

	/**
	 * ValidationCheck
	 * 
	 * @param req
	 * @param res
	 * @throws ElidomClientException 요청이 유효하지 않다고 판단되었을 때 발생하는 예외
	 */
	public void validationCheck(HttpServletRequest req, HttpServletResponse res) throws ElidomClientException;
		
	/**
	 * Streaming 서비스 
	 * 
	 * @param req
	 * @param res
	 * @param streamingFilePath
	 * @return
	 * @throws ElidomServiceException 요청 Parameter 혹은 Body를 Input Object로 변환하던 도중 발생한 예외
	 */
	public boolean handleRequest(HttpServletRequest req, HttpServletResponse res, String streamingFilePath) throws ElidomServiceException;
	
}
