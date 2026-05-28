package xyz.elidom.sys.event;

/**
 * 라벨, PDF 등 인쇄 관련 부가 정보를 추출하기 위한 이벤트
 * 
 * @author shortstop
 */
public class FindPrintInfoEvent extends SysEvent {
	/**
	 * 인쇄 유형 (barcode, normal)
	 */
	private String printType;	
	/**
	 * 작업 유형
	 */
	private String jobType;
	/**
	 * 인쇄 템플
	 */
	private String printTemplate;
	/**
	 * 프린터 ID
	 */
	private String printerId;
	/**
	 * 프린트 이벤트
	 */
	private PrintEvent printEvent;
	
	public FindPrintInfoEvent(PrintEvent printEvent) {
		this.printEvent = printEvent;
		this.jobType = this.printEvent.getJobType();
		this.printType = this.printEvent.getPrintType();
	}

	public String getPrintType() {
		return printType;
	}

	public void setPrintType(String printType) {
		this.printType = printType;
	}

	public String getJobType() {
		return jobType;
	}


	public void setJobType(String jobType) {
		this.jobType = jobType;
	}


	public String getPrintTemplate() {
		return printTemplate;
	}

	public void setPrintTemplate(String printTemplate) {
		this.printTemplate = printTemplate;
	}

	public String getPrinterId() {
		return printerId;
	}

	public void setPrinterId(String printerId) {
		this.printerId = printerId;
	}

	public PrintEvent getPrintEvent() {
		return printEvent;
	}

	public void setPrintEvent(PrintEvent printEvent) {
		this.printEvent = printEvent;
	}
}
