/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.transport;

import java.util.Map;

/**
 * title, from, to, content등의 내용이 있고 from에서 to로 content를 전송하거나 invoke하는 인터페이스  
 * 
 * @author shortstop
 */
public interface ISender {
	
	/**
	 * 전송 제목을 title로, 전송자 from이 대상 to로 파라미터 parameters와 옵션 options로 content 내용으로 호출 
	 *  
	 * @param title 전송 타이틀
	 * @param from 전송하는 측의 정보 예) 메일 주소, IP등 ... (optional)
	 * @param to 받는 측의 정보 예) 메일 주소, IP, Mobile Token, JMS Queue, Topic명 등 ...
	 * @param content 전송할 내용 
	 * @param paramters 전송 파라미터 Http Sender라면 파라미터
	 * @param options 전송하는 데 추가적으로 필요한 옵션 예) 메일이라면 MIME Type, attachment, HTTP Sender라면 Header 정보 등... 
	 * @return
	 */
	public Object send(String title, Object from, Object to, Object content, Map<String, ?> parameters, Map<String, ?> options);
	
}
