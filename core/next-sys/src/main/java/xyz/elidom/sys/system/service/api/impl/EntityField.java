/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * EntityField의 Key Field와 Reference Field 매핑 모델
 * 
 * @author shortstop
 */
public class EntityField {

	/**
	 * key field of entity class
	 */
	private List<Field> keyFields;
	
	/**
	 * Key types of entity class
	 */
	private List<Class<?>> keyTypes;

	/**
	 * refrence field of entity class
	 */
	private List<Field> refFields;
		
	public EntityField() {
	}
	
	public EntityField(List<Field> keyFields, List<Field> refFields) {
		this.setKeyFields(keyFields);
		this.setRefFields(refFields);
	}

	/**
	 * @return the keyFields
	 */
	public List<Field> getKeyFields() {
		return keyFields;
	}

	/**
	 * @param keyFields
	 *            the keyFields to set
	 */
	public void setKeyFields(List<Field> keyFields) {
		if(keyFields != null) {
			this.keyFields = keyFields;
			
			this.keyTypes = new ArrayList<Class<?>>();
			for(int i = 0 ; i < keyFields.size() ; i++) {
				this.keyTypes.add(keyFields.get(i).getType());
			}
		}
	}

	/**
	 * @return the keyTypes
	 */
	public List<Class<?>> getKeyTypes() {
		return keyTypes;
	}

	/**
	 * @param keyTypes the keyTypes to set
	 */
	public void setKeyTypes(List<Class<?>> keyTypes) {
		this.keyTypes = keyTypes;
	}

	/**
	 * @return the refFields
	 */
	public List<Field> getRefFields() {
		return refFields;
	}

	/**
	 * @param refFields
	 *            the refFields to set
	 */
	public void setRefFields(List<Field> refFields) {
		this.refFields = refFields;
	}

}
