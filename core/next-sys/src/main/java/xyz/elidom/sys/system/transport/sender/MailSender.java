/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.transport.sender;

import java.io.File;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.system.transport.ISender;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * Mail Sender
 * 
 * @author shortstop
 */
@Component
@Qualifier("mail")
public class MailSender implements ISender {

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private Environment env;

	public Object send(String title, Object from, Object to, Object content, Map<String, ?> parameters, Map<String, ?> options) {
		// 1. send default
		if (SysValueUtil.isEmpty(options) || !options.containsKey(SysConstants.EMAIL_OPT_MIME_TYPE)) {
			return this.sendTextMail(title, content.toString(), (String) from, to.toString(), null);

		// 2. send html
		} else {
			File[] files = this.checkAttachments(options);
			if (options.containsKey(SysConstants.EMAIL_OPT_MIME_TYPE)) {
				String mimeType = (String)options.get(SysConstants.EMAIL_OPT_MIME_TYPE);
				if (mimeType.toLowerCase().startsWith(SysConstants.EMAIL_MIME_TYPE_TEXT_HTML)) {
					return this.sendHtmlMail(title, content.toString(), (String) from, to.toString(), files);
				} else {
					return this.sendTextMail(title, content.toString(), (String) from, to.toString(), files);
				}
			}
		}
		
		return null;
	}

	/**
	 * Text로 메일 전송
	 * 
	 * @param title
	 * @param content
	 * @param from
	 * @param to
	 * @param files attachments
	 */
	public Object sendTextMail(String title, String content, String from, String to, File[] files) {
		if (SysValueUtil.isEmpty(from)) {
			from = env.getProperty(SysConfigConstants.MAIL_SMTP_USER);
		}

		this.checkValidation(title, to, content);
		SimpleMailMessage mailMessage = new SimpleMailMessage();
		
		if(to.contains(OrmConstants.COMMA)) {
			mailMessage.setTo(to.split(OrmConstants.COMMA));
		} else {
			mailMessage.setTo(to);
		}
		
		mailMessage.setReplyTo(to);
		mailMessage.setFrom(from);
		mailMessage.setSubject(title);
		mailMessage.setText(content);
		// TODO attachments
		this.mailSender.send(mailMessage);
		return SysConstants.CAP_OK_STRING;
	}

	/**
	 * HTML로 메일 전송
	 * 
	 * @param title
	 * @param content
	 * @param from
	 * @param to
	 * @param files
	 *            attachments
	 */
	public Object sendHtmlMail(String title, String content, String from, String to, File[] files) {
		if (SysValueUtil.isEmpty(from)) {
			from = env.getProperty(SysConfigConstants.MAIL_SMTP_USER);
		}
		
		this.checkValidation(title, to, content);

		try {
			MimeMessage mimeMessage = this.mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, SysConstants.CHAR_SET_UTF8);
			mimeMessage.setContent(content, SysConstants.EMAIL_MIME_TYPE_TEXT_HTML_UTF_8);
			helper.setSubject(title);
			helper.setFrom(from);
			
			if(to.contains(OrmConstants.COMMA)) {
				helper.setTo(to.split(OrmConstants.COMMA));
			} else {
				helper.setTo(to);
			}
			
			// TODO attachments
			this.mailSender.send(mimeMessage);
		} catch (Exception e) {
			throw new ElidomServiceException(SysMessageConstants.FAIL_TO_SEND_MAIL, "Failed to send mail!", e);
		}
		
		return SysConstants.CAP_OK_STRING;
	}

	/**
	 * 첨부 파일을 체크한다.
	 * 
	 * @param options
	 * @return
	 */
	private File[] checkAttachments(Map<String, ?> options) {
		if (options.containsKey(SysConstants.EMAIL_ATTACHMENTS)) {
			return (File[]) options.get(SysConstants.EMAIL_ATTACHMENTS);
		} else {
			return null;
		}
	}

	/**
	 * 전송 전 Validation Check
	 * 
	 * @param title
	 * @param to
	 * @param content
	 */
	private void checkValidation(String title, Object to, Object content) {
		AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_TITLE, title);
		AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_DESTINATION, to);
		AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_CONTENT, content);
	}
	
}
