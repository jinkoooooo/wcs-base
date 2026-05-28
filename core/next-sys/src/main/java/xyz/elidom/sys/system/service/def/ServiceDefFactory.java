/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.def;

import xyz.elidom.sys.system.service.def.basic.ServiceApi;
import xyz.elidom.sys.system.service.def.basic.ServiceDef;

/**
 * IServiceDef 구현 클래스를 생성하여 리턴하는 팩토리 클래스 
 *  
 * @author shortstop
 */
public class ServiceDefFactory {

	/**
	 * IServiceDef 객체를 생성
	 * 
	 * @return
	 */
	public static IServiceDef newServiceDef() {
		// TODO 설정에 의해서 new instance
		return new ServiceDef();
	}
	
	/**
	 * IServiceApi 객체를 생성 
	 * 
	 * @return
	 */
	public static IServiceApi newServiceApi() {
		// TODO 설정에 의해서 new instance
		return new ServiceApi();
	}
}
