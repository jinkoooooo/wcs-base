package xyz.anythings.sys.model;

/**
 * Key & Value 모델
 * 
 * @author shortstop
 */
public class KeyValue {
	/**
	 * key
	 */
	private String key;
	/**
	 * value
	 */
	private Object value;
	
	public KeyValue() {
	}
	
	public KeyValue(String key, Object value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}
