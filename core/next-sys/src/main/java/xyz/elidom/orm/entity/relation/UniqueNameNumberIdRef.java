/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.relation;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;

import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * name 값으로 auto-increment형 id를 조회하는 참조 엔티티
 * 
 * @author shortstop
 */
public abstract class UniqueNameNumberIdRef implements IUniqueNameRef {
		
	/**  
	 * get name
	 */
	public abstract String getName();
	
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
		Object domainId = Domain.currentDomain() != null ? Domain.currentDomain().getId() : null;
		
		if(ValueUtil.isNotEmpty(domainId) && ValueUtil.isNotEmpty(this.getName())) {
			Class<?> entityClass = this.getClass();
			Annotation annotation = entityClass.getAnnotation(Table.class);
			Map<String, Object> tableInfoMap = AnnotationUtils.getAnnotationAttributes(annotation);
			String sql = "select id from " + tableInfoMap.get("name") + " where domain_id = :domainId and name = :name";
			IQueryManager queryMan = BeanUtil.get(IQueryManager.class);
			Map<String, Object> params = ValueUtil.newMap("domainId,name", Domain.currentDomain().getId(), this.getName()); 
			this.setId(queryMan.selectBySql(sql, params, Long.class));
		}
		
		return this.getId();
	}
}
