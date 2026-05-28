/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.system.controller.handler.file.upload;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * IFileUploadHandler Abstract 구현
 * 
 * @author Minu.Kim
 */
public abstract class AbstractFileUploadHandler implements IFileUploadHandler {
	
	@Override
	public Map<String, Object> handleRequest(HttpServletRequest req, HttpServletResponse res, Map<String, String> fileInfoMap, MultipartFile... files) {
		// 1. File Validation Check. 
		this.validationCheck(req, files);
		// 2. File 저장 경로 가져오기.
		String savePath = this.getUploadDirectory(req, fileInfoMap);
		// 3. Save File Info 
		Map<String, String> uuidMap = this.saveFileInfo(savePath, fileInfoMap, files);
		// 4. Upload 실행. 
		Map<String, Object> resultMap = this.uploadFile(uuidMap, savePath, files);
		// 5. 리턴 
		return resultMap;
	}
	
}