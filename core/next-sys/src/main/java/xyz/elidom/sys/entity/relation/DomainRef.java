/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.entity.relation;

import java.io.Serializable;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.entity.relation.IdFindable;
import xyz.elidom.orm.entity.relation.UniqueNameNumberIdRef;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

@Table(name = "domains", isRef = true)
public class DomainRef extends UniqueNameNumberIdRef implements IdFindable, Serializable {
	
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -1703144696203147400L;

	@PrimaryKey
	private Long id;

	@Column (name = "name", nullable = false, length = 32)
	private String name;
	
	@Column (name = "brand_name")
	private String brandName;
	
	@Column (name = "brand_image")
	private String brandImage;
	
	@Column (name = "theme")
	private String theme;
	
	@Ignore
	private String fileServiceBaseUrl;
  
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the brandName
	 */
	public String getBrandName() {
		return brandName;
	}

	/**
	 * @param brandName the brandName to set
	 */
	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	/**
	 * @return the brandImage
	 */
	public String getBrandImage() {
		return brandImage;
	}

	/**
	 * @param brandImage the brandImage to set
	 */
	public void setBrandImage(String brandImage) {
		this.brandImage = brandImage;
	}

	/**
	 * @return the theme
	 */
	public String getTheme() {
		return theme;
	}

	/**
	 * @param theme the theme to set
	 */
	public void setTheme(String theme) {
		this.theme = theme;
	}

	/**
	 * @return the fileServiceBaseUrl
	 */
	public String getFileServiceBaseUrl() {
		return fileServiceBaseUrl;
	}

	/**
	 * @param fileServiceBaseUrl the fileServiceBaseUrl to set
	 */
	public void setFileServiceBaseUrl(String fileServiceBaseUrl) {
		this.fileServiceBaseUrl = fileServiceBaseUrl;
	}

	@Override
	public Object findAndSetId() {
		if(ValueUtil.isNotEmpty(this.getName())) {
			String sql = "select id from domains where name = :name";
			IQueryManager queryMan = BeanUtil.get(IQueryManager.class);
			this.setId(queryMan.selectBySql(sql, ValueUtil.newMap("name", this.getName()), Long.class));
		}
		
		return this.getId();
	}
}
