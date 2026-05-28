/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity;

import java.util.List;

import xyz.elidom.base.entity.relation.ResourceRef;
import xyz.elidom.core.entity.CodeDetail;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;

@Table(name = "entity_columns", idStrategy = GenerationRule.UUID, uniqueFields = "entityId,name", indexes = { 
	@Index(name = "ix_entity_col_0", columnList = "entity_id,name", unique = true),
	@Index(name = "ix_entity_col_1", columnList = "entity_id,rank")
})
public class ResourceColumn extends DomainStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -4539706064949417667L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "entity_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String entityId;
	
	@Relation(field = "entityId")
	private ResourceRef entity;

	/**
	 * 필드 명 
	 */
	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	/**
	 * 필드 설명 
	 */
	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	/**
	 * 표시 순
	 */
	@Column(name = "rank", nullable = false)
	private Integer rank;

	/**
	 * 다국어를 위한 용어 키 
	 */
	@Column(name = "term", length = 40)
	private String term;

	/**
	 * 컬럼 타입 - string, number, date, datetime, text
	 */
	@Column(name = "col_type", nullable = false, length = 15)
	private String colType;
	
	/**
	 * 컬럼 사이즈 - 일단 String 타입일 경우만 설정. Validation을 위한 설정 
	 */
	@Column(name = "col_size")
	private Integer colSize;

	/**
	 * Null 허용 여부 - Validation을 위한 설정 
	 */
	@Column(name = "nullable")
	private Boolean nullable;

	/**
	 * 참조 타입 - CommonCode / Entity / Menu
	 * 화면에서 ref_type, ref_name, ref_url, ref_params 등을 통해 특정 리소스의 선택을 위한 Selector 창을 띄우기 위해 필요  
	 */
	@Column(name = "ref_type", length = 15)
	private String refType;

	/**
	 * 참조 타입에 따른 참조 명
	 * 1) CommonCode의 경우 코드명
	 * 2) Entity의 경우 - Entity 명
	 * 3) Menu의 경우 - Menu 명
	 */
	@Column(name = "ref_name", length = OrmConstants.FIELD_SIZE_NAME)
	private String refName;	

	/**
	 * 참조 타입에 따른 서비스 URL
	 */
	@Column(name = "ref_url", length = OrmConstants.FIELD_SIZE_URL)
	private String refUrl;
	
	/**
	 * 리소스를 선택하기 위해 ref_url로 서비스 호출시 파라미터로 참조될 필드들을 ','로 구분하여 입력. 여기서 입력된 필드들은 서비스 호출시 파라미터로 동작한다.
	 */
	@Column(name = "ref_params", length = 128)
	private String refParams;
	
	/**
	 * 그리드(상세 폼)에서 참조 리소스를 선택하면 기본으로는 그리드에 title field로 설정된 값이 하나만 선택이 되고 내부적으로는 id field로 설정된 값을 갖는다. 
	 * 즉 그리드(상세 폼)에는 참조한 리소스의 title field를 보여주고 id field를 내부적으로 가져 저장이나 기타 트랜잭션시에는 id field에 해당하는 값이 서버에 호출된다. 
	 * 하지만 리소스를 선택한 후 해당 컬럼 뿐 아니라 다른 필드에도 값을 설정하고 싶은 경우, 여기에 리소스 필드1=그리드 필드1,리소스 필드2=그리드 필드2 형식으로 입력하면 그리드 행에 선택한 리소스의 여러 필드들이 입력된다.  
	 */
	@Column(name = "ref_related", length = 128)
	private String refRelated;	
	
	/**
	 * 검색 폼 필드 순위 
	 */
	@Column(name = "search_rank")
	private Integer searchRank;

	/**
	 * 검색 시 소팅을 위한 소팅 필드 순위 
	 */
	@Column(name = "sort_rank")
	private Integer sortRank;

	/**
	 * 검색 시 역 소팅 여부 
	 */
	@Column(name = "reverse_sort")
	private Boolean reverseSort;
	
	/**
	 * 테이블에는 존재하지 않는 필드이고 단지 화면 설정을 위해 필요한 필드일 경우 설정
	 * 예) 검색 조건 중에 DatePicker로 날짜 선택시에 from, to를 설정하는 경우에 사용
	 */
	@Column(name = "virtual_field")
	private Boolean virtualField;
	
	/**
	 * 검색 시 (서비스 호출시) 파라미터 명을 column name으로 하지 않고 다른 이름으로 넘기고 싶은 경우 여기서 설정한다. 
	 * searchName이 설정되어 있으면 서비스 호출시 파라미터 명이 searchName 값으로 되고 그렇지 않으면 파라미터 명이 name값이 된다. 
	 */
	@Column(name = "search_name", length = OrmConstants.FIELD_SIZE_NAME)
	private String searchName;	

	/**
	 * 검색 폼 조건(Where 조건)을 위한 editor 
	 */	
	@Column(name = "search_editor", length = OrmConstants.FIELD_SIZE_NAME)
	private String searchEditor;

	/**
	 * 검색 폼 조건(Where 조건)을 위한 operator 
	 */	
	@Column(name = "search_oper", length = 15)
	private String searchOper;
	
	/**
	 * 검색 폼에 기본값이 설정되어야 할 때 사용 
	 */
	@Column(name = "search_init_val", length = OrmConstants.FIELD_SIZE_VALUE_255)
	private String searchInitVal;

	/**
	 * 그리드 컬럼 표시 순서 
	 */	
	@Column(name = "grid_rank")
	private Integer gridRank;	

	/**
	 * 그리드 컬럼 에디터  
	 */	
	@Column(name = "grid_editor", length = OrmConstants.FIELD_SIZE_NAME)
	private String gridEditor;

	/**
	 * 그리드 컬럼 Format  
	 */	
	@Column(name = "grid_format", length = 128)
	private String gridFormat;
	
	/**
	 * 그리드 컬럼 Validator
	 */
	@Column(name = "grid_validator", length = OrmConstants.FIELD_SIZE_NAME)
	private String gridValidator;
	
	/**
	 * 그리드 컬럼 Width
	 */		
	@Column(name = "grid_width")
	private Integer gridWidth;
	
	/**
	 * 그리드 컬럼 정렬 - 왼쪽, 오른쪽, 중앙 정렬
	 */	
	@Column(name = "grid_align", length = 10)
	private String gridAlign;

	/**
	 * 고유값 순위 
	 * 상세 폼 필드에서 Unique Check가 필요한 경우의 Validation을 위해 설정 
	 */
	@Column(name = "uniq_rank")
	private Integer uniqRank;
	
	/**
	 * 상세 폼 필드에서 사용할 Editor
	 */
	@Column(name = "form_editor", length = OrmConstants.FIELD_SIZE_NAME)
	private String formEditor;
	
	/**
	 * 상세 폼 필드에서 사용할 Validator
	 */
	@Column(name = "form_validator", length = OrmConstants.FIELD_SIZE_NAME)
	private String formValidator;

	/**
	 * 상세 폼 필드에 데이터를 표시할 때 Formatting이 필요할 경우 혹은 pattern 정의(예:email,전화번호,주민번호 등)가 필요할 경우 format 입력  
	 */
	@Column(name = "form_format", length = 128)
	private String formFormat;

	/**
	 * 상세 폼, 그리드에 새로 값을 추가할 때 기본값이 설정되어야 할 때 사용 
	 */
	@Column(name = "def_val", length = 128)
	private String defVal;
	
	/**
	 * 상세 폼, 그리드의 값의 범위를 제한하기 위한 필드
	 * 예) 숫자형의 경우 : min:0,max:100 or min:0 or max:100 or 0..100(1부터 12)
	 * 예) 문자형의 경우 : MAN,WOMAN or A,B,C,D,E 
	 * 예) 날짜형의 경우 : min:2016-01-01,max:today or 2016-01-01..today or min:$other_column_name,max:today or $other_column_name..today
	 */
	@Column(name = "range_val", length = 128)
	private String rangeVal;
	
	/**
	 * 상세 폼, 그리드에서 저장 시에 서버에 넘어갈 필요가 없는 필드의 경우 체크 
	 */
	@Column(name = "ignore_on_save")
	private Boolean ignoreOnSave;	
	
	/**
	 * column이 code에서 선택해야 할 경우 client에서 사용할 수 있도록 코드 명, 코드 값을 리턴한다.
	 */
	@Ignore
	private List<CodeDetail> codeList;

	public ResourceColumn() {
	}

	public ResourceColumn(String entityId) {
		this.entityId = entityId;
	}
	
	public ResourceColumn(String entityId, String name) {
		this.entityId = entityId;
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
	 * @return the entityId
	 */
	public String getEntityId() {
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
	
	public ResourceRef getEntity() {
		return entity;
	}

	public void setEntity(ResourceRef entity) {
		this.entity = entity;
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
	 * @return the rank
	 */
	public Integer getRank() {
		return rank;
	}

	/**
	 * @param rank the rank to set
	 */
	public void setRank(Integer rank) {
		this.rank = rank;
	}

	/**
	 * @return the term
	 */
	public String getTerm() {
		return term;
	}

	/**
	 * @param term the term to set
	 */
	public void setTerm(String term) {
		this.term = term;
	}

	/**
	 * @return the colType
	 */
	public String getColType() {
		return colType;
	}

	/**
	 * @param colType the colType to set
	 */
	public void setColType(String colType) {
		this.colType = colType;
	}

	/**
	 * @return the size
	 */
	public Integer getColSize() {
		return colSize;
	}

	/**
	 * @param size the size to set
	 */
	public void setColSize(Integer colSize) {
		this.colSize = colSize;
	}

	/**
	 * @return the nullable
	 */
	public Boolean getNullable() {
		return nullable;
	}

	/**
	 * @param nullable the nullable to set
	 */
	public void setNullable(Boolean nullable) {
		this.nullable = nullable;
	}

	/**
	 * @return the refType
	 */
	public String getRefType() {
		return refType;
	}

	/**
	 * @param refType the refType to set
	 */
	public void setRefType(String refType) {
		this.refType = refType;
	}

	/**
	 * @return the refName
	 */
	public String getRefName() {
		return refName;
	}

	/**
	 * @param refName the refName to set
	 */
	public void setRefName(String refName) {
		this.refName = refName;
	}

	/**
	 * @return the refUrl
	 */
	public String getRefUrl() {
		return refUrl;
	}

	/**
	 * @param refUrl the refUrl to set
	 */
	public void setRefUrl(String refUrl) {
		this.refUrl = refUrl;
	}

	/**
	 * @return the refParams
	 */
	public String getRefParams() {
		return refParams;
	}

	/**
	 * @param refParams the refParams to set
	 */
	public void setRefParams(String refParams) {
		this.refParams = refParams;
	}

	/**
	 * @return the refRelated
	 */
	public String getRefRelated() {
		return refRelated;
	}

	/**
	 * @param refRelated the refRelated to set
	 */
	public void setRefRelated(String refRelated) {
		this.refRelated = refRelated;
	}

	/**
	 * @return the searchRank
	 */
	public Integer getSearchRank() {
		return searchRank;
	}

	/**
	 * @param searchRank the searchRank to set
	 */
	public void setSearchRank(Integer searchRank) {
		this.searchRank = searchRank;
	}

	/**
	 * @return the sortRank
	 */
	public Integer getSortRank() {
		return sortRank;
	}

	/**
	 * @param sortRank the sortRank to set
	 */
	public void setSortRank(Integer sortRank) {
		this.sortRank = sortRank;
	}

	/**
	 * @return the reverseSort
	 */
	public Boolean getReverseSort() {
		return reverseSort;
	}

	/**
	 * @param reverseSort the reverseSort to set
	 */
	public void setReverseSort(Boolean reverseSort) {
		this.reverseSort = reverseSort;
	}

	/**
	 * @return the virtualField
	 */
	public Boolean getVirtualField() {
		return virtualField;
	}

	/**
	 * @param virtualField the virtualField to set
	 */
	public void setVirtualField(Boolean virtualField) {
		this.virtualField = virtualField;
	}

	/**
	 * @return the searchName
	 */
	public String getSearchName() {
		return searchName;
	}

	/**
	 * @param searchName the searchName to set
	 */
	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}

	/**
	 * @return the searchEditor
	 */
	public String getSearchEditor() {
		return searchEditor;
	}

	/**
	 * @param searchEditor the searchEditor to set
	 */
	public void setSearchEditor(String searchEditor) {
		this.searchEditor = searchEditor;
	}

	/**
	 * @return the searchOper
	 */
	public String getSearchOper() {
		return searchOper;
	}

	/**
	 * @param searchOper the searchOper to set
	 */
	public void setSearchOper(String searchOper) {
		this.searchOper = searchOper;
	}

	/**
	 * @return the searchInitVal
	 */
	public String getSearchInitVal() {
		return searchInitVal;
	}

	/**
	 * @param searchInitVal the searchInitVal to set
	 */
	public void setSearchInitVal(String searchInitVal) {
		this.searchInitVal = searchInitVal;
	}

	/**
	 * @return the gridRank
	 */
	public Integer getGridRank() {
		return gridRank;
	}

	/**
	 * @param gridRank the gridRank to set
	 */
	public void setGridRank(Integer gridRank) {
		this.gridRank = gridRank;
	}

	/**
	 * @return the gridEditor
	 */
	public String getGridEditor() {
		return gridEditor;
	}

	/**
	 * @param gridEditor the gridEditor to set
	 */
	public void setGridEditor(String gridEditor) {
		this.gridEditor = gridEditor;
	}

	/**
	 * @return the gridFormat
	 */
	public String getGridFormat() {
		return gridFormat;
	}

	/**
	 * @param gridFormat the gridFormat to set
	 */
	public void setGridFormat(String gridFormat) {
		this.gridFormat = gridFormat;
	}

	/**
	 * @return the gridValidator
	 */
	public String getGridValidator() {
		return gridValidator;
	}

	/**
	 * @param gridValidator the gridValidator to set
	 */
	public void setGridValidator(String gridValidator) {
		this.gridValidator = gridValidator;
	}

	/**
	 * @return the gridWidth
	 */
	public Integer getGridWidth() {
		return gridWidth;
	}

	/**
	 * @param gridWidth the gridWidth to set
	 */
	public void setGridWidth(Integer gridWidth) {
		this.gridWidth = gridWidth;
	}

	/**
	 * @return the gridAlign
	 */
	public String getGridAlign() {
		return gridAlign;
	}

	/**
	 * @param gridAlign the gridAlign to set
	 */
	public void setGridAlign(String gridAlign) {
		this.gridAlign = gridAlign;
	}

	/**
	 * @return the uniqRank
	 */
	public Integer getUniqRank() {
		return uniqRank;
	}

	/**
	 * @param uniqRank the uniqRank to set
	 */
	public void setUniqRank(Integer uniqRank) {
		this.uniqRank = uniqRank;
	}

	/**
	 * @return the formEditor
	 */
	public String getFormEditor() {
		return formEditor;
	}

	/**
	 * @param formEditor the formEditor to set
	 */
	public void setFormEditor(String formEditor) {
		this.formEditor = formEditor;
	}

	/**
	 * @return the formValidator
	 */
	public String getFormValidator() {
		return formValidator;
	}

	/**
	 * @param formValidator the formValidator to set
	 */
	public void setFormValidator(String formValidator) {
		this.formValidator = formValidator;
	}

	/**
	 * @return the formFormat
	 */
	public String getFormFormat() {
		return formFormat;
	}

	/**
	 * @param formFormat the formFormat to set
	 */
	public void setFormFormat(String formFormat) {
		this.formFormat = formFormat;
	}

	/**
	 * @return the defVal
	 */
	public String getDefVal() {
		return defVal;
	}

	/**
	 * @param defVal the defVal to set
	 */
	public void setDefVal(String defVal) {
		this.defVal = defVal;
	}
	
	/**
	 * @return the rangeVal
	 */
	public String getRangeVal() {
		return rangeVal;
	}

	/**
	 * @param rangeVal the rangeVal to set
	 */
	public void setRangeVal(String rangeVal) {
		this.rangeVal = rangeVal;
	}

	/**
	 * @return the ignoreOnSave
	 */
	public Boolean getIgnoreOnSave() {
		return ignoreOnSave;
	}

	/**
	 * @param ignoreOnSave the ignoreOnSave to set
	 */
	public void setIgnoreOnSave(Boolean ignoreOnSave) {
		this.ignoreOnSave = ignoreOnSave;
	}

	/**
	 * @return the codeList
	 */
	public List<CodeDetail> getCodeList() {
		return codeList;
	}

	/**
	 * @param codeList the codeList to set
	 */
	public void setCodeList(List<CodeDetail> codeList) {
		this.codeList = codeList;
	}
	
}