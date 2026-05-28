/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.entity.Domain;

/**
 * domainId, name이 unique한 Resource를 조회하는 (FindOne) 경우 
 * findOne(@PathVariable("id") Object id, @RequestParam(required = false) String name)에 해당하는 API에서 Cache 키를 생성하는 CacheKeyGenerator
 * {domainId}-{name} 으로 캐쉬 키를 생성한다. 
 * 
 * @author shortstop
 */
@Component
public class NamedFindApiKeyGenerator implements KeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		Domain domain = null;
		try {
			domain = Domain.currentDomain();
		} catch (Exception e) {
			e.printStackTrace(); 
		}
		return ((domain != null) ? domain.getId() : "null") + "-" + params[1].toString();
	}
}