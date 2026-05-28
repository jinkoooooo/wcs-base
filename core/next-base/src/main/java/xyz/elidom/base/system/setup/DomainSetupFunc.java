package xyz.elidom.base.system.setup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import xyz.elidom.base.entity.Menu;
import xyz.elidom.base.entity.MenuButton;
import xyz.elidom.base.entity.MenuColumn;
import xyz.elidom.base.entity.MenuDetail;
import xyz.elidom.base.entity.MenuDetailButton;
import xyz.elidom.base.entity.MenuDetailColumn;
import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.base.entity.relation.MenuRef;
import xyz.elidom.base.entity.relation.ResourceRef;
import xyz.elidom.base.model.SeedEntity;
import xyz.elidom.core.CoreConfigConstants;
import xyz.elidom.core.entity.Code;
import xyz.elidom.core.entity.CodeDetail;
import xyz.elidom.dbist.annotation.ChildEntity;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.MasterDetailType;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.AbstractStamp;
import xyz.elidom.sec.entity.Permission;
import xyz.elidom.sec.entity.Role;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.rest.DomainController;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.FormatUtil;

/**
 * DomainSetup을 위한 유틸리티 클래스
 * 
 * @author shortstop
 */
public class DomainSetupFunc {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(DomainSetupFunc.class);

	/**
	 * Time Stamp 테이블 필드 리스트
	 */
	private static List<String> ENTITY_NULLIFY_FIELDS = OrmConstants.ENTITY_FIELD_DEFAULT_IGNORED_LIST;

	/**
	 * query manager
	 */
	private IQueryManager queryManager;

	/**
	 * 초기 데이터 파라미터
	 */
	private Map<String, Object> setupParams;
	
	/**
	 * 복사할 소스 도메인
	 */
	private Domain sourceDomain;
	
	/**
	 * 새로 생성한 도메인
	 */
	private Domain setupDomain;

	/**
	 * Menu 원래 아이디와 새 아이디간 매핑
	 */
	private Map<String, String> menuIdMapping = new HashMap<String, String>();

	/**
	 * Menu Detail 원래 아이디와 새 아이디간 매핑
	 */
	private Map<String, String> menuDetailIdMapping = new HashMap<String, String>();

	/**
	 * Role 원래 아이디와 새 아이디간 매핑
	 */
	private Map<String, String> roleIdMapping = new HashMap<String, String>();

	/**
	 * Entity 원래 아이디와 새 아이디간 매핑
	 */
	private Map<String, String> entityIdMapping = new HashMap<String, String>();

	/**
	 * Code 원래 아이디와 새 아이디간 매핑
	 */
	private Map<String, String> codeIdMapping = new HashMap<String, String>();

	/**
	 * 생성자
	 * 
	 * @param queryManager
	 * @param setupParams
	 */
	public DomainSetupFunc(IQueryManager queryManager, Map<String, Object> setupParams) {
		this.queryManager = queryManager;
		this.setupParams = setupParams;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// Validation
	/////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 도메인 관련 정보 유효성 체크
	 * 
	 * @param domainId
	 * @param domainName
	 * @param domainUrl
	 * @return
	 */
	public Boolean validateDomain(Long domainId, String domainName, String domainUrl) {
		// 1. domainId로 도메인이 이미 존재하는지 체크
		Domain domain = this.queryManager.select(Domain.class, domainId);
		if (domain != null) {
			throw ThrowUtil.newDataDuplicated("terms.label.domain", String.valueOf(domainId));
		}

		// 2. domainName으로 도메인이 이미 존재하는지 체크
		Domain condition = new Domain();
		condition.setName(domainName);
		domain = this.queryManager.selectByCondition(Domain.class, condition);
		if (domain != null) {
			throw ThrowUtil.newDataDuplicated("terms.label.domain", domainName);
		}

		// 3. domainUrl로 도메인이 이미 존재하는지 체크
		condition.setName(null);
		condition.setSubdomain(domainUrl);
		domain = this.queryManager.selectByCondition(Domain.class, condition);
		if (domain != null) {
			throw ThrowUtil.newDataDuplicated("terms.label.domain_url", domainUrl);
		}

		return true;
	}

	/**
	 * 도메인 사용자 정보 유효성 체크
	 * 
	 * @param userId
	 * @param email
	 * @param name
	 * @param password
	 * @return
	 */
	private Boolean validateUser(String userId, String email, String name, String password) {
		// 1. ID 중복 체크
		if (null != this.queryManager.select(User.class, userId)) {
			throw ThrowUtil.newDataDuplicated("terms.label.user_id", userId);
		}

		return true;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 기 타
	/////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * setupParams에 포함된 key 데이터를 찾아 리턴
	 * 
	 * @param key
	 * @return
	 */
	private String getInitialValue(String key) {
		return this.setupParams.containsKey(key) ? (String) this.setupParams.get(key) : null;
	}

	/**
	 * Storage Root 설정을 파라미터로 받은 값으로 업데이트
	 * 
	 * @param queryManager
	 */
	private void updateStorageRootSetting() {
		/*String storageRoot = SettingUtil.getValue("subterm.setup.file.root.path", "c:/uploads");

		if (ValueUtil.isNotEmpty(storageRoot)) {
			Setting condition = new Setting("file.root.path");
			Setting storageRootSetting = this.queryManager.selectByCondition(Setting.class, condition);

			if (storageRootSetting == null) {
				condition.setValue(storageRoot);
				this.queryManager.insert(condition);
			} else {
				storageRootSetting.setValue(storageRoot);
				this.queryManager.update(storageRootSetting);
			}
		}*/
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// Export Domain
	/////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 데이터 초기화된 상태에서 새로운 도메인 셋업
	 * 
	 * @return
	 */
	public Boolean setupDomain() {
		Long sysDomainId = Domain.systemDomain().getId();
		String sourceDomainName = SettingUtil.getValue(sysDomainId, "seed.source.domain.name");
		
		if(SysValueUtil.isNotEmpty(sourceDomainName)) {
			Long sourceDomainId = null;
			try {
				sourceDomainId = Long.parseLong(sourceDomainName);
				this.sourceDomain = Domain.find(sourceDomainId);
			} catch (Exception e) {
				this.sourceDomain = Domain.findByName(sourceDomainName);
			}
		}
		
		if(this.sourceDomain == null) {
			this.sourceDomain = Domain.systemDomain();
		}
		
		String seedSource = SettingUtil.getValue(sysDomainId, "seed.source", "seed-procedure");
		if (!this.setupParams.containsKey("locale")) {
			this.setupParams.put("locale", SettingUtil.getValue(sysDomainId, CoreConfigConstants.DEFAULT_LOCALE, "ko-KR"));
		}
		
		try {
			// 1. 도메인 셋업 전 Validation 체크
			this.validateDomainSetup();

			// 2. Domain, User 정보 생성
			this.createDomainUserData();

			// 3. Seed 정보에 의한 데이터 생성
			if(SysValueUtil.isEqualIgnoreCase(seedSource, "seed-procedure")) {
				this.createDomainSeedData(this.sourceDomain.getId());
				
			// 기본으로 Seed 파일에 의한 데이터 생성
			} else {
				String module = this.setupParams.containsKey("module") ? (String) this.setupParams.get("module") : null;
				this.createDomainSeedData(module);
			}
			
			// 4. Setting 정보 Root Path를 Storage Root 값으로 업데이트
			this.updateStorageRootSetting();

			// 5. rabbitmq 사이트 관리 - 사이트 생성 호출
			// TODO : VirtualHostController.multipleUpdate(@RequestBody List<Site> list)
		} catch (RuntimeException e) {
			throw e;

		} finally {
			// 6 . 도메인 캐쉬 클리어
			BeanUtil.get(DomainController.class).clearCache();
		}

		return true;
	}

	/**
	 * 도메인 셋업 전 Validation 체크
	 */
	public void validateDomainSetup() {
		// 1. Domain Setup 파라미터 존재 체크
		AssertUtil.assertNotEmpty("terms.label.domain_name", this.getInitialValue("domain_name"));
		AssertUtil.assertNotEmpty("terms.label.url", this.getInitialValue("domain_url"));

		Long domainId = 0L;

		// 2. Check Validation Domain
		if (this.setupParams.containsKey("domain_id")) {
			domainId = SysValueUtil.toLong(this.setupParams.get("domain_id"));
		} else {
			Integer maxDomainId = this.queryManager.selectBySql("select max(id) from domains", null, Integer.class);
			domainId = SysValueUtil.toLong(maxDomainId + 1);
		}

		this.setupParams.put("domain_id", domainId);
		String domainName = this.getInitialValue("domain_name");
		String domainUrl = this.getInitialValue("domain_url");
		this.validateDomain(domainId, domainName, domainUrl);

		// 3. Check Validation User
		String userId = this.getInitialValue("admin_id");
		if(SysValueUtil.isNotEmpty(userId)) {
			AssertUtil.assertNotEmpty("terms.label.admin_name", this.getInitialValue("admin_name"));
			AssertUtil.assertNotEmpty("terms.label.password", this.getInitialValue("password"));
			
			String userEmail = this.getInitialValue("admin_email");
			String userName = this.getInitialValue("admin_name");
			String password = this.setupParams.containsKey("password") ? this.getInitialValue("password") : this.getInitialValue("admin_name");
			this.validateUser(userId, userEmail, userName, password);
		}
	}

	/**
	 * 도메인 & 사용자 데이터 생성
	 */
	private void createDomainUserData() {
		// 1. 도메인 생성
		Long setupDomainId = (Long) this.setupParams.get("domain_id");
		Domain newDomain = new Domain(setupDomainId);
		newDomain.setName(this.getInitialValue("domain_name"));
		newDomain.setSystemFlag(false);
		newDomain.setSubdomain(this.getInitialValue("domain_url"));
		newDomain.setBrandName(this.getInitialValue("domain_name"));
		newDomain.setMwSiteCd(newDomain.getName().toLowerCase());
		newDomain.setOwner(this.getInitialValue("admin_id"));
		newDomain.setDescription(this.getInitialValue("domain_name"));
		this.queryManager.insert(newDomain);
		this.setupDomain = this.queryManager.select(Domain.class, setupDomainId);

		// 2. 사용자 생성
		String loginId = this.getInitialValue("admin_id");
		if(SysValueUtil.isNotEmpty(loginId)) {
			String password = this.setupParams.containsKey("password") ? this.getInitialValue("password") : this.getInitialValue("admin_name");
			PasswordEncoder passwordEncoder = BeanUtil.get(PasswordEncoder.class);
			String encrypted = passwordEncoder.encode(password);
			User user = new User();
			user.setDomainId(setupDomainId);
			user.setLogin(this.getInitialValue("admin_id"));
			user.setName(this.getInitialValue("admin_name"));
			user.setEmail(this.getInitialValue("admin_email"));
			user.setAdminFlag(true);
			user.setActiveFlag(true);
			user.setEncryptedPassword(encrypted);
			user.setLocale(this.getInitialValue("locale"));
			this.queryManager.insert(user);
		}
	}

	/**
	 * Entity Data 셋업
	 * 
	 * @param entityClass
	 * @param table
	 * @param dataList
	 */
	private void setupDomainEntityData(Class<?> entityClass, Table table, List<?> dataList) {
		if (dataList != null && !dataList.isEmpty()) {
			List<Field> fieldList = ClassUtil.getAllFields(new ArrayList<Field>(), entityClass);

			if (dataList != null && !dataList.isEmpty()) {
				int successCount = this.restoreData(entityClass, table, fieldList, dataList);
				logger.info("Setup [" + entityClass.getName() + "] Data [" + successCount + "] Count!");
			} else {
				logger.info("Setup [" + entityClass.getName() + "] Data [0] Count!");
			}
		} else {
			logger.info("Setup [" + entityClass.getName() + "] Data [0] Count!");
		}
	}

	/**
	 * 애플리케이션이 관리하는 Seed 데이터 템플릿 리소스 파일로 부터 도메인 데이터 생성
	 * 
	 * @param moduleName
	 * @return
	 */
	private boolean createDomainSeedData(String moduleName) {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		List<SeedEntity> seedEntities = this.parseSeedMetaFile();

		for (SeedEntity seedEntity : seedEntities) {
			String bundle = seedEntity.getBundle();
			IModuleProperties module = configSet.getConfig(bundle);
			if (module != null) {
				this.setupDomainBundleData(seedEntity, module);
			}
		}

		return true;
	}
	
	/**
	 * 프로시져에 의한 도메인 초기 정보 생성
	 * 
	 * @param baseDomainId
	 */
	private void createDomainSeedData(long baseDomainId) {
		// 1. 조건 생성
		Map<String, Object> params = SysValueUtil.newMap("P_BASE_DOMAIN_ID,P_COPY_DOMAIN_ID", baseDomainId, (Long) this.setupParams.get("domain_id"));
		// 2. 프로시져 콜 
		Map<?, ?> result = this.queryManager.callReturnProcedure("SP_SYS_CREATE_DOMAIN_DATA", params, Map.class);
		String resMessage = result.get("sp_sys_create_domain_data").toString();
		// 3. 결과 파싱 
//		int resCode = ((java.math.BigDecimal)result.get("P_OUT_RESULT_CODE")).toBigInteger().intValueExact();
//		String resMessage = ValueUtil.isNotEmpty(result.get("P_OUT_MESSAGE")) ? result.get("P_OUT_MESSAGE").toString() : SysConstants.EMPTY_STRING;
//
//		if(resCode == 0) {
//			if(ValueUtil.isEqualIgnoreCase(resMessage, "OK") == false) {
//				throw new ElidomRuntimeException(resMessage);
//			}
//		} else {
//			throw new ElidomRuntimeException(resMessage);
//		}
//
		if(SysValueUtil.isEqualIgnoreCase(resMessage, "OK") == false) {
			throw new ElidomRuntimeException(resMessage);
		}
	}
	
	/**
	 * Seed Meta Data 파일을 읽어서 SeedMeta로 파싱 
	 * 
	 * @return
	 */
	private List<SeedEntity> parseSeedMetaFile() {
		String metaContent = FileUtil.readClasspathFile("seeds", "domain-seed-meta.json");
		JSONArray metaArray = FormatUtil.parseJsonArray(metaContent);
		java.util.Iterator<?> iter = metaArray.iterator();
		List<SeedEntity> results = new ArrayList<SeedEntity>();
		
		while(iter.hasNext()) {
			String objStr = iter.next().toString();
			results.add(FormatUtil.jsonToObject(objStr, SeedEntity.class));
		}
		
		return results;
	}

	/**
	 * 로컬 파일로 관리하는 Seed 데이터로 부터 도메인 초기 데이터를 셋업
	 * 
	 * @param seedEntity
	 * @param module
	 */
	private void setupDomainBundleData(SeedEntity seedEntity, IModuleProperties module) {
		// Seed Data를 조회하여 데이터를 생성 후 데이터베이스에 저장
		String entityClassName = seedEntity.getEntity();
		Class<?> entityClass = ClassUtil.forName(entityClassName);
		Table table = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);

		if (table != null) {
			// 엔티티 별 데이터 조회 
			List<?> dataList = this.queryManager.selectList(entityClass, SysValueUtil.newMap("domainId", this.sourceDomain.getId()));
			// 엔티티 별 데이터 셋업
			this.setupDomainEntityData(entityClass, table, dataList);
		}
	}

	/**
	 * map 데이터 정보를 entityClass 오브젝트로 변환
	 * 
	 * @param entityClass
	 * @param table
	 * @param fieldList
	 * @param data
	 * @return
	 */
	private Object convertObjectBeforeRestore(Class<?> entityClass, Table table, List<Field> fieldList, Map<String, Object> data) {
		if (SysValueUtil.isEqual(table.idStrategy(), GenerationRule.AUTO_INCREMENT)) {
			data.put(OrmConstants.ENTITY_FIELD_ID, null);
		}

		for (String nullifyField : ENTITY_NULLIFY_FIELDS) {
			data.remove(nullifyField);
		}

		String objJsonStr = FormatUtil.toJsonString(data);
		return FormatUtil.jsonToObject(objJsonStr, entityClass);
	}

	/**
	 * 마스터 - 디테일로 구성된 데이터 파싱
	 * 
	 * @param dataArray
	 * @param entityClass
	 * @param masterFieldList
	 * @param table
	 * @return
	 */
	@SuppressWarnings("all")
	private List<?> parseMasterDetails(JSONArray dataArray, Class<?> entityClass, List<Field> masterFieldList, Table table) {
		// 1. Detail Class별 Field List 및 Annotation Table을 관리
		Map<String, List<Field>> detailClassFieldsMap = new HashMap<String, List<Field>>();
		Map<String, Table> detailClassTablesMap = new HashMap<String, Table>();
		ChildEntity[] childEntities = table.childEntities();

		// 2. 서브 엔티티 처리를 위해 준비
		this.readySubEntities(childEntities, detailClassFieldsMap, detailClassTablesMap);

		// 3. 건별로 순회하면서 master - detail 정보를 추출하여 entity instance를 만들고 리턴
		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < dataArray.size(); i++) {
			Map<String, Object> source = FormatUtil.jsonToObject(dataArray.get(i).toString(), HashMap.class);
			Map<String, Object> target = new HashMap<String, Object>();

			// 3.1. master 처리
			Map<String, Object> master = (Map<String, Object>) source.get("master");
			Object masterInstance = convertObjectBeforeRestore(entityClass, table, masterFieldList, master);
			target.put("master", masterInstance);

			// 3.2. details 처리
			this.parseDetailWithSubEntity(childEntities, detailClassFieldsMap, detailClassTablesMap, source, target);
			dataList.add(target);
		}

		return dataList;
	}

	/**
	 * 서브 엔티티 처리를 위해 준비
	 * 
	 * @param childEntities
	 * @param detailClassFieldsMap
	 * @param detailClassTablesMap
	 */
	private void readySubEntities(ChildEntity[] childEntities, Map<String, List<Field>> detailClassFieldsMap, Map<String, Table> detailClassTablesMap) {
		for (ChildEntity childEntity : childEntities) {
			String dataProp = childEntity.dataProperty();
			Class<?> detailClazz = childEntity.entityClass();
			List<Field> detailFieldList = ClassUtil.getAllFields(new ArrayList<Field>(), detailClazz);
			Table detailTable = detailClazz.getAnnotation(Table.class);
			detailClassFieldsMap.put(dataProp, detailFieldList);
			detailClassTablesMap.put(dataProp, detailTable);
		}
	}

	/**
	 * 마스터 - 디테일 데이터 중 디테일 데이터 처리
	 * 
	 * @param childEntities
	 * @param detailClassFieldsMap
	 * @param detailClassTablesMap
	 * @param source
	 * @param target
	 */
	@SuppressWarnings("unchecked")
	private void parseDetailWithSubEntity(ChildEntity[] childEntities, Map<String, List<Field>> detailClassFieldsMap, Map<String, Table> detailClassTablesMap,
			Map<String, Object> source, Map<String, Object> target) {

		// 1. details 처리
		for (ChildEntity childEntity : childEntities) {
			String dataProp = childEntity.dataProperty();

			if (source.containsKey(dataProp)) {
				Class<?> detailClass = childEntity.entityClass();
				List<Field> detailFieldList = detailClassFieldsMap.get(dataProp);
				Table detailTable = detailClassTablesMap.get(dataProp);

				// Case 1 - Master - Detail 1 : N 관계
				if (SysValueUtil.isEqual(childEntity.type(), MasterDetailType.ONE_TO_MANY)) {
					List<Map<String, Object>> subDataList = (List<Map<String, Object>>) source.get(dataProp);
					List<Object> detailList = new ArrayList<Object>();

					for (Map<String, Object> subData : subDataList) {
						detailList.add(convertObjectBeforeRestore(detailClass, detailTable, detailFieldList, subData));
					}

					target.put(dataProp, detailList);

					// Case 2 - Master - Detail 1 : 1 관계
				} else {
					Map<String, Object> subData = (Map<String, Object>) source.get(dataProp);
					Object detailInstance = convertObjectBeforeRestore(detailClass, detailTable, detailFieldList, subData);
					target.put(dataProp, detailInstance);
				}
			}
		}
	}

	/**
	 * dataList를 데이터베이스에 저장
	 * 
	 * @param entityClass
	 * @param table
	 * @param fieldList
	 * @param dataList
	 * @return
	 */
	private <T> int restoreData(Class<?> entityClass, Table table, List<Field> fieldList, List<T> dataList) {
		ChildEntity[] childEntities = table.childEntities();
		boolean isMasterDetail = (childEntities != null && childEntities.length > 0);
		
		for (T data : dataList) {
			if (isMasterDetail) {
				this.restoreMasterDetails(entityClass, table, fieldList, data);
			} else {
				this.restoreInstance(data);
			}
		}

		return dataList.size();
	}

	/**
	 * master - detail 구조의 데이터 저장
	 * 
	 * @param entityClass
	 * @param table
	 * @param fieldList
	 * @param masterInstance
	 * @return
	 */
	private <T> Object restoreMasterDetails(Class<?> entityClass, Table table, List<Field> fieldList, T masterInstance) {
		// 1. Master 처리
		Object masterId = this.restoreInstance(masterInstance);
		
		// 2. Detail 처리
		ChildEntity[] detailEntities = table.childEntities();

		// 3. Detail 클래스를 돌면서 ...
		for (int i = 0; i < detailEntities.length; i++) {
			ChildEntity detailEntity = detailEntities[i];
			Class<?> subClass = detailEntity.entityClass();
			
			if(SysValueUtil.isEqual(entityClass.getName(), subClass.getName())) {
				continue;
			}
			
			// Detail 데이터 조회를 위한 파라미터 설정
			String[] refFields = detailEntity.refFields().split(OrmConstants.COMMA);
			Map<String, Object> params = SysValueUtil.newMap("domainId", this.sourceDomain.getId());
			
			for (int x = 0; x < refFields.length; x++) {
				String refFieldName = refFields[x];

				// 필드명이 onType, resourceType인 경우 검색 조건에 마스터 Entity의 이름을 설정
				if (SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_TYPE) || SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_TYPE)) {
					continue;

				// 필드명이 onId, resourceId인 마스터 Entity의 id를 무조건 String으로 변환하여 설정
				} else if (SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_ID) || SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_ID)) {
					params.put(refFieldName, masterId);

				// 그 외의 경우 마스터 Entity의 id 정보를 설정
				} else {
					params.put(refFieldName, masterId);
				}
			}			
			
			if(refFields == null || refFields.length == 0) {
				continue;
			}
			
			// 데이터 리스트 조회
			List<?> subDataList = this.queryManager.selectList(subClass, params);
			if(SysValueUtil.isNotEmpty(subDataList)) {
				// 1) master - detail 1:N 관계 처리
				if (SysValueUtil.isEqual(detailEntity.type(), MasterDetailType.ONE_TO_MANY)) {
					this.restoreDetailList(refFields, masterId, subDataList);
	
				// 2) master - detail 1:1 관계 처리
				} else {
					Object detail = (Object) subDataList.get(0);
					this.restoreDetail(refFields, masterId, detail);
				}
			}
		}

		return masterId;
	}

	/**
	 * restore detail object list
	 * 
	 * @param refFieldNames
	 * @param masterId
	 * @param detailList
	 */
	private <T> void restoreDetailList(String[] refFieldNames, Object masterId, List<T> detailList) {
		for (T detail : detailList) {
			restoreDetail(refFieldNames, masterId, detail);
		}
	}

	/**
	 * restore detail object
	 * 
	 * @param refFieldNames
	 * @param masterId
	 * @param detail
	 */
	private <T> void restoreDetail(String[] refFieldNames, Object masterId, T detail) {
		for (int x = 0; x < refFieldNames.length; x++) {
			String refFieldName = refFieldNames[x];

			// 필드명이 onType, resourceType인 경우 검색 조건에 마스터 Entity의 이름을 설정
			if (SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_TYPE) || SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_TYPE)) {
				continue;

				// 필드명이 onId, resourceId인 마스터 Entity의 id를 무조건 String으로 변환하여 설정
			} else if (SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_ID) || SysValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_ID)) {
				ClassUtil.setFieldValue(detail, refFieldName, masterId);

				// 그 외의 경우 마스터 Entity의 id 정보를 설정
			} else {
				ClassUtil.setFieldValue(detail, refFieldName, masterId);
			}
		}

		this.restoreInstance(detail);
	}

	/**
	 * DomainId, ID를 새롭게 설정하고 데이터베이스에 저장
	 * 
	 * @param data
	 * @return object의 id를 리턴
	 */
	private <T> Object restoreInstance(T data) {
		Object oldId = ClassUtil.getFieldValue(data, OrmConstants.ENTITY_FIELD_ID);
		ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_DOMAIN_ID, this.setupDomain.getId());
		ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_ID, null);

		this.onBeforeRestoreInstance(data);
		this.queryManager.insert(data);
		Object newId = this.getObjectId(data);
		this.onAfterRestoreInstance(oldId, newId, data);

		return newId;
	}

	/**
	 * 저장 전에 해야 하는 일을 ...
	 * 
	 * @param data
	 */
	private <T> void onBeforeRestoreInstance(T data) {
		if (data instanceof Menu) {
			Menu menu = (Menu) data;
			MenuRef parentRef = menu.getParent();

			if (parentRef != null) {
				Map<String, Object> params = SysValueUtil.newMap("domainId,name", menu.getDomainId(), parentRef.getName());
				String parentId = queryManager.selectBySql("select id from menus where domain_id = :domainId and name = :name ", params, String.class);
				if (parentId != null) {
					menu.setParentId(parentId);
				}
			}
		} else if (data instanceof MenuDetail) {
			MenuDetail entity = (MenuDetail) data;
			entity.setMenuId(this.menuIdMapping.get(entity.getMenuId()));
			
		} else if (data instanceof MenuColumn) {
			MenuColumn entity = (MenuColumn) data;
			entity.setMenuId(this.menuIdMapping.get(entity.getMenuId()));
			
		} else if (data instanceof MenuButton) {
			MenuButton entity = (MenuButton) data;
			entity.setMenuId(this.menuIdMapping.get(entity.getMenuId()));
			
		} else if (data instanceof MenuDetailColumn) {
			MenuDetailColumn entity = (MenuDetailColumn) data;
			String detailId = this.menuDetailIdMapping.get(entity.getMenuDetailId());
			entity.setMenuDetailId(SysValueUtil.checkValue(detailId, UUID.randomUUID().toString()));
			
		} else if (data instanceof MenuDetailButton) {
			MenuDetailButton entity = (MenuDetailButton) data;
			String detailId = this.menuDetailIdMapping.get(entity.getMenuDetailId());
			entity.setMenuDetailId(SysValueUtil.checkValue(detailId, UUID.randomUUID().toString()));
			
		} else if (data instanceof Resource) {
			Resource entity = (Resource) data;
			ResourceRef masterRef = entity.getMaster();

			if (masterRef != null) {
				Map<String, Object> params = SysValueUtil.newMap("domainId,name", entity.getDomainId(), masterRef.getName());
				String masterId = this.queryManager.selectBySql("select id from entities where domain_id = :domainId and name = :name ", params, String.class);
				if (masterId != null) {
					entity.setMasterId(masterId);
				}
			}
		} else if (data instanceof ResourceColumn) {
			ResourceColumn entity = (ResourceColumn) data;
			entity.setEntityId(this.entityIdMapping.get(entity.getEntityId()));
			
		} else if (data instanceof CodeDetail) {
			CodeDetail entity = (CodeDetail) data;
			entity.setParentId(this.codeIdMapping.get(entity.getParentId()));
			
		} else if (data instanceof Permission) {
			Permission pmss = (Permission) data;

			String newRoleId = this.roleIdMapping.get(pmss.getRoleId());
			if (newRoleId != null) {
				pmss.setRoleId(newRoleId);
			}

			if (SysValueUtil.isEqual(pmss.getResourceType(), "Menu")) {
				String newMenuId = this.menuIdMapping.get(pmss.getResourceId());
				if (newMenuId != null) {
					pmss.setResourceId(newMenuId);
				}
			}
		}
	}

	/**
	 * 저장 후에 해야 하는 일을 ...
	 * 
	 * @param oldId
	 * @param newId
	 * @param data
	 */
	private <T> void onAfterRestoreInstance(Object oldId, Object newId, T data) {
		if (data instanceof Menu) {
			this.menuIdMapping.put(oldId.toString(), newId.toString());
		} else if (data instanceof MenuDetail) {
			this.menuDetailIdMapping.put(oldId.toString(), newId.toString());
		} else if (data instanceof Role) {
			this.roleIdMapping.put(oldId.toString(), newId.toString());
		} else if (data instanceof Resource) {
			this.entityIdMapping.put(oldId.toString(), newId.toString());
		} else if (data instanceof Code) {
			this.codeIdMapping.put(oldId.toString(), newId.toString());
		}
	}

	/**
	 * 엔티티 오브젝트 data의 id 값을 추출한다.
	 * 
	 * @param data
	 * @return
	 */
	private <T> Object getObjectId(T data) {
		Object dataId = ClassUtil.getFieldValue(data, OrmConstants.ENTITY_FIELD_ID);
		if ((SysValueUtil.isEmpty(dataId)) && (dataId instanceof AbstractStamp)) {
			dataId = ((AbstractStamp) data).findAndSetId();
		}

		return dataId;
	}

}
