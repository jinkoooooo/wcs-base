/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.ClassUtil;

/**
 * domainId, name이 unique한 Resource를 업데이트하는 (Update) 경우 
 * update(@PathVariable("id") Long id, @RequestBody Entity object)에 해당하는 API에서 Cache 키를 생성하는 CacheKeyGenerator
 * {domainId}-{name} 으로 캐쉬 키를 생성한다. 
 * 
 * @author shortstop
 */
@Component
public class NamedUpdateApiKeyGenerator implements KeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		Domain domain = Domain.currentDomain();
		Object obj = params[params.length - 1];
		Object nameValue = ClassUtil.getFieldValue(obj, "name");
		return ((domain != null) ? domain.getId() : "null") + "-" + nameValue.toString();
	}
}