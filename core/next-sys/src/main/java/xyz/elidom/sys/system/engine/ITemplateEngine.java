/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.engine;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Template Engine 인터페이스
 * 
 * @author shortstop
 */
@Component
public interface ITemplateEngine {

	/**
	 * template file로 부터 템플릿을 변환하여 내용을 리턴  
	 * 
	 * @param templateFilePath
	 * @param os
	 * @param variables
	 * @param options
	 */
	public void processTemplateByFile(String templateFilePath, OutputStream os, Map<String, ?> variables, Map<String, ?> options);
	
	/**
	 * template file로 부터 템플릿을 변환하여 내용을 리턴  
	 * 
	 * @param templateFilePath
	 * @param writer
	 * @param variables
	 * @param options
	 */
	public void processTemplateByFile(String templateFilePath, Writer writer, Map<String, ?> variables, Map<String, ?> options);
	
	/**
	 * template file로 부터 템플릿을 변환하여 내용을 리턴  
	 * 
	 * @param templateContent
	 * @param os
	 * @param variables
	 * @param options
	 */
	public void processTemplate(String templateContent, OutputStream os, Map<String, ?> variables, Map<String, ?> options);
	
	/**
	 * template file로 부터 템플릿을 변환하여 내용을 리턴  
	 * 
	 * @param templateContent
	 * @param writer
	 * @param variables
	 * @param options
	 */
	public void processTemplate(String templateContent, Writer writer, Map<String, ?> variables, Map<String, ?> options);
	
	/**
	 * template 내용으로 템플릿을 변환하여 부터 변환
	 * 
	 * @param is
	 * @param os
	 * @param variables
	 * @param options
	 * @return
	 */
	public void processTemplate(InputStream is, OutputStream os, Map<String, ?> variables, Map<String, ?> options);
	
	/**
	 * template 내용으로 템플릿을 변환하여 부터 변환
	 * 
	 * @param is
	 * @param writer
	 * @param variables
	 * @param options
	 * @return
	 */
	public void processTemplate(Reader reader, Writer writer, Map<String, ?> variables, Map<String, ?> options);
		
}