/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.core.system.controller.handler.file.download;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.core.CoreMessageConstants;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.client.ElidomRecordNotFoundException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 파일 경로 기반 Download Handler
 * 
 * @author Minu.Kim
 */
@Component
public class PathBasedDownloadHandler implements IPathBasedDownloadHandler {
	
	private Logger logger = LoggerFactory.getLogger(PathBasedDownloadHandler.class);
	
	@Override
	public void validationCheck(HttpServletRequest req, HttpServletResponse res) throws ElidomClientException {
		// TODO 
	}

	@Override
	public boolean handleRequest(HttpServletRequest req, HttpServletResponse res, String filePath, String fileName) throws ElidomServiceException {
		File file = FileUtils.getFile(filePath);
		if(!file.exists()) {
			throw new ElidomRecordNotFoundException(CoreMessageConstants.FILE_NOT_FOUND, "File ({0}) Not Found", MessageUtil.params(filePath));
		}

		String name = SysValueUtil.isEmpty(fileName) ? file.getName() : fileName;
		// Setting Response Header
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/plain;charset=UTF-8");
		res.addHeader("Content-Type", "application/octet-stream");
		res.addHeader("Content-Transfer-Encoding", "binary;");
		res.addHeader("Content-Length", Long.toString(file.length()));
		res.setHeader("Pragma", "cache");
		res.setHeader("Cache-Control", "public");
		
		ServletOutputStream outStream = null;
		ByteArrayInputStream inStream = null;
		byte[] buffer = new byte[4096];
		
		try {
			res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name, "UTF-8"));
			outStream = res.getOutputStream();
			inStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));			
			int byteCount = 0;
			
			do {
				byteCount = inStream.read(buffer);
				if(byteCount == -1)
					break;
				
				outStream.write(buffer, 0, byteCount);
				outStream.flush();
			} while(true);

		} catch (Exception e) {
			throw new ElidomServiceException(CoreMessageConstants.FILE_DOWNLOAD_ERROR, "File Download 실행 중, 에러가 발생하였습니다.", e);
			
		} finally {
			this.closeStream(inStream);
			this.closeStream(outStream);
		}
		
		return true;
	}
	
	/**
	 * Close stream
	 * 
	 * @param stream
	 */
	private void closeStream(Closeable stream) {
		if (SysValueUtil.isNotEmpty(stream)) {
			try {
				stream.close();
			} catch (IOException e) {
				this.logger.error("Error When Close Stream", e);
			}
		}
	}
}