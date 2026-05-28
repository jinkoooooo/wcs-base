/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.model;

import java.util.List;

/**
 * SeedMeta Data 정의
 * 
 * @author shortstop
 */
public class SeedMeta {

	/**
	 * Dependency Module
	 */
	private String dependencyModule;
	/**
	 * Entity 순서 
	 */
	private List<SeedEntity> entityOrders;
	
	/**
	 * @return the dependencyModule
	 */
	public String getDependencyModule() {
		return dependencyModule;
	}
	
	/**
	 * @param dependencyModule the dependencyModule to set
	 */
	public void setDependencyModule(String dependencyModule) {
		this.dependencyModule = dependencyModule;
	}
	
	/**
	 * @return the entityOrders
	 */
	public List<SeedEntity> getEntityOrders() {
		return entityOrders;
	}
	
	/**
	 * @param entityOrders the entityOrders to set
	 */
	public void setEntityOrders(List<SeedEntity> entityOrders) {
		this.entityOrders = entityOrders;
	}
}
