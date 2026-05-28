/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.converter.excel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import xyz.elidom.util.ClassUtil;

/**
 * Data를 List 형태로 받아서 Excel로 변환한다. 
 * Excel Export용 Excel Serializer
 * 
 * @author shortstop
 */
@Component
@Qualifier("export")
public class ExcelExporter implements IExcelSerializer {

	@Override
	public <E> Workbook serialize(Class<?> entityClass, List<E> dataList) {
		// create a workbook , worksheet
		Workbook wb = new HSSFWorkbook();
		this.buildSheet(wb, entityClass.getSimpleName(), entityClass, dataList);
		return wb;
	}
	
	@Override
	public Sheet buildSheet(Workbook wb, String sheetName, Class<?> entityClass, List<?> dataList) {
		Sheet sheet = wb.createSheet(sheetName);
		//CreationHelper createHelper = wb.getCreationHelper();

		// 1. Style
		this.addStyle(wb);
		// 2. Header
		List<Field> headerFields = this.addHeader(sheet, entityClass);
		int rowIndex = 1;

		// 3. Data
		for(Object data : dataList) {
			Row dataRow = sheet.createRow(rowIndex);
	
			for(int i = 0 ; i < headerFields.size() ; i++) {
				Field field = headerFields.get(i);
				Object value = ClassUtil.getFieldValue(data, field);    			
				Cell cell = dataRow.createCell(i);
				cell.setCellValue(value == null ? null : value.toString());
			}

			rowIndex++;
		}

		return sheet;
	}
	
	/**
	 * 엑셀 스타일 추가 
	 * 
	 * @param wb
	 */
	private void addStyle(Workbook wb) {
		CellStyle style = wb.createCellStyle();
		style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.index);
		// style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		// style.setAlignment(HorizontalAlignment.CENTER);
	}
	
	/**
	 * 엑셀 헤더 추가 
	 * 
	 * @param sheet
	 * @param entityClass
	 * @param data
	 * @return
	 */
	private <E> List<Field> addHeader(Sheet sheet, Class<?> entityClass) {
		Field[] fields = entityClass.getDeclaredFields();
		Row headerRow = sheet.createRow(0);
		List<Field> headerFields = new ArrayList<Field>();
		
		for(int i = 0 ; i < fields.length ; i++) {
			Field field = fields[i];
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(field.getName());
			headerFields.add(field);
		}
		
		return headerFields;
	}
	
}
