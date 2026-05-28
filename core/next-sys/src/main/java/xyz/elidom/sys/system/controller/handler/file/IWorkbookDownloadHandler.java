/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.file;

import org.apache.poi.ss.usermodel.Workbook;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.server.ElidomServiceException;

/**
 * Excel (Workbook) 파일 다운로드 핸들러
 * 
 * @author shortstop
 */
public interface IWorkbookDownloadHandler {

	/**
	 * ValidationCheck
	 * 
	 * @param req
	 * @param res
	 * @throws ElidomClientException 요청이 유효하지 않다고 판단되었을 때 발생하는 예외
	 */
	public void validationCheck(HttpServletRequest req, HttpServletResponse res) throws ElidomClientException;
		
	/**
	 * inputStream에서 읽어서 Servlet OutputStream에 쓰기 
	 * 
	 * @param req
	 * @param res
	 * @param downloadFileName
	 * @param workbook
	 * @return
	 * @throws ElidomServiceException 요청 Parameter 혹은 Body를 Input Object로 변환하던 도중 발생한 예외
	 */
	public boolean handleRequest(HttpServletRequest req, HttpServletResponse res, String downloadFileName, Workbook workbook) throws ElidomServiceException;

}
