/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.engine.excel;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.engine.ITemplateEngine;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.ThrowUtil;

/**
 * Excel Template Engine
 * 
 * @author lyonghwan
 */
@Component
@Qualifier("excel")
public class ExcelTemplateEngine implements ITemplateEngine {

	@Override
	public void processTemplate(String templateContent, OutputStream os, Map<String, ?> variables, Map<String, ?> options) {
		InputStream is = this.contentToInputStream(templateContent);
		this.process(is, os, variables);
	}

	@Override
	public void processTemplate(String templateContent, Writer writer, Map<String, ?> variables, Map<String, ?> options) {
		throw ThrowUtil.newNotSupportedMethodYet();
	}

	@Override
	public void processTemplate(InputStream is, OutputStream os, Map<String, ?> variables, Map<String, ?> options) {
		this.process(is, os, variables);
	}

	@Override
	public void processTemplate(Reader reader, Writer writer, Map<String, ?> variables, Map<String, ?> options) {
		throw ThrowUtil.newNotSupportedMethodYet();
	}

	@Override
	public void processTemplateByFile(String templateFilePath, OutputStream os, Map<String, ?> variables, Map<String, ?> options) {
		InputStream is = pathToInputStream(templateFilePath);
		this.process(is, os, variables);
	}

	@Override
	public void processTemplateByFile(String templateFilePath, Writer writer, Map<String, ?> variables, Map<String, ?> options) {
		throw ThrowUtil.newNotSupportedMethodYet();
	}

	/**
	 * process Template
	 * 
	 * @param is
	 * @param os
	 * @param variables
	 */
	private void process(InputStream is, OutputStream os, Map<String, ?> variables) {
		Context context = new Context();
		this.putVariablesToContext(context, variables);
		
		try {
			JxlsHelper.getInstance().processTemplate(is, os, context);
		} catch (Exception e) {
			throw ThrowUtil.newFailToProcessTemplate("Excel", e);
		}
	}

	/**
	 * context에 변수를 추가
	 * 
	 * @param context
	 * @param variables
	 */
	private void putVariablesToContext(Context context, Map<String, ?> variables) {
		AssertUtil.assertNotEmpty("terms.label.parameter", variables);

		Iterator<String> keyIter = variables.keySet().iterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			context.putVar(key, variables.get(key));
		}
	}

	/**
	 * file path to input stream
	 * 
	 * @param filePath
	 * @return
	 */
	private InputStream pathToInputStream(String filePath) {
		try {
			return new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			throw ThrowUtil.newNotFoundFile(filePath);
		}
	}

	/**
	 * file content to input stream
	 * 
	 * @param content
	 * @return
	 */
	private InputStream contentToInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}
}