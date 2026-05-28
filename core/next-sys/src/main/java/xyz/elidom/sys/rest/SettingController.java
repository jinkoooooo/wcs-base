/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.rest;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Setting;
import xyz.elidom.sys.event.CacheClearEvent;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/settings")
@ServiceDesc(description="Setting Service API")
public class SettingController extends AbstractRestService {

	@Override
	protected Class<?> entityClass() {
		return Setting.class;
	}

	private final RedisTemplate<String, Object> redisTemplate;

	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Setting (Pagination) By Search Conditions")	
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page, 
			@RequestParam(name = "limit", required = false) Integer limit, 
			@RequestParam(name = "select", required = false) String select, 
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {		
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}
	
	@GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find Setting By ID or Name")
	@Cacheable(cacheNames="Setting", condition="#name != null", keyGenerator="namedFindApiKeyGenerator")
	public Setting findOne(@PathVariable("id") String id, @RequestParam(name = "name", required = false) String name) {
		if(SysConstants.SHOW_BY_NAME_METHOD.equalsIgnoreCase(id)) {
			AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_NAME, name);
			return this.selectByCondition(Setting.class, new Setting(name));
		} else {
			return this.getOne(this.entityClass(), id);
		}
	}
	
	@GetMapping(value="/find_by_name/{name}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find Setting By Name")
	@Cacheable(cacheNames="Setting", keyGenerator="namedFindApiKeyGenerator")
	public Setting findOne(@PathVariable("name") String name) {
		return this.selectByCondition(Setting.class, new Setting(name));
	}
	
	@GetMapping(value="/{id}/exist", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Check if Setting exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Before Import")
	public List<Setting> checkImport(@RequestBody List<Setting> list) {
		for (Setting item : list) {
			this.checkForImport(Setting.class, item);
		}
		
		return list;
	}
	
	@PostMapping( consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description="Create Setting")
	@CachePut(cacheNames="Setting", keyGenerator="namedUpdateApiKeyGenerator")
	public Setting create(@RequestBody Setting setting) {
		return this.createOne(setting);
	}
	
	@PutMapping(value="/{id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update Setting")
	@CachePut(cacheNames="Setting", keyGenerator="namedUpdateApiKeyGenerator")
	public Setting update(@PathVariable("id") String id, @RequestBody Setting setting) {
		return this.updateOne(setting);
	}

	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete Setting By ID")
	@CacheEvict(cacheNames="Setting", allEntries=true)
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value="/update_multiple", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple Settings at one time")
	@CacheEvict(cacheNames="Setting", allEntries=true)
	public Boolean multipleUpdate(@RequestBody List<Setting> settingList) {
		return this.cudMultipleData(this.entityClass(), settingList);
	}
	
	@PutMapping(value = "/clear_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean settingsClearCache() {
		return BeanUtil.get(DomainController.class).requestClearCache("settings");
	}

	@ApiDesc(description = "Clear Settings Cache")	
	@CacheEvict(cacheNames = "Setting", allEntries = true)
	public boolean clearCache() {
		Set<String> keys = redisTemplate.keys("Setting*");
		if (ValueUtil.isNotEmpty(keys)) {
			redisTemplate.delete(keys);
		}

		return true;
	}
	
	/**
	 * 수정1. domainId, name으로 설정값 조회 API 추가
	 * 
	 * @param domainId
	 * @param name 
	 * @return
	 */
	@Cacheable(cacheNames="Setting", key="#p0 + '-' + #p1")
	public Setting findByName(Long domainId, String name) {
		Setting condition = new Setting();
		condition.setDomainId(domainId);
		condition.setName(name);
		return this.selectByCondition(Setting.class, condition);
	}
	
    @EventListener(
        classes = CacheClearEvent.class,
        condition = "#event.targetResource == 'settings'"
    )
    public void handleCacheClear(CacheClearEvent event) {
        // 도메인 캐시 초기화 로직
        this.clearCache();
    }
}