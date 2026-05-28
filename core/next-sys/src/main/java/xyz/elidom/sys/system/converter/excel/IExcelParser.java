/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.converter.excel;

import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel Parser
 * 
 * @author shortstop
 */
public interface IExcelParser {

	/**
	 * Excel Workbook을 parsing하여 entityClass의 인스턴스 List형식으로 변환하여 리턴한다.
	 * 
	 * @param entityClass
	 * @param headers
	 * @param workbook
	 * @return
	 */
	public <E> List<E> parseList(Class<?> entityClass, List<String> headers, Workbook workbook);
	
	/**
	 * Excel Workbook을 parsing하여 entityClass의 인스턴스로 변환하여 리턴한다.
	 * 
	 * @param entityClass
	 * @param workbook
	 * @return
	 */
	public <E> E parseOne(Class<?> entityClass, Workbook workbook);
}
