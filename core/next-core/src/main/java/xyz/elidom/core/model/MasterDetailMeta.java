/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.model;

/**
 * Master Detail Meta Class
 * 
 * @author lyonghwan
 */
public class MasterDetailMeta {

	/**
	 * Detail Entity가 소속된 bundle
	 */
	private String bundle;
	/**
	 * Detail Entity Class
	 */
	private Class<?> detailClass;
	/**
	 * Master Reference(foreign) Key Field
	 */
	private String referenceKey;
	/**
	 * Data Property
	 */
	private String dataProperty;
	/**
	 * Master - Detail Association : one-to-one / one-to-many
	 */
	private String association;
	/**
	 * Detail Delete Strategy
	 */
	private String deleteStrategy;

	/**
	 * @return the bundle
	 */
	public String getBundle() {
		return bundle;
	}

	/**
	 * @param bundle the bundle to set
	 */
	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	/**
	 * @return the detailClass
	 */
	public Class<?> getDetailClass() {
		return detailClass;
	}

	/**
	 * @param detailClass the detailClass to set
	 */
	public void setDetailClass(Class<?> detailClass) {
		this.detailClass = detailClass;
	}

	/**
	 * @return the referenceKey
	 */
	public String getReferenceKey() {
		return referenceKey;
	}

	/**
	 * @param referenceKey the referenceKey to set
	 */
	public void setReferenceKey(String referenceKey) {
		this.referenceKey = referenceKey;
	}

	/**
	 * @return the dataProperty
	 */
	public String getDataProperty() {
		return dataProperty;
	}

	/**
	 * @param dataProperty the dataProperty to set
	 */
	public void setDataProperty(String dataProperty) {
		this.dataProperty = dataProperty;
	}

	/**
	 * @return the association
	 */
	public String getAssociation() {
		return association;
	}

	/**
	 * @param association the association to set
	 */
	public void setAssociation(String association) {
		this.association = association;
	}

	/**
	 * @return the deleteStrategy
	 */
	public String getDeleteStrategy() {
		return deleteStrategy;
	}

	/**
	 * @param deleteStrategy the deleteStrategy to set
	 */
	public void setDeleteStrategy(String deleteStrategy) {
		this.deleteStrategy = deleteStrategy;
	}
	
}
