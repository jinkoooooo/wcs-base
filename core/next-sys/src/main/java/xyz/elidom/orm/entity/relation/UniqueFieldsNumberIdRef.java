/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.relation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;

import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.ValueUtil;

/**
 * Annotation UniqueFields가 존재하는 EntityReference
 * 
 * @author shortstop
 */
public abstract class UniqueFieldsNumberIdRef implements IdFindable {
	
	/**
	 * set Id
	 * 
	 * @param id
	 */
	public abstract void setId(Long id);
	
	/**
	 * get Id
	 * 
	 * @return
	 */
	public abstract Long getId();
	
	@Override
	public Object findAndSetId() {
		Class<?> entityClass = this.getClass();
		Annotation annotation = entityClass.getAnnotation(Table.class);
		Map<String, Object> tableInfoMap = AnnotationUtils.getAnnotationAttributes(annotation);
		StringBuffer sql = new StringBuffer("select id from ").append(tableInfoMap.get("name")).append(" where 1 = 1 ");
		
		String uniqueFields = (String)tableInfoMap.get("uniqueFields");
		if(ValueUtil.isEmpty(uniqueFields)) {
			uniqueFields = (String)tableInfoMap.get("meaningfulFields");
		}
		
		if(ValueUtil.isEmpty(uniqueFields)) {
			return null;
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		IQueryManager queryMan = BeanUtil.get(IQueryManager.class);
		String[] uniqFieldArr = uniqueFields.split(",");
		for(String uniqField : uniqFieldArr) {
			sql.append(" and ").append(uniqField).append("= :").append(uniqField);
			Object value = ValueUtil.isEqual("domainId", uniqField) ? Domain.currentDomainId() : ClassUtil.getFieldValue(this, uniqField);
			params.put(uniqField, value);
		}
		
		this.setId(queryMan.selectBySql(sql.toString(), params, Long.class));
		return this.getId();
	}
}