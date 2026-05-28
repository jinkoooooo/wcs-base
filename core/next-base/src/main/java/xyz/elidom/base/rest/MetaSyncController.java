package xyz.elidom.base.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.base.entity.Menu;
import xyz.elidom.base.entity.MenuButton;
import xyz.elidom.base.entity.MenuColumn;
import xyz.elidom.base.entity.MenuDetail;
import xyz.elidom.base.entity.MenuDetailButton;
import xyz.elidom.base.entity.MenuDetailColumn;
import xyz.elidom.base.entity.MenuParam;
import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.core.entity.Code;
import xyz.elidom.core.entity.CodeDetail;
import xyz.elidom.msg.entity.Message;
import xyz.elidom.msg.entity.Terminology;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.manager.DataSourceManager;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.Setting;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SysValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/meta_sync")
@ServiceDesc(description = "Synchronize Metadata Service API")
public class MetaSyncController extends AbstractRestService {

	@Autowired
	private DataSourceManager dsMgr;
	/**
	 * Nothing To Sync
	 */
	private static final String NOTHING_TO_SYNC = "Nothing To Sync";
	
	@Override
	protected Class<?> entityClass() {
		return Resource.class;
	}
	
	/**
	 * DataSourceManager로 부터 QueryManager를 찾아 리턴한다. 
	 * 
	 * @param ds
	 * @return
	 */
	private IQueryManager getQueryManagerFromDs(String ds) {
		if(SysValueUtil.isEmpty(ds)) {
			return this.queryManager;
		} else {
			IQueryManager sourceQueryManager = this.dsMgr.getQueryManager(ds);
			return sourceQueryManager;
		}
	}
	
	@PostMapping(value = "/pull/entity/{from_ds}/{from_domain_id}/{bundle}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Sync entity by pull mode")
	public Integer syncBundleEntitiesByPull(
			@PathVariable("from_ds") String fromDs, 
			@PathVariable("from_domain_id") String fromDomainIdStr, 
			@PathVariable("bundle") String bundle,
			@RequestParam(name = "sync_mode", required = true) String syncMode) {
		
		// 1. From DataSource로 부터 QueryManager 추출
		Long domainId = Domain.currentDomainId();
		Long fromDomainId = SysValueUtil.toLong(fromDomainIdStr);
		int count = 0;
		
		// 2. bundle 정보로 부터 엔티티 리스트 정보를 조회 ...
		IQueryManager sourceQueryManager = this.getQueryManagerFromDs(fromDs);
		Map<String, Object> condition = (SysValueUtil.isEmpty(bundle) || SysValueUtil.isEqual("all", bundle)) ? 
			SysValueUtil.newMap("domainId", fromDomainId) :
			SysValueUtil.newMap("domainId,bundle", fromDomainId, bundle);
		
		List<Resource> sourceEntities = sourceQueryManager.selectList(Resource.class, condition);
		
		if(SysValueUtil.isEmpty(sourceEntities)) {
			return count;
		} 
		
		// 3. syncMode - r : replace, u : update
		if(SysValueUtil.isEqual(syncMode, "r")) {
			Map<String, Object> deleteCond = SysValueUtil.newMap("domainId", domainId);
			this.queryManager.deleteList(ResourceColumn.class, deleteCond);
			this.queryManager.deleteList(Resource.class, deleteCond);
		}
		
		// 4. Entity를 traverse하면서 엔티티 데이터 동기화 
		for(Resource sourceEntity : sourceEntities) {
			List<ResourceColumn> sourceColumns = sourceQueryManager.selectList(ResourceColumn.class, SysValueUtil.newMap("resourceId", sourceEntity.getId()));
			
			if(SysValueUtil.isEmpty(sourceColumns)) {
				continue;
			}
			
			this.syncEntity(domainId, syncMode, sourceEntity, sourceColumns);
			count++;
		}
		
		return count;
	}
	
	@PostMapping(value = "/pull/entity/{from_ds}/{from_domain_id}/{entity_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Sync entity by pull mode")
	public String syncEntityByPull(
			@PathVariable("from_ds") String fromDs,
			@PathVariable("from_domain_id") String fromDomainIdStr, 
			@PathVariable("entity_name") String entityName,
			@RequestParam(name = "sync_mode", required = true) String syncMode) {
		
		// 1. From DataSource로 부터 QueryManager 추출
		Long domainId = Domain.currentDomainId();
		Long fromDomainId = SysValueUtil.toLong(fromDomainIdStr);
		
		// 2. 엔티티 명으로 엔티티 정보 조회 
		IQueryManager sourceQueryManager = this.getQueryManagerFromDs(fromDs);
		Map<String, Object> condition = SysValueUtil.newMap("domainId,name", fromDomainId, entityName);
		Resource sourceEntity = sourceQueryManager.selectByCondition(Resource.class, condition);
		
		if(sourceEntity == null) {
			return NOTHING_TO_SYNC;
		}
		
		// 3. 엔티티 컬럼 정보 조회 
		List<ResourceColumn> sourceColumns = sourceQueryManager.selectList(ResourceColumn.class, SysValueUtil.newMap("resourceId", sourceEntity.getId()));
		
		if(SysValueUtil.isEmpty(sourceColumns)) {
			return NOTHING_TO_SYNC;
		}
		
		// 3. syncMode - r : replace, u : update
		if(SysValueUtil.isEqual(syncMode, "r")) {
			Map<String, Object> deleteCond = SysValueUtil.newMap("domainId,name", domainId, entityName);
			this.queryManager.deleteList(ResourceColumn.class, deleteCond);
			this.queryManager.deleteList(Resource.class, deleteCond);
		}		
		
		// 4. 엔티티 데이터 동기화 
		return this.syncEntity(domainId, syncMode, sourceEntity, sourceColumns);
	}
	
	@PostMapping(value = "/pull/menu/{from_ds}/{from_domain_id}/{menu_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Sync menus by pull mode")
	public Integer syncMenusByPull(
			@PathVariable("from_ds") String fromDs, 
			@PathVariable("from_domain_id") String fromDomainIdStr, 
			@PathVariable("menu_name") String menuName,
			@RequestParam(name = "change_id", required = false) Boolean changeId) {
		
		// 1. From DataSource로 부터 QueryManager 추출
		boolean idChange = (changeId != null) && (changeId == true);
		Long domainId = Domain.currentDomainId();
		Long fromDomainId = SysValueUtil.toLong(fromDomainIdStr);
		int count = 0;
		
		// 2. 메뉴 명으로 메뉴 정보 조회 
		IQueryManager sourceQueryManager = this.getQueryManagerFromDs(fromDs);
		Map<String, Object> condition = null;
		
		// 3. all이면 모든 메뉴 조회 
		if(SysValueUtil.isEqual("all", menuName)) {
			condition = SysValueUtil.newMap("domainId", fromDomainId);
			List<Menu> sourceMenus = sourceQueryManager.selectList(Menu.class, condition);
			for(Menu sourceMenu : sourceMenus) {
				if(SysValueUtil.isEqual(SysConstants.OK_STRING, this.syncMenu(domainId, idChange, sourceMenu, sourceQueryManager))) {
					count++;
				}
			}
			
		} else {
			condition = SysValueUtil.newMap("domainId,name", fromDomainId, menuName);
			Menu sourceMenu = sourceQueryManager.selectByCondition(Menu.class, condition);
			String retStr = this.syncMenu(domainId, idChange, sourceMenu, sourceQueryManager);
			count = SysValueUtil.isEqual(SysConstants.OK_STRING, retStr) ? 0 : 1;
		}
		
		return count;
	}
	
	@PostMapping(value = "/pull/code/{from_ds}/{from_domain_id}/{code_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Sync codes by pull mode")
	public Integer syncCodesByPull(
			@PathVariable("from_ds") String fromDs, 
			@PathVariable("from_domain_id") String fromDomainIdStr, 
			@PathVariable("code_name") String codeName,
			@RequestParam(name = "change_id", required = false) Boolean changeId) {
		
		// 1. From DataSource로 부터 QueryManager 추출
		boolean idChange = (changeId != null) && (changeId == true);
		Long domainId = Domain.currentDomainId();
		Long fromDomainId = SysValueUtil.toLong(fromDomainIdStr);
		int count = 0;
		
		// 2. 메뉴 명으로 메뉴 정보 조회 
		IQueryManager sourceQueryManager = this.getQueryManagerFromDs(fromDs);
		
		// 3. all이면 모든 메뉴 조회 
		if(SysValueUtil.isEqual("all", codeName)) {
			List<Code> sourceCodes = sourceQueryManager.selectList(Code.class, SysValueUtil.newMap("domainId", fromDomainId));
			
			for(Code sourceCode : sourceCodes) {
				if(SysValueUtil.isEqual(SysConstants.OK_STRING, this.syncCode(domainId, idChange, sourceCode, sourceQueryManager))) {
					count++;
				}
			}
			
		} else {
			Code sourceCode = sourceQueryManager.selectByCondition(Code.class, SysValueUtil.newMap("domainId,name", fromDomainId, codeName));
			String retStr = this.syncCode(domainId, idChange, sourceCode, sourceQueryManager);
			count = SysValueUtil.isEqual(SysConstants.OK_STRING, retStr) ? 0 : 1;
		}
		
		return count;
	}
	
	@PostMapping(value = "/pull/setting/{from_ds}/{from_domain_id}/{setting_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Sync settings by pull mode")
	public Integer syncSettingsByPull(
			@PathVariable("from_ds") String fromDs, 
			@PathVariable("from_domain_id") String fromDomainIdStr, 
			@PathVariable("setting_name") String settingName) {
		
		// 1. From DataSource로 부터 QueryManager 추출
		Long domainId = Domain.currentDomainId();
		Long fromDomainId = SysValueUtil.toLong(fromDomainIdStr);
		int count = 0;
		
		// 2. 설정 명으로 메뉴 정보 조회 
		IQueryManager sourceQueryManager = this.getQueryManagerFromDs(fromDs);
		List<Setting> sourceSettings = null;
		
		// 3. all이면 모든 설정 조회 
		if(SysValueUtil.isEqual("all", settingName)) {
			sourceSettings = sourceQueryManager.selectList(Setting.class, SysValueUtil.newMap("domainId", fromDomainId));
			
		} else {
			sourceSettings = new ArrayList<Setting>();
			Setting sourceSetting = sourceQueryManager.selectByCondition(Setting.class, SysValueUtil.newMap("domainId,name", fromDomainId, settingName));
			if(sourceSetting != null) {
				sourceSettings.add(sourceSetting);
			}
		}
		
		for(Setting sourceSetting : sourceSettings) {
			sourceSetting.setDomainId(domainId);
			sourceSetting.setId(null);
			this.queryManager.deleteByCondition(Setting.class, new Setting(sourceSetting.getName()));
			this.queryManager.insert(sourceSetting);
		}		
		
		return count;
	}
	
	@PostMapping(value = "/pull/terms/{from_ds}/{from_domain_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Sync terminologies by pull mode")
	public Integer syncTermsByPull(
			@PathVariable("from_ds") String fromDs, 
			@PathVariable("from_domain_id") String fromDomainIdStr,
			@RequestParam(name = "replace_all", required = false) Boolean replaceAll) {
		
		// 1. From DataSource로 부터 QueryManager 추출
		boolean replace = (replaceAll != null && replaceAll == true);
		Long domainId = Domain.currentDomainId();
		Long fromDomainId = SysValueUtil.toLong(fromDomainIdStr);
		int count = 0;
		
		// 2. 기존 정보 모두 삭제 
		if(replace) {
			this.queryManager.deleteList(Terminology.class, SysValueUtil.newMap("domainId", domainId));
		}
		
		IQueryManager sourceQueryManager = this.getQueryManagerFromDs(fromDs);
		List<Terminology> sourceTerms = sourceQueryManager.selectList(Terminology.class, SysValueUtil.newMap("domainId", fromDomainId));
		
		// 3. From DataSource의 데이터 복사 
		for(Terminology sourceTerm : sourceTerms) {
			sourceTerm.setDomainId(domainId);
			sourceTerm.setId(null);
			
			if(!replace) {
				Map<String, Object> params = SysValueUtil.newMap("domainId,name,locale,category", domainId, sourceTerm.getName(), sourceTerm.getLocale(), sourceTerm.getCategory());
				this.queryManager.deleteList(Terminology.class, params);
			}
			
			this.queryManager.insert(sourceTerm);
			count++;
		}		
		
		return count;
	}
	
	@PostMapping(value = "/pull/message/{from_ds}/{from_domain_id}/{msg_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Sync messages by pull mode")
	public Integer syncMessagesByPull(
			@PathVariable("from_ds") String fromDs, 
			@PathVariable("from_domain_id") String fromDomainIdStr,
			@RequestParam(name = "replace_all", required = false) Boolean replaceAll) {
		
		// 1. From DataSource로 부터 QueryManager 추출
		boolean replace = (replaceAll != null && replaceAll == true);
		Long domainId = Domain.currentDomainId();
		Long fromDomainId = SysValueUtil.toLong(fromDomainIdStr);
		int count = 0;
		
		// 2. 기존 정보 모두 삭제 
		if(replace) {
			this.queryManager.deleteList(Message.class, SysValueUtil.newMap("domainId", domainId));
		}
		
		IQueryManager sourceQueryManager = this.getQueryManagerFromDs(fromDs);
		List<Message> sourceMsgs = sourceQueryManager.selectList(Message.class, SysValueUtil.newMap("domainId", fromDomainId));
		
		// 3. From DataSource의 데이터 복사 
		for(Message sourceMsg : sourceMsgs) {
			sourceMsg.setDomainId(domainId);
			sourceMsg.setId(null);
			
			if(!replace) {
				Map<String, Object> params = SysValueUtil.newMap("domainId,name,locale", domainId, sourceMsg.getName(), sourceMsg.getLocale());
				this.queryManager.deleteList(Message.class, params);
			}
			
			this.queryManager.insert(sourceMsg);
			count++;
		}		
		
		return count;
	}
	
	/**
	 * 공통 코드 동기화 
	 * 
	 * @param domainId
	 * @param idChange
	 * @param sourceCode
	 * @param sourceQueryManager
	 * @return
	 */
	private String syncCode(Long domainId, boolean idChange, Code sourceCode, IQueryManager sourceQueryManager) {
		if(sourceCode == null) {
			return NOTHING_TO_SYNC;
		}
		
		Map<String, Object> condition = SysValueUtil.newMap("parentId", sourceCode.getId());
		List<CodeDetail> sourceCodeDetails = sourceQueryManager.selectList(CodeDetail.class, condition);
		if(SysValueUtil.isEmpty(sourceCodeDetails)) {
			return NOTHING_TO_SYNC;
		}
		
		// 기본 DS의 코드가 있다면 코드 디테일 삭제 
		Code targetCode = new Code(domainId, sourceCode.getName());
		targetCode = this.queryManager.selectByCondition(Code.class, targetCode);
		
		if(targetCode != null) {
			Map<String, Object> targetCond = SysValueUtil.newMap("domainId,parentId", domainId, targetCode.getId());
			List<CodeDetail> targetDetails = this.queryManager.selectList(CodeDetail.class, targetCond);
			this.queryManager.deleteBatch(targetDetails);
			
		} else {
			sourceCode.setDomainId(domainId);
			sourceCode.setId(null);
			this.queryManager.insert(sourceCode);
		}
		
		// 새로운 코드 디테일 삽입 
		for(CodeDetail sourceCodeDetail : sourceCodeDetails) {
			sourceCodeDetail.setParentId(sourceCode.getId());
			sourceCodeDetail.setDomainId(domainId);
			sourceCodeDetail.setId(null);
			this.queryManager.insert(sourceCodeDetail);
		}
		
		return SysConstants.OK_STRING;
	}
	
	/**
	 * 엔티티 sourceEntity와 sourceColumns 정보를 현재 데이터베이스에 업데이트 한다. 
	 *  
	 * @param domainId
	 * @param syncMode
	 * @param sourceEntity
	 * @param sourceColumns
	 * @return
	 */
	private String syncEntity(Long domainId, String syncMode, Resource sourceEntity, List<ResourceColumn> sourceColumns) {
		// 1. syncMode가 업데이트 (u)인 경우 - 기존 엔티티 삭제 후 기존 엔티티 아이디로 소스 엔티티 정보 설정  
		if(SysValueUtil.isEqual("u", syncMode)) {
			Resource targetEntity = new Resource(domainId, sourceEntity.getName());
			targetEntity = this.queryManager.selectByCondition(Resource.class, targetEntity);
			
			if(targetEntity != null) {
				Map<String, Object> rcCond = SysValueUtil.newMap("domainId,entityId", domainId, targetEntity.getId());
				List<ResourceColumn> targetColumns = this.queryManager.selectList(ResourceColumn.class, rcCond);
				this.queryManager.deleteBatch(targetColumns);
				this.queryManager.delete(targetEntity);				
			}
			
			sourceEntity.setId((targetEntity == null) ? null : targetEntity.getId());			
		}

		// 2. 엔티티 및 엔티티 컬럼 추가 
		sourceEntity.setDomainId(domainId);
		this.queryManager.insert(sourceEntity);
		
		for(ResourceColumn sourceColumn : sourceColumns) {
			sourceColumn.setDomainId(domainId);
			sourceColumn.setEntityId(sourceEntity.getId());
			sourceColumn.setId(null);
			this.queryManager.insert(sourceColumn);
		}
		
		return SysConstants.OK_STRING;
	}
	
	/**
	 * 메뉴 동기화 
	 * 
	 * @param domainId
	 * @param idChange
	 * @param sourceMenu
	 * @param sourceQueryManager
	 * @return
	 */
	private String syncMenu(Long domainId, boolean idChange, Menu sourceMenu, IQueryManager sourceQueryManager) {
		Menu targetMenu = new Menu(domainId, sourceMenu.getName());
		targetMenu = this.queryManager.selectByCondition(Menu.class, targetMenu);
		Map<String, Object> sourceCond = SysValueUtil.newMap("menuId", sourceMenu.getId());
		List<MenuColumn> sourceColumns = sourceQueryManager.selectList(MenuColumn.class, sourceCond);
		
		if(SysValueUtil.isEmpty(sourceColumns)) {
			return NOTHING_TO_SYNC;
		}
		
		if(targetMenu != null) {
			Map<String, Object> targetCond = SysValueUtil.newMap("menuId", targetMenu.getId());
						
			// Target의 기존 메뉴 관련 데이터 모두 삭제 
			List<MenuParam> targetParams = this.queryManager.selectList(MenuParam.class, targetCond);
			List<MenuButton> targetButtons = this.queryManager.selectList(MenuButton.class, targetCond);
			List<MenuColumn> targetColumns = this.queryManager.selectList(MenuColumn.class, targetCond);
			
			this.queryManager.deleteBatch(targetParams);
			this.queryManager.deleteBatch(targetButtons);
			this.queryManager.deleteBatch(targetColumns);
			this.queryManager.delete(targetMenu);				
		}
		
		// 메뉴 복사 
		sourceMenu.setId((targetMenu == null || idChange) ? null : targetMenu.getId());
		sourceMenu.setDomainId(domainId);
		this.queryManager.insert(sourceMenu);
		
		// 메뉴 컬럼 복사 
		List<MenuColumn> menuColumns = sourceQueryManager.selectList(MenuColumn.class, sourceCond);
		if(SysValueUtil.isNotEmpty(menuColumns)) {
			for(MenuColumn menuColumn : menuColumns) {
				menuColumn.setDomainId(domainId);
				menuColumn.setMenuId(sourceMenu.getId());
				menuColumn.setId(null);
				this.queryManager.insert(menuColumn);
			}			
		}
		
		// 메뉴 버튼 복사 
		List<MenuButton> menuButtons = sourceQueryManager.selectList(MenuButton.class, sourceCond);
		if(SysValueUtil.isNotEmpty(menuButtons)) {
			for(MenuButton menuButton : menuButtons) {
				menuButton.setDomainId(domainId);
				menuButton.setMenuId(sourceMenu.getId());
				menuButton.setId(null);
				this.queryManager.insert(menuButton);
			}			
		}
		
		// 메뉴 파라미터 복사 
		List<MenuParam> menuParams = sourceQueryManager.selectList(MenuParam.class, sourceCond);
		if(SysValueUtil.isNotEmpty(menuParams)) {
			for(MenuParam menuParam : menuParams) {
				menuParam.setDomainId(domainId);
				menuParam.setMenuId(sourceMenu.getId());
				menuParam.setId(null);
				this.queryManager.insert(menuParam);
			}
		}
		
		if(targetMenu != null) {
			Map<String, Object> targetCond = SysValueUtil.newMap("menuId", targetMenu.getId());
			MenuDetail targetMenuDetail = this.queryManager.selectByCondition(MenuDetail.class, targetCond);
			this.syncMenuDetail(domainId, sourceMenu, targetMenuDetail, sourceQueryManager);
		}
		
		return SysConstants.OK_STRING;
	}
	
	/**
	 * 메뉴 상세 동기화 
	 * 
	 * @param domainId
	 * @param sourceMenu
	 * @param targetMenuDetail
	 * @param sourceQueryManager
	 * @return
	 */
	private String syncMenuDetail(Long domainId, Menu sourceMenu, MenuDetail targetMenuDetail, IQueryManager sourceQueryManager) {
		// Target MenuDetail 관련 정보 모두 삭제 
		if(targetMenuDetail != null) {
			Map<String, Object> targetCond = SysValueUtil.newMap("menuDetailId", targetMenuDetail.getId());
			List<MenuDetailColumn> targetColumns = this.queryManager.selectList(MenuDetailColumn.class, targetCond);
			List<MenuDetailButton> targetButtons = this.queryManager.selectList(MenuDetailButton.class, targetCond);
			
			this.queryManager.deleteBatch(targetButtons);
			this.queryManager.deleteBatch(targetColumns);
			this.queryManager.delete(targetMenuDetail);
		}
		
		Map<String, Object> sourceCond = SysValueUtil.newMap("menuId", sourceMenu.getId());
		MenuDetail sourceMenuDetail = sourceQueryManager.selectByCondition(MenuDetail.class, sourceCond);
		
		if(sourceMenuDetail != null) {
			// 메뉴 상세 복사 
			sourceMenuDetail.setId(null);
			sourceMenuDetail.setMenuId(sourceMenu.getId());
			sourceMenuDetail.setDomainId(domainId);
			this.queryManager.insert(sourceMenu);
			
			sourceCond = SysValueUtil.newMap("menuDetailId", sourceMenuDetail.getId());
			List<MenuDetailColumn> detailColumns = sourceQueryManager.selectList(MenuDetailColumn.class, sourceCond);
			if(SysValueUtil.isNotEmpty(detailColumns)) {
				for(MenuDetailColumn detilColumn : detailColumns) {
					detilColumn.setDomainId(domainId);
					detilColumn.setMenuDetailId(sourceMenuDetail.getId());
					detilColumn.setId(null);
					this.queryManager.insert(detilColumn);
				}			
			}
			
			List<MenuDetailButton> detailButtons = sourceQueryManager.selectList(MenuDetailButton.class, sourceCond);
			if(SysValueUtil.isNotEmpty(detailButtons)) {
				for(MenuDetailButton detilButton : detailButtons) {
					detilButton.setDomainId(domainId);
					detilButton.setMenuDetailId(sourceMenuDetail.getId());
					detilButton.setId(null);
					this.queryManager.insert(detilButton);
				}			
			}
		}
		
		return SysConstants.OK_STRING;
	}

}
