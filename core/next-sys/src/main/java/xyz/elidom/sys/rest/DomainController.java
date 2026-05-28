/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.rest;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
import org.springframework.web.client.RestTemplate;

import xyz.elidom.dbist.dml.Order;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.event.CacheClearEvent;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/domains")
@ServiceDesc(description="Domain Service API")
public class DomainController extends AbstractRestService {
	
	/**
	 * 기본 소팅 조건 - '[{\"field\": \"name\", \"ascending\": true}]'
	 */
	private static final String DEFAULT_SORT_COND = "[{\"field\": \"name\", \"ascending\": true}]";
	
	@Value("${redis.was.urls}")
	private String[] redisWasUrls;
	/**
	 * 캐쉬 리셋 요청 URL 
	 */
	private String clearCacheReqUrl = "http://%s/rest/domains/clear_cache/%s";
	private final CacheManager cacheManager;
	private final RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

    @Override
	protected Class<?> entityClass() {
		return Domain.class;
	}
	
	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Domains (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page, 
			@RequestParam(name = "limit", required = false) Integer limit, 
			@RequestParam(name = "select", required = false) String select, 
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		if(ValueUtil.isEmpty(sort)) {
			sort = DEFAULT_SORT_COND;
		}
		
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}
	
	@GetMapping(value="/search", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Domains (Pagination) By Authorization")
	public Page<?> searchByAuth(
			@RequestParam(name = "page", required = false) Integer page, 
			@RequestParam(name = "limit", required = false) Integer limit, 
			@RequestParam(name = "select", required = false) String select, 
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		
		if(ValueUtil.isNotEmpty(User.currentUser()) && ValueUtil.isEqual(User.currentUser().getSuperUser(), true)) {
			return this.search(this.entityClass(), page, limit, select, sort, query);
			
		} else {
			Page<Domain> result = new Page<Domain>();
			result.setIndex(1);
			result.setTotalSize(1);
			result.setList(ValueUtil.toList(Domain.currentDomain()));
			return result;
		}
	}
	
	@GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find one Domain by ID")
	@Cacheable(cacheNames="Domain", condition="#name == null", key="#p0")
	public Domain findOne(@PathVariable("id") Object id, @RequestParam(required = false) String name) {
		if(SysConstants.SHOW_BY_NAME_METHOD.equalsIgnoreCase(id.toString())) {
			return this.findByName(name); 
		} else {
			return this.getOne(true, this.entityClass(), ValueUtil.toLong(id));
		}
	}
	
	@GetMapping(value="/current_domain", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find current domain")
	public Domain findCurrentDomain() {
		return BeanUtil.get(DomainController.class).findOne(Domain.currentDomain().getId(), null);
	}	
	
	@GetMapping(value="/show_by_name/{name}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find one Domain by name")
	@Cacheable(cacheNames="Domain", key="#p0")
	public Domain findByName(String name) {
		AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_NAME, name);
		return this.selectByCondition(Domain.class, new Domain(name));
	}
	
	@GetMapping(value="/show_by_url/{name}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find one Domain by name")
	@Cacheable(cacheNames="Domain", key="#p0")
	public Domain findBySubdomain(String subdomain) {
		AssertUtil.assertNotEmpty("terms.label.subdomain", subdomain);
		Domain cond = new Domain();
		cond.setSubdomain(subdomain);
		return this.selectByCondition(Domain.class, cond);
	}
	
	@GetMapping(value="/{id}/exist", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Check if Domain exists By ID")
	public Boolean isExist(@PathVariable("id") Long id) {
		return this.isExistOne(this.entityClass(), id);
	}
		
	@PostMapping( consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description="Create Domain")
	@CacheEvict(cacheNames="Domain", allEntries=true)
	public Domain create(@RequestBody Domain domain) {
		return this.createOne(domain);
	}
	
	@PutMapping(value="/{id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update Domain")
	@CacheEvict(cacheNames="Domain", allEntries=true)
	public Domain update(@PathVariable("id") Long id, @RequestBody Domain domain) {
		domain = this.updateOne(domain);
		
		if(domain.getSystemFlag()) {
			Domain.resetSystemDomain();
		}
		
		return domain;
	}

	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete Domain By ID")
	@CacheEvict(cacheNames="Domain", allEntries=true)
	public boolean delete(@PathVariable("id") Long id) {
		ThrowUtil.newNotSupportedMethodYet();
		return false;
	}
	
	@PostMapping(value="/update_multiple", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple domains at one time")
	@CacheEvict(cacheNames="Domain", allEntries=true)
	public Boolean multipleUpdate(@RequestBody List<Domain> domainList) {		
		DomainController ctrl = BeanUtil.get(DomainController.class);
		
		for (Domain d : domainList) {
			if (ValueUtil.isEqual(d.getCudFlag_(), SysConstants.CUD_FLAG_UPDATE)) {
				ctrl.update(d.getId(), d);
			}			
		}
		
		return true;		
	}
	
	@GetMapping(value="/list", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Domain List")
	@Cacheable(cacheNames="Domain")
	public List<Domain> domainList() {
		Query query = new Query();
		query.setSelect(ValueUtil.newStringList(OrmConstants.ENTITY_FIELD_ID, OrmConstants.ENTITY_FIELD_NAME, OrmConstants.ENTITY_FIELD_DESCRIPTION));
		query.addOrder(new Order(OrmConstants.ENTITY_FIELD_ID, true));
		return queryManager.selectList(Domain.class, query);
	}
	
	@GetMapping(value="/show_by_port/{port}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find one Domain by port")
	@Cacheable(cacheNames="Domain", key="#p0")
	public Domain findByPort(Integer port) {
		AssertUtil.assertNotEmpty("terms.label.site_port", port);
		Domain cond = new Domain();
		cond.setSitePort(port);
		return this.selectByCondition(Domain.class, cond);
	}
	
	@PutMapping(value = "/clear_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean domainClearCache() {
		return BeanUtil.get(DomainController.class).requestClearCache("domain");
	}
	
	@CacheEvict(cacheNames = "Domain", allEntries = true)
	public boolean clearCache() {
		Set<String> keys = redisTemplate.keys("Domain*");
		if (ValueUtil.isNotEmpty(keys)) {
			redisTemplate.delete(keys);
		}

		return true;
	}
	
	public boolean requestClearCache(String targetResource) {
		// ① HttpClient 5.x 객체 생성
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3000);
		factory.setReadTimeout(3000);
		
		RestTemplate rest = new RestTemplate(factory);

		for(String serverAddr : this.redisWasUrls) {
			rest.put(this.buildRequestUri(serverAddr,targetResource), null);
		}
		
		return true;
	}
	
	public URI buildRequestUri(String serverAddr, String targetResource) {
	    return URI.create(String.format(this.clearCacheReqUrl, serverAddr, targetResource));
	}
	
    @EventListener(
        classes = CacheClearEvent.class,
        condition = "#event.targetResource == 'domain'"
    )
    public void handleCacheClear(CacheClearEvent event) {
        // 도메인 캐시 초기화 로직
        this.clearCache();
    }
    
    @PutMapping(value = "/clear_cache/{target_resource}", produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean clearTargetCache(@PathVariable("target_resource") String targetResource) {
    	eventPublisher.publishEvent(new CacheClearEvent(targetResource));
        return true;
    }

	@PutMapping(value = "/clear_all_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean clearAllCache() {
		String[] resources = {"domain", "terminology", "settings", "messages", "menu", "resource", "code"};
		for (String resource : resources) {
			this.requestClearCache(resource);
		}

		return true;
	}

	@GetMapping("/cache_names")
	public Collection<String> getCacheNames() {
		return cacheManager.getCacheNames();
	}
}