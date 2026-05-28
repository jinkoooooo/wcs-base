package xyz.elidom.sys.event;

/**
 * 각 모듈별 Out Event 최상위 클래스 
 * @author yang
 *
 */
public class SysOutEvent extends SysEvent{
	
	
	/**
	 * out 모듈명 
	 */
	protected String from;
	/**
	 * in 모듈명 
	 */
	protected String to;
	
	/**
	 * 미들웨어를 통한 메시지 전송 여부 
	 */
	protected boolean isSendMessage;
	
	/**
	 * 전송 메시지 
	 * TODO : 추후 최상위 메시지 Interface 로 교체 
	 */
	protected Object message;
	
	
	/**
	 * 생성자 
	 * @param from
	 * @param to
	 * @param message : Message 전송을 위한 프로토콜에 맞는 데이터 
	 *                : 추후 최상위 메시지 Interface 로 교체 
	 */
	public SysOutEvent(String from, String to, Object message) {
		super();
		this.setConstructData(from, to, message);
	}
	
	/**
	 * 생성자 
	 * @param domainId
	 * @param from
	 * @param to
	 * @param message : Message 전송을 위한 프로토콜에 맞는 데이터 
	 *                : 추후 최상위 메시지 Interface 로 교체 
	 */
	public SysOutEvent(Long domainId, String from, String to, Object message) {
		super(domainId);
		this.setConstructData(from,to,message);
	}
	
	/**
	 * 생성 기본 데이터 셋팅 
	 * @param from
	 * @param to
	 * @param message
	 */
	private void setConstructData(String from, String to, Object message) {
		this.setFrom(from);
		this.setTo(to);
		this.setMessage(message);
		this.setSendMessage(false);
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Object getMessage() {
		return message;
	}

	public void setMessage(Object message) {
		this.message = message;
	}

	public boolean isSendMessage() {
		return isSendMessage;
	}

	public void setSendMessage(boolean isSendMessage) {
		this.isSendMessage = isSendMessage;
	}
}
