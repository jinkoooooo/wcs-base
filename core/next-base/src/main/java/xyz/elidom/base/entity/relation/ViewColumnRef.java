/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity.relation;


import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "view_columns", isRef = true)
public class ViewColumnRef {

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