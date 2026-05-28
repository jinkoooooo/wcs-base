/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.entity;

import java.util.List;

import xyz.elidom.core.entity.relation.CodeRef;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;

@Table(name = "common_code_details", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,parentId,name", indexes = { 
	@Index(name = "ix_code_detail_0", columnList = "domain_id,parent_id,name", unique = true),
	@Index(name = "ix_code_detail_1", columnList = "parent_id,rank")
})
public class CodeDetail extends DomainStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -3820809339503298563L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;
	
	@Column(name = "parent_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String parentId;
	
	@Relation(field = "parentId")
	private CodeRef parent;	

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;
	
	@Column(name = "rank", nullable = false)
	private Integer rank;

	@Column(name = "data_1", length = OrmConstants.FIELD_SIZE_VALUE_255)
	private String data1;

	@Column(name = "data_2", length = OrmConstants.FIELD_SIZE_VALUE_255)
	private String data2;

	@Column(name = "data_3", length = OrmConstants.FIELD_SIZE_VALUE_255)
	private String data3;

	@Column(name = "data_4", length = OrmConstants.FIELD_SIZE_VALUE_255)
	private String data4;

	@Column(name = "data_5", length = OrmConstants.FIELD_SIZE_VALUE_255)
	private String data5;

	@Ignore
	private List<CodeDetail> items;

	public CodeDetail() {
	}
	
	public CodeDetail(String parentId, String name) {
		this.parentId = parentId;
		this.name = name;
	}

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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the parentId
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	
	public CodeRef getParent() {
		return parent;
	}

	public void setCommonCode(CodeRef parent) {
		this.parent = parent;
	}

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}

	public String getData3() {
		return data3;
	}

	public void setData3(String data3) {
		this.data3 = data3;
	}

	public String getData4() {
		return data4;
	}

	public void setData4(String data4) {
		this.data4 = data4;
	}

	public String getData5() {
		return data5;
	}

	public void setData5(String data5) {
		this.data5 = data5;
	}
	
	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	/**
	 * @return the items
	 */
	public List<CodeDetail> getItems() {
		return items;
	}

	/**
	 * @param items the items to set
	 */
	public void setItems(List<CodeDetail> items) {
		this.items = items;
	}
}