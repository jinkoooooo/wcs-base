/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.ValueUtil;

/**
 * 애플리케이션을 구성하는 모듈 정보를 제공하는 API  
 * 
 * @author shortstop
 */
@RestController
@ResponseStatus(HttpStatus.OK)	
@RequestMapping("/rest/modules")
@ServiceDesc(description="Module Listing Service")
public class ModuleController {
	
	@Autowired
	Environment env;
	
	@Autowired
	private ModuleConfigSet configSet;
	/**
	 * Module (name - description) List 
	 */
	private List<Map<String, String>> modules;

	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="List Module Names")	
	public List<String> names() {
		List<String> modules = this.configSet.getModuleList();
		return modules;		
	}
	
	@GetMapping(value="/codes", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="List Module Name - Description")	
	public List<Map<String, String>> nameDescriptions() {
		if(this.modules == null) {
			List<IModuleProperties> orderedModules = this.configSet.allOrderedModules();
			this.modules = new ArrayList<Map<String, String>>(orderedModules.size());
			
			for(IModuleProperties mod : orderedModules) {
				Map<String, String> moduleInfo = new HashMap<String, String>();
				moduleInfo.put(mod.getName(), mod.getDescription());
				this.modules.add(moduleInfo);
			}
		}
		
		return this.modules;
	}	
	
	@GetMapping(value="/infos", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="List Module Informations")
	public Iterator<IModuleProperties> list() {
		Iterator<IModuleProperties> modules = this.configSet.getModules().iterator();
		return modules;
	}
	
	@ApiDesc(description = "Root Project Version Information")
	@GetMapping(value = "/root", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> version() {
		Map<String, String> info = new HashMap<String, String>();
		String rootProjectName = env.getProperty(SysConfigConstants.INFO_ID);

		Collection<IModuleProperties> modules = this.configSet.getModules();
		for (IModuleProperties module : modules) {
			if (ValueUtil.isEqual(module.getProjectName(), rootProjectName)) {
				info.put("module", module.getName());
				info.put("version", module.getVersion());
				info.put("project_name", module.getProjectName());
				info.put("built_at", module.getBuiltAt());
				break;
			}
		}

		if (info.isEmpty()) {
			info.put("projectName", rootProjectName);
			info.put("version", "Project name not matched with info ID.");
		}

		return info;
	}

	@ApiDesc(description = "Project Module Version Informations")
	@GetMapping(value = "/versions", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Map<String, String>> detailVersions() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		List<IModuleProperties> moduleList = new ArrayList<IModuleProperties>();
		this.configSet.getModules().forEach(module -> moduleList.add(module));

		moduleList.sort(Comparator.comparing(IModuleProperties::getName));

		for (IModuleProperties module : moduleList) {
			Map<String, String> info = new HashMap<String, String>();
			info.put("module", module.getName());
			info.put("version", module.getVersion());
			info.put("project_name", module.getProjectName());

			list.add(info);
		}

		return list;
	}
}