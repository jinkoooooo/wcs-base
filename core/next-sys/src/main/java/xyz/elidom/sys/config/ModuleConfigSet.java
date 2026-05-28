/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import xyz.elidom.exception.client.ElidomInvalidParamsException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.util.ValueUtil;

/**
 * 모듈 Config 정보들을 관리하는 관리자 
 * 
 * @author shortstop
 */
@Component
public class ModuleConfigSet {

	/**
	 * Module Config Set
	 */
	private Map<String, IModuleProperties> configSet = new HashMap<String, IModuleProperties>();
	/**
	 * Dependency 순서대로 정렬된 모듈 리스트  
	 */
	private List<IModuleProperties> orderedModules;
	/**
	 * 애플리케이션 메인 모듈 프로퍼티  
	 */
	private IModuleProperties applicationModule;

	/**
	 * 모든 Module 객체를 키 : 모듈명, 값 : 모듈 객체 즉 Map 형태로 리턴한다.
	 * 
	 * @return
	 */
	public Map<String, IModuleProperties> getAll() {
		return this.configSet;
	}
	
	/**
	 * 모든 모듈 객체를 컬렉션 형태로 리턴
	 * 
	 * @return
	 */
	public Collection<IModuleProperties> getModules() {
		return this.configSet.values();
	}

	/**
	 * 모듈명으로 Module Config를 리턴한다.
	 * 
	 * @param moduleName
	 * @return
	 */
	public IModuleProperties getConfig(String moduleName) {
		return this.configSet.get(moduleName);
	}
	
	
	/**
	 * 모듈명이 포함되어 있는지 확인 
	 * @param moduleName
	 * @return
	 */
	public boolean containsModuleName(String moduleName) {
		return this.configSet.containsKey(moduleName);
	}

	/**
	 * 모듈명과 Module Config를 설정한다.
	 * 
	 * @param moduleName
	 * @param module
	 */
	public void addConfig(String moduleName, IModuleProperties module) {
		if(!this.configSet.containsKey(moduleName)) {
			this.configSet.put(moduleName, module);
		}
	}
	
	/**
	 * Scan Service Package List를 리턴 
	 * 
	 * @return
	 */
	public List<String> getScanServicePackages() {
		Iterator<IModuleProperties> iter = getModules().iterator();
		List<String> scanServicePackages = new ArrayList<String>();

		while(iter.hasNext()) {
			IModuleProperties prop = iter.next();
			scanServicePackages.add(prop.getScanServicePackage());
		}
		
		return scanServicePackages;
	}
	
	/**
	 * Module name list
	 * 
	 * @return
	 */
	public List<String> getModuleList() {
		Iterator<String> iter = this.configSet.keySet().iterator();
		List<String> moduleList = new ArrayList<String>();

		while(iter.hasNext()) {
			moduleList.add(iter.next());
		}
		
		return moduleList;
	}
	
	/**
	 * Dependency 순으로 정렬된 모듈 프로퍼티 리스트. 맨 처음이 core 모듈이다.  
	 * 
	 * @return
	 */
	public List<IModuleProperties> allOrderedModules() {
		if(this.orderedModules == null) {
			this.orderedModules = new ArrayList<IModuleProperties>();
			IModuleProperties sysModule = this.configSet.get("sys");
			this.orderedModules.add(sysModule);
			this.arrangeOrderedModule(sysModule.getName());
		}
		
		return this.orderedModules;
	}
	
	/**
	 * 모듈을 Dependency 순으로 배치하기 - 모든 모듈을 순회하면서 자신의 parent가 parentModuleName인 모듈을 순차적으로 추가한다.   
	 * 
	 * @param parentModuleName
	 */
	private void arrangeOrderedModule(String parentModuleName) {
		List<IModuleProperties> modules = this.findModuleByParentModule(parentModuleName);
		
		if(!modules.isEmpty()) {
			for(IModuleProperties module : modules) {
				if(!this.isContainsOrderedModule(module, this.orderedModules)) {
					this.orderedModules.add(module);
				}
			}
			
			for(IModuleProperties module : modules) {
				this.arrangeOrderedModule(module.getName());
			}
		}
	}
	
	/**
	 * 전달받은 parentModule이 자신의 parent 모듈과 동일한 모든 모듈을 찾아 리턴  
	 * 
	 * @param parentModule
	 * @return
	 */
	private List<IModuleProperties> findModuleByParentModule(String parentModule) {
		List<IModuleProperties> modules = new ArrayList<IModuleProperties>();
		Iterator<IModuleProperties> iter = this.configSet.values().iterator();
		
		while(iter.hasNext()) {
			IModuleProperties module = (IModuleProperties)iter.next();
			String parentsOfModule = module.getParentModule();
			if(ValueUtil.isEmpty(parentsOfModule)) {
				continue;
			}
			
			List<String> parentList = this.toParentList(parentsOfModule);
			if(parentList.contains(parentModule)) {
				modules.add(module);
			}
		}
		
		return modules;
	}
	
	/**
	 * parentModule이 ','로 구분된 여러 개의 모듈일 수 있으니 이를 구분하여 리스트 형태로 리턴한다.
	 * 
	 * @param parentModule
	 * @return
	 */
	private List<String> toParentList(String parentModule) {
		String[] parentArr = parentModule.split(OrmConstants.COMMA);
		List<String> parentList = new ArrayList<String>(parentArr.length);
		for(String parent : parentArr) {
			parentList.add(parent.trim());
		}
		
		return parentList;
	}
	
	/**
	 * OrderedModule에 포함되어 있는지 체크 
	 * 
	 * @param module
	 * @return
	 */
	private boolean isContainsOrderedModule(IModuleProperties module, List<IModuleProperties> orderedModuleList) {
		if(this.orderedModules != null) {
			boolean isExist = false;
			for(IModuleProperties prop : orderedModuleList) {
				if(ValueUtil.isEqual(module.getName(), prop.getName())) {
					isExist = true;
					break;
				}
			}
			
			return isExist;
			
		} else {
			return false;
		}
	}
	
	/**
	 * 모듈명으로 모듈명의 부모만 따라가서 순서대로 모듈 프로퍼티를 찾아 리턴한다. 
	 * 
	 * @param moduleName
	 * @return
	 */
	public List<IModuleProperties> orderedModules(String moduleName) {
		List<IModuleProperties> orderedModules = new ArrayList<IModuleProperties>();
		if(!this.configSet.containsKey(moduleName)) {
			throw new ElidomInvalidParamsException("Invalid module [" + moduleName + "]");
		}
		
		IModuleProperties currentModule = this.configSet.get(moduleName);
		orderedModules.add(currentModule);
		String parentModuleNames = currentModule.getParentModule(); 
		List<String> parentList = this.toParentList(parentModuleNames);
		
		for(String parentModuleName : parentList) {
			while(ValueUtil.isNotEmpty(parentModuleName)) {
				if(this.configSet.containsKey(parentModuleName)) {
					IModuleProperties parentModule = this.configSet.get(parentModuleName);
					if(!this.isContainsOrderedModule(parentModule, orderedModules)) {
						orderedModules.add(parentModule);
						parentModuleName = parentModule.getParentModule();
					} else {
						parentModuleName = null;
					}
				}
			}			
		}
		
		Collections.reverse(orderedModules);
		return orderedModules;
	}
	
	/**
	 * 애플리케이션의 모듈을 mainPropName 모듈로 설정 
	 * 
	 * @param mainPropName
	 */
	public void setApplicationModule(String mainPropName) {
		IModuleProperties mainProp = this.getConfig(mainPropName);
		this.applicationModule = mainProp;
	}
	
	/**
	 * 애플리케이션의 모듈을 리턴 
	 * 
	 * @return
	 */
	public IModuleProperties getApplicationModule() {
		return this.applicationModule;
	}
	
}