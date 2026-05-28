/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.model;

/**
 * Seed Entity Meta 정의
 * 
 * @author shortstop
 */
public class SeedEntity {

	/**
	 * Entity가 소속된 Bundle 명 
	 */
	private String bundle;
	/**
	 * Seed 대상 entity
	 */
	private String entity;
	/**
	 * Seed 대상 subentities
	 */
	private String subEntities;
	/**
	 * Delete Query
	 */
	private String query;
	
	/**
	 * @return the bundle
	 */
	public String getBundle() {
		return bundle;
	}

	/**
	 * @return the bundle
	 */
	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	/**
	 * @return the entity
	 */
	public String getEntity() {
		return entity;
	}
	
	/**
	 * @param entity the entity to set
	 */
	public void setEntity(String entity) {
		this.entity = entity;
	}
	
	/**
	 * @return the subEntities
	 */
	public String getSubEntities() {
		return subEntities;
	}
	
	/**
	 * @param subEntities the subEntities to set
	 */
	public void setSubEntities(String subEntities) {
		this.subEntities = subEntities;
	}
	
	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}
	
	/**
	 * @param query the query to set
	 */
	public void setQuery(String query) {
		this.query = query;
	}	
	
}
