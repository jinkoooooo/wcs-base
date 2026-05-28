/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.system.controller.handler;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controller로 진입한 요청을 처리하는 핸들러
 * Request 핸들러는 Request로 부터 파라미터를 추출하여 수행한다.
 * 
 * @author shortstop
 */
public interface IRequestHandler {

	/**
	 * 수행로직을 수행한다.
	 * 
	 * @param req
	 * @param res
	 * @return
	 */
	public Map<String, Object> handleRequest(HttpServletRequest req, HttpServletResponse res,  Map<String, String> fileInfoMap, MultipartFile... files);	
}
