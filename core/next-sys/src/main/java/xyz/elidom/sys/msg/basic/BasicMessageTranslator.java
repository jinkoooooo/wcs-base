/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.msg.basic;

import java.util.List;

import org.springframework.stereotype.Component;

import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.msg.I18nMsgTranslator;
import xyz.elidom.util.ValueUtil;

/**
 * 기본 메시지 번역기 - 메시지 번역을 위한 소스는 없고 기본 언어로만 번역 
 * 
 * @author shortstop
 */
@Component
public class BasicMessageTranslator implements I18nMsgTranslator {

	@Override
	public String translate(String locale, String msgCode) {
		return ValueUtil.isEmpty(msgCode) ? msgCode : msgCode.replace(SysConstants.CHAR_UNDER_SCORE, SysConstants.CHAR_EMPTY);
	}

	@Override
	public String translate(String locale, String msgCode, String defaultValue) {
		return defaultValue;
	}

	@Override
	public String translate(String locale, String msgCode, String defaultValue, List<String> params) {
		return defaultValue;
	}

}
