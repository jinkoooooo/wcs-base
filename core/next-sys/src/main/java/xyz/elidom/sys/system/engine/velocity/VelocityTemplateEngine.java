/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.engine.velocity;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.engine.ITemplateEngine;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * Velocity Template Engine
 * 
 * @author shortstop
 */
@Component
@Qualifier("basic")
public class VelocityTemplateEngine implements ITemplateEngine {

	@Override
	public void processTemplateByFile(String templateFilePath, OutputStream os, Map<String, ?> variables, Map<String, ?> options) {
		throw ThrowUtil.newNotSupportedMethodYet();
	}

	@Override
	public void processTemplateByFile(String templateFilePath, Writer writer, Map<String, ?> variables, Map<String, ?> options) {
		VelocityEngine ve = new VelocityEngine();
		VelocityContext context = this.createVelocityContext(ve, variables);
		Template t = ve.getTemplate(templateFilePath);		
		t.merge(context, writer);
	}

	@Override
	public void processTemplate(String templateContent, OutputStream os, Map<String, ?> variables, Map<String, ?> options) {
		throw ThrowUtil.newNotSupportedMethodYet();
	}

	@Override
	public void processTemplate(String templateContent, Writer writer, Map<String, ?> variables, Map<String, ?> options) {
		VelocityEngine ve = new VelocityEngine();
		VelocityContext context = this.createVelocityContext(ve, variables);
		Velocity.evaluate(context, writer, "processTemplate", templateContent);
	}

	@Override
	public void processTemplate(InputStream is, OutputStream os, Map<String, ?> variables, Map<String, ?> options) {
		throw ThrowUtil.newNotSupportedMethodYet();
	}

	@Override
	public void processTemplate(Reader reader, Writer writer, Map<String, ?> variables, Map<String, ?> options) {
		throw ThrowUtil.newNotSupportedMethodYet();
	}
	
	/**
	 * create velocity context
	 * 
	 * @param ve
	 * @param variables
	 * @return
	 */
	private VelocityContext createVelocityContext(VelocityEngine ve, Map<String, ?> variables) {
		ve.init();
		VelocityContext context = new VelocityContext();
		
		if(SysValueUtil.isNotEmpty(variables)) {
			Iterator<String> keyIter = variables.keySet().iterator();
			while(keyIter.hasNext()) {
				String key = keyIter.next();
				Object value = variables.get(key);
				context.put(key, value);
			}
		}
		
		return context;
	}
		
	/*@Override
	public String processTemplateByFile(String templateFile, Map<String, Object> variables) {
		VelocityEngine ve = new VelocityEngine();
		VelocityContext context = this.createVelocityContext(ve, variables);
		
		Template t = ve.getTemplate(templateFile);
		String result = this.process(context, t);
		return result;
	}

	@Override
	public String processTemplate(String templateContent, Map<String, Object> variables) {
		VelocityEngine ve = new VelocityEngine();
		VelocityContext context = this.createVelocityContext(ve, variables);
		
		StringWriter writer = new StringWriter();
		StringReader reader = new StringReader(templateContent);
		Reader templateReader = new BufferedReader(reader);
		Velocity.evaluate(context, writer, "processTemplate", templateReader);
		return writer.toString();
	}
	
	
	private VelocityContext createVelocityContext(VelocityEngine ve, Map<String, Object> variables) {
		ve.init();
		VelocityContext context = new VelocityContext();
		
		if(!CommUtil.isEmpty(variables)) {
			Iterator<String> keyIter = variables.keySet().iterator();
			while(keyIter.hasNext()) {
				String key = keyIter.next();
				Object value = variables.get(key);
				context.put(key, value);
			}
		}
		
		return context;
	}
	
	private String process(VelocityContext context, Template t) {
		StringWriter writer = new StringWriter();
		t.merge(context, writer);
		return writer.toString();
	}*/
}