/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.system.controller.handler.file.download;

import java.io.File;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.core.CoreMessageConstants;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.client.ElidomRecordNotFoundException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.sys.util.MessageUtil;

/**
 * IStreamingHandler 구현체
 * 
 * @author shortstop
 */
@Component
public class StreamingHandler implements IStreamingHandler {

	@Override
	public void validationCheck(HttpServletRequest req, HttpServletResponse res) throws ElidomClientException {
		// TODO
	}

	@Override
	public boolean handleRequest(HttpServletRequest req, HttpServletResponse res, String streamingFilePath) throws ElidomServiceException {
		File file = new File(streamingFilePath);

		if (!file.exists()) {
			throw new ElidomRecordNotFoundException(CoreMessageConstants.FILE_NOT_FOUND, "File ({0}) Not Found", MessageUtil.params(file.getPath()));
		}

		try {
			// 미디어 처리
			MultipartFileSender.fromFile(file).with(req).with(res).serveResource();
		} catch (Exception e) {
			// 사용자 취소 Exception 은 콘솔 출력 제외
			if (!e.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
				throw new ElidomServiceException(CoreMessageConstants.STREAMING_ERROR, "Streaming error", e);
			}
		}

		return true;
	}
}