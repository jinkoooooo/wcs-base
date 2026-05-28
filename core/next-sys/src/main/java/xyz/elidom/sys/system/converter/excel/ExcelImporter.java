/* Copyright © Nearsolution Inc. All rights reserved. */

package xyz.elidom.sys.system.converter.excel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.util.ClassUtil;

/**
 * Excel Workbook을 List 형태로 변환한다. 
 * Excel Import용 Excel Parser
 * 
 * @author shortstop
 */
@Component
@Qualifier("import")
public class ExcelImporter implements IExcelParser {

	@SuppressWarnings("unchecked")
	@Override
	public <E> List<E> parseList(Class<?> entityClass, List<String> headers, Workbook workbook) {
		int headerCount = headers.size();
		List<Field> fieldList = this.getFieldList(entityClass, headers);
		List<E> dataList = new ArrayList<E>();
		Sheet sheet = workbook.getSheetAt(0);
		Iterator<Row> rows = sheet.rowIterator();
		boolean isFirst = true;
		
		while (rows.hasNext()) {
			Row row = rows.next();
			
			if(isFirst) {
				isFirst = false;
				continue;
			}
			
			E data = (E)ClassUtil.newInstance(entityClass);
			
			for(int i = 0 ; i < headerCount ; i++) {
				Field field = fieldList.get(i);
				Cell cell = row.getCell(i);
				this.setCellValue(field, data, cell);
			}
			
			dataList.add(data);
		}
		
		return dataList;
	}

	@Override
	public <E> E parseOne(Class<?> entityClass, Workbook workbook) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private List<Field> getFieldList(Class<?> entityClass, List<String> headers) {
		List<Field> fieldList = new ArrayList<Field>();
		for(int i = 0 ; i < headers.size() ; i++) {
			try {
				fieldList.add(entityClass.getDeclaredField(headers.get(i)));
			} catch (Exception e) {
				throw new ElidomServiceException("Failed to get field. [{0}]", MessageUtil.params(headers.get(i)), e);
			}
		}
		
		return fieldList;
	}
	
	private <E> void setCellValue(Field field, E data, Cell cell) {
		Object value = null;
		
		switch (cell.getCellType()) {
			case _NONE:
				value = " ";
				break;
			case BOOLEAN:
				value = cell.getBooleanCellValue();
				break;
			case NUMERIC:
				value = cell.getNumericCellValue();
				break;
			case STRING:
				value = cell.getStringCellValue();
				break;
			case BLANK:
				value = " ";
				break;
			case ERROR:
				value = cell.getErrorCellValue();
				break;
			case FORMULA:
				switch (cell.getCachedFormulaResultType()) {
					case NUMERIC:
						value = cell.getNumericCellValue();
						break;
					case STRING:
						value = cell.getStringCellValue();
						break;
					default : break;
				}
		}
		
		ClassUtil.setFieldValue(data, field, value);
	}

}
