/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.util;

import java.util.Date;

import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.util.ValueUtil;

/**
 * Date 관련 유틸리티 클래스
 * 
 * @author Minu.Kim
 */
public class DateUtil extends xyz.elidom.util.DateUtil {
	
	/**
	 * UTC 활성화 여부에 따라 UTC Time 또는 현재 OS의 시간 가져오기 실행 
	 * 
	 * @return
	 */
	public static Date getDate() {
		// UTC Time 활성화 여부 --> SettingUtil에서 가져오면 seed 작업시 에러 발생, 따라서 EnvUtil로 변경  
		//boolean enableUtcTime = ValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.ENABLE_UTC_TIME, "false"));
		boolean enableUtcTime = ValueUtil.toBoolean(EnvUtil.getValue(SysConfigConstants.ENABLE_UTC_TIME, "false"));
		return enableUtcTime ? DateUtil.getCurrentUtcDate() : new Date();
	}
}