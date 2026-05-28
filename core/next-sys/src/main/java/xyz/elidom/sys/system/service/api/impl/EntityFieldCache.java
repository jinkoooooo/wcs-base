/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.system.annotation.entity.RefInclude;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.util.ClassUtil;

/**
 * IEntityFieldCache 기본 구현 
 * 
 * @author shortstop
 */
@Component
public class EntityFieldCache implements IEntityFieldCache {

	/**
	 * Cache For Entity Class & Field 모델 
	 */
	private Map<Class<?>, EntityField> cache = new ConcurrentHashMap<Class<?>, EntityField>();
	
	@Override
	public void scanEntityFieldsByBasePackage(String basePackage) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(Table.class));
		
		for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
			String beanName = bd.getBeanClassName();
			Class<?> entityClass = ClassUtil.forName(beanName);
			this.parseEntityFields(entityClass);
		}
	}
	
	@Override
	public void parseEntityFields(Class<?> entityClass) {
		List<Field> keyFields = new ArrayList<Field>(1);
		ClassUtil.extractAllAnnotatedFields(keyFields, entityClass, PrimaryKey.class);
		
		List<Field> refFields = new ArrayList<Field>(3);
		ClassUtil.extractAllAnnotatedFields(refFields, entityClass, RefInclude.class);
		
		this.setKeyFields(entityClass, keyFields);
		this.setRefFields(entityClass, refFields);
	}
	
	@Override
	public List<Field> getKeyFields(Class<?> entityClass) {
		if(!this.cache.containsKey(entityClass)) {
			this.parseEntityFields(entityClass);
		}
		
		EntityField ef = this.cache.get(entityClass);
		return ef.getKeyFields();
	}

	@Override
	public boolean hasMultiKey(Class<?> entityClass) {
		List<Field> keyFields = this.getKeyFields(entityClass);
		return keyFields.size() > 1;
	}
	
	@Override
	public Field getSingleKeyField(Class<?> entityClass) {
		return this.getKeyFields(entityClass).get(0);
	}	

	@Override
	public void setKeyFields(Class<?> entityClass, List<Field> keyFields) {
		EntityField ef = this.cache.get(entityClass);
		
		if(ef == null) {
			ef = new EntityField(keyFields, null);
			this.cache.put(entityClass, ef);
		} else {
			ef.setKeyFields(keyFields);
		}
	}

	@Override
	public List<Field> getRefFields(Class<?> entityClass) {
		if(!this.cache.containsKey(entityClass)) {
			this.parseEntityFields(entityClass);
		}
		
		EntityField ef = this.cache.get(entityClass);
		return ef.getRefFields();
	}

	@Override
	public void setRefFields(Class<?> entityClass, List<Field> refFields) {
		EntityField ef = this.cache.get(entityClass);
		
		if(ef == null) {
			ef = new EntityField(null, refFields);
			this.cache.put(entityClass, ef);
		} else {
			ef.setRefFields(refFields);
		}
	}

	@Override
	public List<Class<?>> getKeyTypes(Class<?> entityClass) {
		EntityField ef = this.cache.get(entityClass);
		return ef.getKeyTypes();
	}

	@Override
	public Class<?> getSingleKeyType(Class<?> entityClass) {
		EntityField ef = this.cache.get(entityClass);
		return ef.getKeyTypes().get(0);
	}

}
