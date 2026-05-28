/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.def;

import java.util.List;

/**
 * Service Object의 데이터 구조 정의 인터페이스 
 * 
 * @author shortstop
 */
public interface IServiceDef {

	/**
	 * 모듈명
	 * 
	 * @return
	 */
	public String getModule();

	/**
	 * 모듈명 설정
	 * 
	 * @param module
	 */
	public void setModule(String module);

	/**
	 * 서비스 아이디 - 서비스 URL에서 method명이 빠진 URL
	 * 
	 * @return
	 */
	public String getId();

	/**
	 * 서비스 아이디 - 서비스 URL에서 method명이 빠진 URL
	 * 
	 * @param id
	 */
	public void setId(String id);

	/**
	 * Service Bean 
	 * 
	 * @return
	 */
	public Object getBean();
	
	/**
	 * Service Bean 설정 
	 * 
	 * @param bean
	 */
	public void setBean(Object bean);
	
	/**
	 * Service Bean Class
	 * 
	 * @return
	 */
	public Class<?> getBeanClass();
	
	/**
	 * Service Bean Class 설정 
	 * 
	 * @param beanClass
	 */
	public void setBeanClass(Class<?> beanClass);
	
	/**
	 * 서비스 빈 클래스 명
	 * 
	 * @return
	 */
	public String getBeanClassName();

	/**
	 * 서비스 빈 클래스 명 설정
	 * 
	 * @param beanClassName
	 */
	public void setBeanClassName(String beanClassName);

	/**
	 * 서비스 이름 - 사용자가 annotation에 설정한 서비스 이름. annotation이 없는 경우 서비스 빈 이름이 된다.
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * 서비스 이름 - 사용자가 annotation에 설정한 서비스 이름. annotation이 없는 경우 서비스 빈 이름이 된다.
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * 서비스 설명 - 사용자가 annotation에 설정한 서비스 설명
	 * 
	 * @return
	 */
	public String getDescription();

	/**
	 * 서비스 설명 설정 - 사용자가 annotation에 설정한 서비스 설명
	 * 
	 * @param description
	 */
	public void setDescription(String description);
	
	/**
	 * 서비스 클래스가 외부에 노출하는 API 개수를 리턴
	 * 
	 * @return
	 */
	public int getApiCount();
	
	/**
	 * 서비스 클래스가 외부에 노출하는 API 개수를 설정 
	 * 
	 * @param apiCount
	 */
	public void setApiCount(int apiCount);
	
	/**
	 * API Scan이 모두 완료되었는지 여부 
	 * 
	 * @return
	 */
	public boolean isApiScannedAll();

	/**
	 * 서비스 API (서비스 빈의 노출된 메소드에 대응) 리스트
	 * 
	 * @return
	 */
	public List<IServiceApi> getApiList();

	/**
	 * 서비스 API (서비스 빈의 노출된 메소드에 대응) 리스트 설정
	 * 
	 * @param apiList
	 */
	public void setApiList(List<IServiceApi> apiList);

	/**
	 * 서비스 API 리스트에 추가
	 * 
	 * @param api
	 */
	public void addApi(IServiceApi api);
	
	/**
	 * apiId로 IServiceApi를 찾는다. 
	 * 
	 * @param apiId
	 * @return
	 */
	public IServiceApi getApiById(String apiId);
	
	/**
	 * methodName으로 IServiceApi를 찾는다
	 * 
	 * @param methodName
	 * @return
	 */
	public IServiceApi getApiByMethod(String methodName);
	
}
