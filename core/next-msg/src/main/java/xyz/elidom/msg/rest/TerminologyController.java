/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.msg.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

//import xyz.elidom.core.model.BaseResponse;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.msg.entity.Terminology;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.event.CacheClearEvent;
import xyz.elidom.sys.model.BaseResponse;
import xyz.elidom.sys.rest.DomainController;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/terminologies")
@ServiceDesc(description = "Terminology Service API")
public class TerminologyController extends AbstractRestService {
	
	/**
	 * 로케일 쿼리 
	 */
	private static final String LOCALE_QUERY = "SELECT distinct(LOCALE) locale FROM terminologies";
	
	/**
	 * 용어 조회 쿼리 
	 */
	private static final String TERM_QUERY = "SELECT CATEGORY, NAME, DISPLAY FROM terminologies WHERE DOMAIN_ID = :domainId AND LOCALE = :locale";

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	protected Class<?> entityClass() {
		return Terminology.class;
	}
	
	@GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Load all terminologies")
	public void all() {
		List<String> locales = this.queryManager.selectListBySql(LOCALE_QUERY, null, String.class, 0, 0);
		TerminologyController ctrl = BeanUtil.get(TerminologyController.class);

		// locale별 terminologies 추출
		for (String locale : locales) {
			ctrl.resource(locale);
		}		
	}
	
	@GetMapping(value = "/resource/{locale}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Resource List")
	@Cacheable(cacheNames = "Terminology", key = "T(xyz.elidom.sys.entity.Domain).currentDomain().getId() + #p0")
	public Map<String, Object> resource(@PathVariable("locale") String locale) {
		Map<String, Object> termsMap = new HashMap<String, Object>();
		List<Terminology> terms = this.queryManager.selectListBySql(TERM_QUERY, SysValueUtil.newMap("domainId,locale", Domain.currentDomain().getId(), locale), Terminology.class, 0, 0);

		for (Terminology term : terms) {
			termsMap.put(term.getCategory() + "." + term.getName(), term.getDisplay());
		}

		return SysValueUtil.newMap(locale, termsMap);
	}
		
	@SuppressWarnings("unchecked")
	@ApiDesc(description="Find terminology by locale and category and name")
	@Cacheable(cacheNames="Terminology", key="T(xyz.elidom.sys.entity.Domain).currentDomain().getId() + #p0 + #p1")
	public String findBy(String locale, String key) {
		TerminologyController ctrl = BeanUtil.get(TerminologyController.class);
		Map<String, Object> termsMap = (Map<String, Object>)ctrl.resource(locale).get(locale);
		return (termsMap.containsKey(key)) ? (String)termsMap.get(key) : null;
	}

	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Dynamic Templates (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page, 
			@RequestParam(name = "limit", required = false) Integer limit, 
			@RequestParam(name = "select", required = false) String select, 
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}
	
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find one Terminology by ID")
	public Terminology findOne(@PathVariable("id") String id) {
		return this.getOne(true, this.entityClass(), id);
	}

	@GetMapping(value = "/{id}/exist", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check if Terminology exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Before Import")
	public List<Terminology> checkImport(@RequestBody List<Terminology> list) {
		for (Terminology item : list) {
			this.checkForImport(Terminology.class, item);
		}
		
		return list;
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create Terminology")
	public Terminology create(@RequestBody Terminology terminology) {
		return this.createOne(terminology);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update Terminology")
	public Terminology update(@PathVariable("id") String id, @RequestBody Terminology terminology) {
		return this.updateOne(terminology);
	}

	@DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete Terminology")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete Multiple Terminologies at one time")
	public Boolean multipleUpdate(@RequestBody List<Terminology> terminologyList) {
		Boolean result = this.cudMultipleData(this.entityClass(), terminologyList);
		
		if(result) {
			BeanUtil.get(TerminologyController.class).clearCache();
		}
		
		return result;
	}
	
	@PostMapping(value="/sync_terms/to_other_domains", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Syncronize terminologies to other domains")
	public BaseResponse syncTemsToOtherDomains() {
		// 1. 쿼리 
		Long domainId = Domain.currentDomainId();
		Map<String, Object> params = SysValueUtil.newMap("currentDomainId,creatorId", domainId, User.currentUser().getId());
		StringBuffer script = new StringBuffer();
		script.append("insert into terminologies(");
		script.append("	id, name, category, locale, display, domain_id, creator_id, updater_id, created_at, updated_at");
		script.append(") select");
		script.append("		uuid_generate_v4(), name, category, locale, display, :targetDomainId, :creatorId, :creatorId, now(), now()");
		script.append("	from");
		script.append("		terminologies");
		script.append(" where");
		script.append("		domain_id = :currentDomainId");
		script.append("		and (name, category, locale) in (");
		script.append("			select name, category, locale from terminologies where domain_id = :currentDomainId");
		script.append("			EXCEPT ");
		script.append("			select name, category, locale from terminologies where domain_id = :targetDomainId");
		script.append("		)");
		
		// 2. Domain 조회
		List<Domain> domains = this.queryManager.selectList(Domain.class, new Domain());
		
		// 3. 도메인 별로 동일한 이름의 엔티티 & 엔티티 컬럼 제거
		for(Domain domain : domains) {
			if(SysValueUtil.isNotEqual(domainId, domain.getId())) {
				params.put("targetDomainId", domain.getId());
				this.queryManager.executeBySql(script.toString(), params);
			}
		}
		
		// 5. Clear Cache Resource Column
		BeanUtil.get(TerminologyController.class).clearCache();
		
		// 6. 리턴 
		return new BaseResponse(true, "ok");
	}
	
	@PutMapping(value = "/clear_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean terminologyClearCache() {
		return BeanUtil.get(DomainController.class).requestClearCache("terminology");
	}

	@CacheEvict(cacheNames = "Terminology", allEntries = true)
	public boolean clearCache() {
		Set<String> keys = redisTemplate.keys("Terminology*");
		if (ValueUtil.isNotEmpty(keys)) {
			redisTemplate.delete(keys);
		}

		return true;
	}
	
	@SuppressWarnings("unchecked")
	@ApiDesc(description="Find terminology by locale and category and name")
	@Cacheable(cacheNames="Terminology", key="#p0 + #p1 + #p2")
	public String findBy(Long domainId, String locale, String key) {
		TerminologyController ctrl = BeanUtil.get(TerminologyController.class);
		Map<String, Object> termsMap = (Map<String, Object>)ctrl.resource(domainId, locale).get(locale);
		return (termsMap.containsKey(key)) ? (String)termsMap.get(key) : null;
	}
	
	@ApiDesc(description = "Resource List")
	@Cacheable(cacheNames = "Terminology", key = "#p0 + #p1")
	public Map<String, Object> resource(Long domainId, String locale) {
		Map<String, Object> termsMap = new HashMap<String, Object>();
		List<Terminology> terms = this.queryManager.selectListBySql(TERM_QUERY, SysValueUtil.newMap("domainId,locale", domainId, locale), Terminology.class, 0, 0);
 
		for (Terminology term : terms) {
			termsMap.put(term.getCategory() + "." + term.getName(), term.getDisplay());
		}

		return SysValueUtil.newMap(locale, termsMap);
	}
	
    @EventListener(
        classes = CacheClearEvent.class,
        condition = "#event.targetResource == 'terminology'"
    )
    public void handleCacheClear(CacheClearEvent event) {
        // 도메인 캐시 초기화 로직
        this.clearCache();
    }
}