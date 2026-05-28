/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.entity.relation;

import java.io.Serializable;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "users", isRef = true)
public class UserRef implements Serializable {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -4199395465186066190L;

	@PrimaryKey
	private String id;

	@Column(name = "name")
	private String name;
	
	@Column(name = "email")
	private String email;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
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
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
}
