/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.core.entity.DataSrc;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.IDataSourceManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SysValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/data_srcs")
@ServiceDesc(description = "DataSrc Service API")
public class DataSrcController extends AbstractRestService {
	
	/**
	 * 기본 소트 조건 - '[{\"field\": \"name\", \"ascending\": true}]'
	 */
	private static String DEFAULT_SORT = "[{\"field\": \"name\", \"ascending\": true}]";

	@Autowired
	protected IDataSourceManager dataSourceManager;
	
	@Override
	protected Class<?> entityClass() {
		return DataSrc.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search (Pagination) By Search Conditions")
	public Page<?> index(@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		
		if(SysValueUtil.isEmpty(sort)) {
			sort = DEFAULT_SORT;
		}
		
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find one by ID")
	public DataSrc findOne(@PathVariable("id") String id) {
		return this.getOne(this.entityClass(), id);
	}

	@GetMapping(value = "/{id}/exist", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Before Import")
	public List<DataSrc> checkImport(@RequestBody List<DataSrc> list) {
		for (DataSrc item : list) {
			this.checkForImport(DataSrc.class, item);
		}
		
		return list;
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create")
	public DataSrc create(@RequestBody DataSrc input) {
		return this.createOne(input);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update")
	public DataSrc update(@PathVariable("id") String id, @RequestBody DataSrc input) {
		return this.updateOne(input);
	}

	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple data at one time")
	public Boolean multipleUpdate(@RequestBody List<DataSrc> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}
	
	@PostMapping(value = "/{id}/init_pool", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Initialize datasource pool")	
	public Boolean initPool(@PathVariable("id") String id) {
		DataSrc datasource = this.getOne(false, false, this.entityClass(), id);
		
		if(datasource == null) {
			return false;
			
		} else {
			// 1. 이전 데이터 소스가 있다면 destroy
			String dsName = datasource.getName();
			if(this.dataSourceManager.isExistDataSource(dsName)) {
				this.dataSourceManager.destroyDataSource(dsName);
			}
			
			// 2. 새로운 데이터 소스 initialize
			this.dataSourceManager.initializeDataSource(dsName, datasource.getClassName(), datasource.getUrl(), datasource.getDomain(), datasource.getUserid(), datasource.getPassword(), datasource.getMinIdle(), datasource.getMaxIdle(), datasource.getMaxActive(), datasource.getMaxWait(), datasource.getEvictTime());
			datasource.setStatus(DataSrc.STATUS_CONNECTED);
			this.queryManager.update(datasource, OrmConstants.ENTITY_FIELD_STATUS, OrmConstants.ENTITY_FIELD_UPDATED_AT);
			return true;
		}
	}

	@PostMapping(value = "/{id}/destroy_pool", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Destroy datasource pool")	
	public Boolean destroyPool(@PathVariable("id") String id) {
		DataSrc datasource = this.getOne(false, false, this.entityClass(), id);
		dataSourceManager.destroyDataSource(datasource.getName());
		datasource.setStatus(DataSrc.STATUS_CLOSED);
		this.queryManager.update(datasource, OrmConstants.ENTITY_FIELD_STATUS, OrmConstants.ENTITY_FIELD_UPDATED_AT);
		return true;
	}
	
}