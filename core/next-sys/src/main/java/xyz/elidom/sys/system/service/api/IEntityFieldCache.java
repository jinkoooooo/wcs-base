/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 모든 Entity Class의 키 필드와 Reference 필드를 Cache에 관리한다. 
 *   
 * @author shortstop
 */
public interface IEntityFieldCache {

	/**
	 * basePackage에 있는 모든 entity를 파싱한다.
	 * 
	 * @param basePackage
	 */
	public void scanEntityFieldsByBasePackage(String basePackage);
	
	/**
	 * entityClass의 필드를 분석하여 Caching한다. 
	 * 
	 * @param entityClass
	 */
	public void parseEntityFields(Class<?> entityClass);
	
	/**
	 * entityClass의 키 필드를 리턴한다.
	 * 
	 * @param entityClass
	 * @return
	 */
	public List<Field> getKeyFields(Class<?> entityClass);
	
	/**
	 * entityClass가 복합키가 아닌 경우 키를 리턴한다. entityClass가 복합키인 경우 런타임 에러가 발생한다.
	 * 
	 * @param entityClass
	 * @return
	 */
	public Field getSingleKeyField(Class<?> entityClass);
	
	/**
	 * entityClass의 키 타입을 리턴한다.
	 * 
	 * @param entityClass
	 * @return
	 */
	public List<Class<?>> getKeyTypes(Class<?> entityClass);
	
	/**
	 * entityClass가 복합키가 아닌 경우 키 타입을 리턴한다. entityClass가 복합키인 경우 런타임 에러가 발생한다.
	 * 
	 * @param entityClass
	 * @return
	 */
	public Class<?> getSingleKeyType(Class<?> entityClass);
	
	/**
	 * entityClass의 키가 Multiple Key인지를 확인한다.
	 * 
	 * @param entityClass
	 * @return
	 */
	public boolean hasMultiKey(Class<?> entityClass);
	
	/**
	 * entityClass의 키 필드를 캐쉬에 Put
	 * 
	 * @param entityClass
	 * @param keyFields
	 */
	public void setKeyFields(Class<?> entityClass, List<Field> keyFields);
	
	/**
	 * entityClass로 Reference Field들을 찾아 리턴한다.
	 * 
	 * @param entityClass
	 * @return
	 */
	public List<Field> getRefFields(Class<?> entityClass);
	
	/**
	 * entityClass로 Reference Field들을 설정한다.
	 * 
	 * @param entityClass
	 * @param refFields
	 */
	public void setRefFields(Class<?> entityClass, List<Field> refFields);
	
}
