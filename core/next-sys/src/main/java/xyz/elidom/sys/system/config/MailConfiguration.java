/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.util.ValueUtil;

@Configuration
public class MailConfiguration {

	@Autowired
	Environment env;

    @Bean
    JavaMailSender javaMailSender() {
		String host = env.getProperty(SysConfigConstants.MAIL_SMTP_HOST, "smtp.gmail.com");
		Integer port = ValueUtil.toInteger(env.getProperty(SysConfigConstants.MAIL_SMTP_PORT), 587);
		String username = env.getProperty(SysConfigConstants.MAIL_SMTP_USER);
		String password = env.getProperty(SysConfigConstants.MAIL_SMTP_PASSWORD);
		String protocol = env.getProperty(SysConfigConstants.MAIL_SMTP_PROTOCOL, "smtp");
		boolean auth = ValueUtil.toBoolean(env.getProperty(SysConfigConstants.MAIL_SMTP_AUTH), true);
		boolean starttls = ValueUtil.toBoolean(env.getProperty(SysConfigConstants.MAIL_SMTP_STARTTLS_ENABLE), true);

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(host);
		mailSender.setPort(port);
		mailSender.setUsername(username);
		mailSender.setPassword(password);

		Properties prop = mailSender.getJavaMailProperties();
		prop.put(SysConfigConstants.MAIL_TRANSPORT_PROTOCOL, protocol);
		prop.put(SysConfigConstants.MAIL_SMTP_AUTH, auth);
		prop.put(SysConfigConstants.MAIL_SMTP_STARTTLS_ENABLE, starttls);

		return mailSender;
	}
}