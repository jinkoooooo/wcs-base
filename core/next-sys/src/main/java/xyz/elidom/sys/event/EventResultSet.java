package xyz.elidom.sys.event;

/**
 * 이벤트 처리 결과 셋 
 * 
 * @author yang
 */
public class EventResultSet {
	/**
	 *  이벤트 처리 결과 - 각 이벤트에서 알아서 설정
	 */
	protected Object result;
	/**
	 * 이벤트 처리 완료 여부
	 * 이벤트 처리가 오버라이드 된 경우 기본 이벤트를 skip하기 위한 flag
	 */
	protected boolean isExecuted;
	
	/**
	 * 다음 이벤트를 계속 발생 할건지 여부, DEFAULT => false
	 */
	protected boolean isAfterEventCancel;

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public boolean isExecuted() {
		return isExecuted;
	}

	public void setExecuted(boolean isExecuted) {
		this.isExecuted = isExecuted;
	}

	public boolean isAfterEventCancel() {
		return isAfterEventCancel;
	}

	public void setAfterEventCancel(boolean isAfterEventCancel) {
		this.isAfterEventCancel = isAfterEventCancel;
	}

}
