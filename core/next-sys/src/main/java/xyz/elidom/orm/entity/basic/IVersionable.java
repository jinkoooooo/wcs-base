/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

/**
 * 버전 관리를 위한 인터페이스 
 * 
 * @author shortstop
 */
public interface IVersionable {
	
	/**
	 * 결재 상태 
	 */
	public static final String STATUS_APPROVAL = "APPROVAL";
	
	/**
	 * Release 상태 
	 */
	public static final String STATUS_RELEASED = "RELEASED";

	/**
	 * @return the version
	 */
	public Integer getVersion();

	/**
	 * @param version the version to set
	 */
	public void setVersion(Integer version);

	/**
	 * @return the status
	 */
	public String getStatus();

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status);

	/**
	 * @param active the active version or not
	 */
	public Boolean getActive();

	/**
	 * @return the active version
	 */
	public void setActive(Boolean active);
	
	/**
	 * release를 위한 심사 승인 요청 
	 * 
	 * @return
	 */
	public IVersionable requestRelease();
	
	/**
	 * release를 위한 심사 승인 요청 취소 
	 * 
	 * @return
	 */
	public IVersionable cancelRequestRelease();	
	
	/**
	 * release
	 * 
	 * @return
	 */
	public IVersionable release();
	
	/**
	 * 강제 release
	 * 
	 * @return
	 */
	public IVersionable releaseForcibly();
	
	/**
	 * version up
	 * 
	 * @return
	 */
	public IVersionable versionUp();
	
	/**
	 * release 중단 - 심사 승인 취소 혹은 심사 승인 중 반려된 경우 Release를 중단하고 이전 상태로 복귀한다.
	 * 
	 * @return
	 */
	public IVersionable cancelRelease();
	
}
