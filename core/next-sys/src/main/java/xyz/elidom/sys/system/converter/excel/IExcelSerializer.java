/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.converter.excel;

import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel Serializer
 * 
 * @author shortstop
 */
public interface IExcelSerializer {

	/**
	 * excel serialize
	 * 
	 * @param entityClass
	 * @param dataList
	 * @return
	 */
	public <E> Workbook serialize(Class<?> entityClass, List<E> dataList);
	
	/**
	 * build sheet
	 * 
	 * @param wb
	 * @param sheetName
	 * @param entityClass
	 * @param dataList
	 * @return
	 */
	public Sheet buildSheet(Workbook wb, String sheetName, Class<?> entityClass, List<?> dataList);
}
