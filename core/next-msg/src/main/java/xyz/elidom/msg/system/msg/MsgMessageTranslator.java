/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.msg.system.msg;

import java.text.MessageFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import xyz.elidom.msg.rest.MessageController;
import xyz.elidom.sys.msg.I18nMsgTranslator;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 다국어 메시지 번역기 - 메시지 번역을 위한 소스는 없고 기본 언어로만 번역 
 * TODO domainId가 들어간 API 추가 필요
 * 
 * @author shortstop
 */
@Component
public class MsgMessageTranslator implements I18nMsgTranslator {

	/**
	 * Message Controller
	 */
	@Autowired
	private MessageController msgCtrl;
	
	@Override
	public String translate(String locale, String msgCode) {
		// 1. 로케일 체크 
		locale = SettingUtil.getLocale(locale);
		// 2. 메시지 번역 
		String msgValue = this.msgCtrl.findBy(locale, msgCode);
		// 3. 찾는 값이 없다면 defaultValue를 리턴, defaultValue가 없다면 msgCode를 리턴
		return ValueUtil.checkValue(msgValue, msgCode);
	}

	@Override
	public String translate(String locale, String msgCode, String defaultValue) {
		// 1. 로케일 체크 
		locale = SettingUtil.getLocale(locale);
		// 2. 메시지 번역 
		String msgValue = this.msgCtrl.findBy(locale, msgCode);
		// 3. 찾는 값이 없다면 defaultValue를 리턴, defaultValue가 없다면 msgCode를 리턴
		return ValueUtil.checkValue(msgValue, ValueUtil.checkValue(defaultValue, msgCode));
	}

	@Override
	public String translate(String locale, String msgCode, String defaultValue, List<String> params) {
		// 1. 로케일 체크 
		locale = SettingUtil.getLocale(locale);
		// 2. 메시지 번역 
		String msgValue = this.msgCtrl.findBy(locale, msgCode);
		// 3. 찾는 값이 없다면 defaultValue를 리턴, defaultValue가 없다면 msgCode를 리턴
		if(ValueUtil.isEmpty(msgValue)) {
			msgValue = ValueUtil.checkValue(defaultValue, msgCode);
		}
		
		// 4. 파라미터가 없다면 값 리턴 
		if(ValueUtil.isEmpty(params)) {
			return msgValue;
		
		// 5. 파라미터가 있다면 번역해서 값 리턴 
		} else {
			Object[] paramStrs = StringUtils.toStringArray(params);
			return MessageFormat.format(msgValue, paramStrs);
		}
	}
	
}
