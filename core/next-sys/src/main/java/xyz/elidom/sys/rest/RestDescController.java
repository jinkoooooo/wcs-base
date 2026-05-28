/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.rest;

import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.api.IServiceFinder;
import xyz.elidom.sys.system.service.def.IServiceApi;
import xyz.elidom.sys.system.service.def.IServiceDef;
import xyz.elidom.util.ValueUtil;
import xyz.elidom.util.converter.msg.IJsonParser;

/**
 * Restful Service API Descriptor
 * 
 * @author shortstop
 */
@RestController
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/rests")
@ServiceDesc(description="Restful Service API Descriptor")
public class RestDescController {
	
	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(RestDescController.class);
	
	@Autowired
	@Qualifier("under_to_camel")
	private IJsonParser jsonParser;
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	/**
	 * Rest Service Descriptor 검색 API
	 * 
	 * @param module
	 * @return
	 */
	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Service Descriptor By Search Conditions")
	public List<IServiceDef> index(
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		
		Filter[] filters = null;
		
		if(ValueUtil.isNotEmpty(query)) {
			filters = this.jsonParser.parse(query, Filter[].class);
		}
		
		String module = "base";
		if(filters != null && filters.length > 0) {
			for(int i = 0 ; i < filters.length ; i++) {
				if(ValueUtil.isEqual(filters[i].getName(), "module")) {
					module = (String)filters[i].getValue();
				}
			}
		}
		
		return this.restFinder.getServicesByModule(module);
	}
	
	/**
	 * Service Id로 하나의 서비스를 찾는다. 
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find Restful Service Descriptor By Service ID")
	public IServiceDef findService(@RequestParam("id") String id) {
		return this.restFinder.getServiceDetail(id);
	}
	
	/**
	 * Service Id && API Id로 Service API를 찾는다.
	 * 
	 * @param id
	 * @param api_id
	 * @return
	 */
	@GetMapping(value="/api/{api_id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find Restful Service API Descriptor By API ID")
	public IServiceApi findApi(@RequestParam("api_id") String api_id) {
		return this.restFinder.getServiceApiDetail(api_id);
	}
	
	/**
	 * Excel Export
	 * 
	 * @param input
	 * @return
	 */
	@GetMapping(value = "/export/xls", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search and Export to Excel")
	public @ResponseBody Boolean export(HttpServletRequest request, HttpServletResponse response) {
		List<IServiceDef> list = this.restFinder.getServicesByModule(null);
		Workbook workbook = this.createWorkbook(list);

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=services.xls");        
        ServletOutputStream out = null;
        
		try {
			out = response.getOutputStream();
	        workbook.write(out);
	        
		} catch (Exception e) {
			throw new ElidomServiceException("Failed to write excel to outputstream!", e);
			
		} finally {
			if(out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		
		return true;
	}
	
	@ApiDesc(description="Create Work Book")
	private Workbook createWorkbook(List<IServiceDef> list) {
		Workbook workbook = new HSSFWorkbook();
		// TODO module 별로 Sheet 하나 생성 
        Sheet sheet = workbook.createSheet("Service List");
        this.createHeaderRow(sheet);
        this.createDataRows(sheet, list);
		return workbook;
	}
	
	@ApiDesc(description="Create Header Row")
	private Row createHeaderRow(Sheet sheet) {
		Row headerRow = sheet.createRow(0);
		String[] headers = new String[] { "Bundle", "URL", "Service Class", "Description" };
		
		for(int i = 0 ; i < headers.length ; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
		}
		
		return headerRow;
	}
	
	@ApiDesc(description="Create Data Rows")
	private void createDataRows(Sheet sheet, List<IServiceDef> dataList) {
        int rowIndex = 1;
        
        for(IServiceDef data : dataList) {
    		Row dataRow = sheet.createRow(rowIndex);    		
			dataRow.createCell(0).setCellValue(data.getModule());
			dataRow.createCell(1).setCellValue(data.getName());
			dataRow.createCell(2).setCellValue(data.getBeanClassName());
			dataRow.createCell(3).setCellValue(data.getDescription());
        	rowIndex++;
        }
	}
}