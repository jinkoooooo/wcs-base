package xyz.elidom.sys.model;

/**
 * 가장 기본적인 응답 모델
 * 
 * @author shortstop
 */
public class BaseResponse {

	/**
	 * 성공 여부
	 */
	private boolean success;
	/**
	 * 메시지 
	 */
	private String message;
	/**
	 * 리턴 결과 
	 */
	private Object result;

	/**
	 * 생성자 1
	 */
	public BaseResponse() {
	}
	
	/**
	 * 생성자 2
	 * 
	 * @param success
	 */
	public BaseResponse(boolean success) {
		this(success, null, null);
	}
	
	/**
	 * 생성자 3
	 * 
	 * @param success
	 * @param message
	 */
	public BaseResponse(boolean success, String message) {
		this(success, message, null);
	}
	
	/**
	 * 생정자 4
	 * 
	 * @param success
	 * @param message
	 * @param result
	 */
	public BaseResponse(boolean success, String message, Object result) {
		this.success = success;
		this.message = message;
		this.result = result;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setResult(Object result) {
		this.result = result;
	}
	
	public Object getResult() {
		return this.result;
	}

}
