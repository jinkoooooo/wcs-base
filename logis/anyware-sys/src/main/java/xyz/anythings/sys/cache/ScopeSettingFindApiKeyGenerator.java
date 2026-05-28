package xyz.anythings.sys.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 범위 별 설정 (domainId, scopeType, scopeName, name)에 대한 FindOne 캐쉬를 위한 KeyGenerator. 
 * {domainId}-{scopeType}-{scopeName}-{name}으로 캐쉬 키를 생성한다. 
 * 
 * @author shortstop
 */
@Component
public class ScopeSettingFindApiKeyGenerator implements KeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		String domainIdStr = null;
		try {
			domainIdStr = SysValueUtil.toString(Domain.currentDomainId());
		} catch (Exception e) {
			domainIdStr = "null";
		}
		
		return domainIdStr + SysConstants.DASH + params[0].toString() + SysConstants.DASH + params[1].toString() + SysConstants.DASH + params[2].toString();
	}
}