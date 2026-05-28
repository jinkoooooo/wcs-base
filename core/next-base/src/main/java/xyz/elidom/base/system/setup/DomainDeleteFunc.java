package xyz.elidom.base.system.setup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.elidom.base.util.ResourceUtil;
import xyz.elidom.core.CoreConstants;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;

/**
 * 도메인 삭제 기능 
 * 
 * @author shortstop
 */
public class DomainDeleteFunc {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(DomainDeleteFunc.class);
	
	/**
	 * query manager
	 */
	private IQueryManager queryManager;
	
	/**
	 * 생성자 
	 * 
	 * @param queryManager
	 */
	public DomainDeleteFunc(IQueryManager queryManager) {
		this.queryManager = queryManager;
	}
	
	/**
	 * 도메인 및 도메인 관련 데이터 삭제
	 * 
	 * @param id
	 * @param deleteParams
	 * @return
	 */
	public Boolean deleteDomain(Long id, Map<String, Object> deleteParams) {
		// 1. 도메인 조회 
		Domain domain = this.queryManager.select(true, new Domain(id));
		
		// 2. 시스템 도메인 인지 체크 
		if(domain.getSystemFlag()) {
			throw ThrowUtil.newCannotDeleteSystemDomain(domain.getName());
		}
		
		// 3. original domain을 keep
		Domain originalDomain = Domain.currentDomain();
		
		// 4. 삭제하려는 도메인이 current domain이면 삭제할 수 없다.
		if(originalDomain.getId() == id) {
			throw new ElidomValidationException("Can not delete current domain!");
		}
		
		// 5. Current Domain 설정 
		SessionUtil.setAttribute(CoreConstants.CURRENT_DOMAIN, domain);
		
		try {
			// 6. Domain 정보로 모든 Entity 데이터 삭제
			this.deleteObjectsByDomain(domain);
		
			// 7. 도메인 삭제 
			this.queryManager.delete(domain);
			
		} catch (RuntimeException e) {
			throw e;
			
		} finally {
			// 8. Current Domain은 시스템 도메인으로 원복 
			SessionUtil.setAttribute(CoreConstants.CURRENT_DOMAIN, originalDomain == null ? Domain.systemDomain() : originalDomain);			
		}
		
		return true;
	}
	
	/**
	 * domain 정보로 모든 엔티티 삭제 
	 * 
	 * @param domain
	 */
	private void deleteObjectsByDomain(Domain domain) {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		List<IModuleProperties> orderedModules = configSet.allOrderedModules();
		// 삭제시에는 가장 말단 모듈부터 삭제한다.
		Collections.reverse(orderedModules);
		// 완료된 패키지 리스트 
		List<String> donePackagePathList = new ArrayList<String>();
				
		// 모든 모듈을 순회하면서 
		for(IModuleProperties module : orderedModules) {
			String entityPackagePath = module.getScanEntityPackage();
			if(donePackagePathList.contains(entityPackagePath)) {
				continue;
			}
			
			Map<String, Object> paramMap = SysValueUtil.newMap(OrmConstants.ENTITY_FIELD_DOMAIN_ID, domain.getId());
			List<Class<?>> entityClassList = ResourceUtil.getEntityClassesByBundle(module.getName());
			
			for(Class<?> entityClass : entityClassList) {
				// Domain 클래스가 아니고 클래스가 domain_id 필드를 가지고 있으면 domain_id가 삭제하려는 도메인의 id와 같은 데이터를 삭제 
				if(SysValueUtil.isNotEqual(entityClass.getName(), Domain.class.getName()) && ClassUtil.getField(entityClass, OrmConstants.ENTITY_FIELD_DOMAIN_ID) != null) {
					xyz.elidom.dbist.annotation.Table table = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);
					String tableName = table.name();
					int count = this.queryManager.executeBySql("delete from " + tableName + " where domain_id = :domainId", paramMap);
					logger.info("[" + count + "] record count of table [" + tableName + "] deleted successfully!");
				}
			}
 		}
	}
	
}
