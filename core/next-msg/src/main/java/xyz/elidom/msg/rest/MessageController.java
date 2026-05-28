/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
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

import xyz.elidom.dbist.dml.Page;
import xyz.elidom.msg.entity.Message;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.event.CacheClearEvent;
import xyz.elidom.sys.rest.DomainController;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/messages")
@ServiceDesc(description = "Message Service API")
public class MessageController extends AbstractRestService {
	
	/**
	 * 로케일 쿼리 
	 */
	private static final String LOCALE_QUERY = "SELECT distinct(LOCALE) locale FROM messages";
	/**
	 * MESSAGE QUERY
	 */
	private static final String MSG_QUERY = "SELECT NAME, DISPLAY FROM messages WHERE DOMAIN_ID = :domainId AND LOCALE = :locale";

	@Override
	protected Class<?> entityClass() {
		return Message.class;
	}

	private final RedisTemplate<String, Object> redisTemplate;

	@GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find all messages")
	public Map<String, Object> all() {
		Map<String, Object> messageMap = new HashMap<String, Object>();
		List<String> locales = this.queryManager.selectListBySql(LOCALE_QUERY, null, String.class, 0, 0);
		MessageController ctrl = BeanUtil.get(MessageController.class);

		// locale별 message 추출
		for (String locale : locales) {
			messageMap.put(locale, ctrl.allByLocale(locale));
		}
		
		return messageMap;
	}

	@GetMapping(value = "/all/{locale}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find All Messages By Locale")
	@Cacheable(cacheNames = "Message", key = "#p0")
	public Map<String, String> allByLocale(@PathVariable("locale") String locale) {
		Map<String, String> messageMap = new HashMap<String, String>();
		Map<String, Object> params = SysValueUtil.newMap("domainId,locale", Domain.systemDomain().getId(), locale);
		List<Message> list = this.queryManager.selectListBySql(MSG_QUERY, params, Message.class, 0, 0);

		for (Message msg : list) {
			messageMap.put(msg.getName(), msg.getDisplay());
		}
		
		return messageMap;
	}

	@GetMapping(value = "/{locale}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find message by locale and name")
	@Cacheable(cacheNames = "Message", key = "#p0 + #p1")
	public String findBy(@PathVariable("locale") String locale, @PathVariable("name") String name) {
		Map<String, String> messageMap = BeanUtil.get(MessageController.class).allByLocale(locale);
		return (messageMap.containsKey(name)) ? messageMap.get(name) : null;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search Messages (Pagination) By Search Conditions")
	public Page<?> index(@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit, 
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort, 
			@RequestParam(name = "query", required = false) String query) {
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "find one Message by ID")
	public Message findOne(@PathVariable("id") String id) {
		return this.getOne(true, this.entityClass(), id);
	}	

	@GetMapping(value = "/{id}/exist", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Message is exist by ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Before Import")
	public List<Message> checkImport(@RequestBody List<Message> list) {
		for (Message item : list) {
			this.checkForImport(Message.class, item);
		}
		
		return list;
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create Message")
	public Message create(@RequestBody Message Message) {
		return this.createOne(Message);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update Message")
	public Message update(@PathVariable("id") String id, @RequestBody Message Message) {
		return this.updateOne(Message);
	}

	@DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete Message by ID")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple Message by one time")
	public Boolean multipleUpdate(@RequestBody List<Message> MessageList) {
		Boolean result = this.cudMultipleData(this.entityClass(), MessageList);
		
		if(result) {
			BeanUtil.get(MessageController.class).clearCache();
		}
		
		return result;
	}
	
	@PutMapping(value = "/clear_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean messageClearCache() {
		return BeanUtil.get(DomainController.class).requestClearCache("messages");
	}


	@CacheEvict(cacheNames = "Message", allEntries = true)
	public boolean clearCache() {
		Set<String> keys = redisTemplate.keys("Message*");
		if (ValueUtil.isNotEmpty(keys)) {
			redisTemplate.delete(keys);
		}

		return true;
	}
	
    @EventListener(
        classes = CacheClearEvent.class,
        condition = "#event.targetResource == 'messages'"
    )
    public void handleCacheClear(CacheClearEvent event) {
        // 도메인 캐시 초기화 로직
        this.clearCache();
    }
}