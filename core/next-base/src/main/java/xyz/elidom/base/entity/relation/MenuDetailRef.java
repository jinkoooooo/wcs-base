/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity.relation;

import java.io.Serializable;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.relation.UniqueNameStringIdRef;

@Table(name = "menu_details", isRef = true)
public class MenuDetailRef extends UniqueNameStringIdRef implements Serializable {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 8474823160876347676L;

	@PrimaryKey
	private String id;
	
	@Column (name = "name")
	private String name;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
}