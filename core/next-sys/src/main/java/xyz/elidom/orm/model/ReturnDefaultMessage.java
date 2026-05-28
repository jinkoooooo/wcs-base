package xyz.elidom.orm.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import xyz.elidom.sys.util.SysValueUtil;

/**
 * 프로시저 기본 처리 결과 메시지 모델  
 * @author yang
 *
 */
public class ReturnDefaultMessage {
	
	// 결과 코드 
	@SerializedName("P_OUT_RESULT_CODE")
	private int pOutResultCode;
	
	// 결과 메시지 
	@SerializedName("P_OUT_MESSAGE")
	private String pOutMessage;
	
	public int getpOutResultCode() {
		return pOutResultCode;
	}
	public void setpOutResultCode(int pOutResultCode) {
		this.pOutResultCode = pOutResultCode;
	}
	public String getpOutMessage() {
		return pOutMessage;
	}
	public void setpOutMessage(String pOutMessage) {
		this.pOutMessage = pOutMessage;
	}
	
	public List<String> returnListToStringList(List<DefaultVarchar2RowValue> list){
		List<String> retList = new ArrayList<String>();
		
		if(SysValueUtil.isEmpty(list)) return retList;
		
		for(DefaultVarchar2RowValue value : list) {
			retList.add(value.getValue());
		}
		
		return retList;
	}
}