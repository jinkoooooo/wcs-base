/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity.relation;


import java.io.Serializable;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.relation.UniqueNameStringIdRef;

@Table(name = "menus", isRef = true)
public class MenuRef extends UniqueNameStringIdRef implements Serializable {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -680415706745825754L;

	@PrimaryKey
	private String id;

	@Column (name = "name")
	private String name;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}