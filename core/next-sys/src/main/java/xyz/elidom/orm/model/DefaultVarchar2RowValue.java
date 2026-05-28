package xyz.elidom.orm.model;

import com.google.gson.annotations.SerializedName;

public class DefaultVarchar2RowValue {
	@SerializedName("column_value")
	private String Value;

	public String getValue() {
		return Value;
	}

	public void setValue(String value) {
		Value = value;
	}
}
