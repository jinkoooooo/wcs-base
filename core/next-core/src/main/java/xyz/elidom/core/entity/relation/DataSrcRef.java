/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.entity.relation;


import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.relation.UniqueNameStringIdRef;

@Table(name = "data_srcs", isRef = true)
public class DataSrcRef extends UniqueNameStringIdRef {

	@PrimaryKey
	private String id;

	@Column (name = "name", nullable = false, length = 32)
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