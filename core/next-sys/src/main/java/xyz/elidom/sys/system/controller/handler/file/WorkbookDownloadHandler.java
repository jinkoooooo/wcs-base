/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.file;

import java.io.IOException;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.server.ElidomServiceException;

/**
 * Workbook 기반의 다운로드 핸들러 구현체  
 * 
 * @author shortstop
 */
@Component
public class WorkbookDownloadHandler implements IWorkbookDownloadHandler {
	
	@Override
	public void validationCheck(HttpServletRequest req, HttpServletResponse res) throws ElidomClientException {
		// TODO
	}

	@Override
	public boolean handleRequest(HttpServletRequest req, HttpServletResponse res, String fileName, Workbook workbook) throws ElidomServiceException {
		res.setContentType("application/vnd.ms-excel");
		res.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xls");        
        ServletOutputStream out = null;
        
		try {
			out = res.getOutputStream();
	        workbook.write(out);
	        
		} catch (Exception e) {
			throw new ElidomServiceException("Failed to write excel to outputstream!", e);
			
		} finally {
			if(out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					throw new ElidomServiceException("Failed to write excel to outputstream!", e);
				}
			}
		}
		
		return true;
	}
	
}
