package xyz.elidom.sys.event;

import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.ValueUtil;

/**
 * Anythings 시스템 최상위 이벤트
 * 
 * @author shortstop
 */
public class SysEvent {
	
	/**
	 * 이벤트 전 처리 
	 */
	public static final short EVENT_STEP_BEFORE = 1;
	/**
	 * 이벤트 후 처리 
	 */
	public static final short EVENT_STEP_AFTER = 2;
	/**
	 * 이벤트 단독
	 */
	public static final short EVENT_STEP_ALONE = 3;

	/**
	 * 도메인 ID
	 */
	protected Long domainId;
	/**
	 * 이벤트 스텝 - 1 : 전 처리 , 2 : 후 처리
	 */
	protected short eventStep;
	/**
	 * 이벤트 소스 
	 */
	protected String eventSource;
	/**
	 * 이벤트 타겟 
	 */
	protected String eventTarget;
	/**
	 * 다음 이벤트를 계속 발생 할 건지 여부, DEFAULT => false
	 */
	protected boolean isAfterEventCancel;
	/**
	 * 확장을 위한 Event 파라미터
	 */
	protected Object[] payload;
	/**
	 * 이벤트 처리 완료 flag
	 * 이벤트 처리가 오버라이드 된 경우 기본 이벤트를 skip하기 위한 flag
	 */
	protected boolean isExecuted;
	/**
	 *  이벤트 처리 결과 
	 */
	protected Object result;
	
	/**
	 * 기본 생성자
	 */
	public SysEvent() {
		this(Domain.currentDomainId());
	}
	
	/**
	 * 생성자 1
	 *  
	 * @param domainId
	 */
	public SysEvent(Long domainId) {
		this(domainId, null);
	}
	
	/**
	 * 생성자 2
	 * 
	 * @param domainId
	 * @param payload
	 */
	public SysEvent(Long domainId, Object[] payload) {
		this.domainId = domainId;
		this.payload = payload;
		this.isExecuted = false;
	}

	public Long getDomainId() {
		return domainId;
	}

	public void setDomainId(Long domainId) {
		this.domainId = domainId;
	}

	public short getEventStep() {
		return eventStep;
	}

	public void setEventStep(short eventStep) {
		this.eventStep = eventStep;
	}

	public String getEventSource() {
		return eventSource;
	}

	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}

	public String getEventTarget() {
		return eventTarget;
	}

	public void setEventTarget(String eventTarget) {
		this.eventTarget = eventTarget;
	}

	public boolean isAfterEventCancel() {
		return isAfterEventCancel;
	}

	public void setAfterEventCancel(boolean isAfterEventCancel) {
		this.isAfterEventCancel = isAfterEventCancel;
	}

	public Object[] getPayload() {
		return payload;
	}

	public void setPayload(Object[] payload) {
		this.payload = payload;
	}

	public boolean isExecuted() {
		return isExecuted;
	}

	public void setExecuted(boolean isExecuted) {
		this.isExecuted = isExecuted;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	/**
	 * 이벤트 전/후 처리 결과 셋 리턴
	 * 
	 * @return 이벤트 전/후 처리 결과 셋
	 */
	public EventResultSet getEventResultSet() {
		EventResultSet resultSet = new EventResultSet();
		
		// 단독 이벤트
		if(ValueUtil.isEqual(this.getEventStep(), SysEvent.EVENT_STEP_ALONE)) {
			resultSet.setExecuted(this.isExecuted());
			resultSet.setResult(this.getResult());
		
		// 후 처리 이벤트
		} else if(ValueUtil.isEqual(this.getEventStep(), SysEvent.EVENT_STEP_AFTER)) {
			resultSet.setExecuted(this.isExecuted());
			resultSet.setResult(this.getResult());
		
		// 전 처리 이벤트
		} else {
			resultSet.setAfterEventCancel(this.isExecuted() && this.isAfterEventCancel());
			resultSet.setExecuted(this.isExecuted());
			resultSet.setResult(this.getResult());
		}
		
		return resultSet;
	}
}
