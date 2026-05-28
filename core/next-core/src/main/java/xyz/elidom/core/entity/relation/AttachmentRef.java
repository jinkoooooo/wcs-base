/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.entity.relation;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.entity.relation.IdFindable;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

@Table(name = "attachments", isRef = true)
public class AttachmentRef implements IdFindable {

	@PrimaryKey
	private String id;

	@Column(name = "name")
	private String name;
	
	@Column(name = "path")
	private String path;

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

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	@Override
	public Object findAndSetId() {		
		if(SysValueUtil.isNotEmpty(this.getPath())) {
			String sql = "select id from attachments where domain_id = :domainId and path = :path";
			IQueryManager queryMan = BeanUtil.get(IQueryManager.class);
			this.setId(queryMan.selectBySql(sql, SysValueUtil.newMap("domainId,path", Domain.currentDomainId(), this.getPath()), String.class));
		}
		
		return this.getId();
	}
}
