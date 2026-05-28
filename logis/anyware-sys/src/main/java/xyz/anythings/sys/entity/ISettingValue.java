package xyz.anythings.sys.entity;

/**
 * 설정 값 인터페이스
 * 
 * @author shortstop
 */
public interface ISettingValue {
	/**
	 * 설정 키
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * 설정 키
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * 설정 값
	 * 
	 * @return
	 */
	public String getValue();

	/**
	 * 설정 값
	 * 
	 * @param value
	 */
	public void setValue(String value);
}
