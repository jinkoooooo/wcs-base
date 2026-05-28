package xyz.elidom.base.system.setup;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import xyz.elidom.base.BaseConstants;
import xyz.elidom.core.CoreConfigConstants;
import xyz.elidom.core.util.BundleUtil;
import xyz.elidom.dbist.annotation.ChildEntity;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.MasterDetailType;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.Setting;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.system.dsl.groovy.QueryDsl;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.FormatUtil;

/**
 * 초기 셋업을 위한 유틸리티 클래스
 * 
 * @author shortstop
 */
@Service
public class InitialSetupFunc {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(InitialSetupFunc.class);

	/**
	 * 초기에 셋업할 임시 도메인 이름
	 */
	private static final String INITIAL_TEMP_DOMAIN_NAME = "[Empty]";

	/**
	 * seeds 클래스패스
	 */
	private static final String SEED_DEFAULT_CLASSPATH = "seeds";

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
	private Map<String, Object> initialParams;

	/**
	 * 초기 데이터 셋업시 생성한 도메인
	 */
	private Domain initialDomain;

	/**
	 * 생성자
	 * 
	 * @param queryManager
	 * @param initialParams
	 */
	public InitialSetupFunc(IQueryManager queryManager, Map<String, Object> initialParams) {
		this.queryManager = queryManager;
		this.initialParams = initialParams;
	}

	/**
	 * 초기 데이터 셋업
	 * 
	 * @return
	 */
	public boolean initialSetup() {
		// 1. Validation Check - Initial Parameters
		this.checkInitialParams();

		// 2. Domain, User 정보 생성
		this.createDomainUserData();

		if (SetupUtil.initialSetupUseSeedServer()) {
			// 3. Seed Server로 부터 Seed 템플릿 데이터를 받아 Seed 데이터 생성
			this.createDataBySeedServer();
		} else {
			// 4. Local Seed 파일로 부터 Seed 데이터 생성
			this.createDataBySeedFiles();
		}

		// 4. Setting 정보 Root Path를 Storage Root 값으로 업데이트
		this.updateStorageRootSetting();

		// 5. Seed가 완료되었다는 표시로 파일 저장
		FileUtil.createFile(".", "seed-ok.txt", "OK");
		return true;
	}
	
	public boolean initialSetup(List<Resource> seedList) {
		// 1. Validation Check - Initial Parameters
		this.checkInitialParams();

		// 2. Domain, User 정보 생성
		this.createDomainUserData();
		
		if (SetupUtil.initialSetupUseSeedServer()) {
			// 3. Seed Server로 부터 Seed 템플릿 데이터를 받아 Seed 데이터 생성
			this.createDataBySeedServer();
		} else {
			// 4. Local Seed 파일로 부터 Seed 데이터 생성
			this.createDataBySeedFiles(seedList);
		}

		// 4. Setting 정보 Root Path를 Storage Root 값으로 업데이트
		this.updateStorageRootSetting();

		// 5. Seed가 완료되었다는 표시로 파일 저장
		FileUtil.createFile(".", "seed-ok.txt", "OK");
		return true;
	}

	/**
	 * 초기 데이터 셋업을 위한 파라미터 설정
	 */
	public void checkInitialParams() {
		// 1. 필요한 파라미터가 존재하는지 체크
		AssertUtil.assertNotEmpty("terms.label.domain_id", this.getInitialValue("domain_id"));
		AssertUtil.assertNotEmpty("terms.label.sub_term_code", this.getInitialValue("domain_name"));
		AssertUtil.assertNotEmpty("terms.label.admin_id", this.getInitialValue("admin_id"));
		AssertUtil.assertNotEmpty("terms.label.email", this.getInitialValue("admin_email"));
		AssertUtil.assertNotEmpty("terms.label.admin_name", this.getInitialValue("admin_name"));
		AssertUtil.assertNotEmpty("terms.label.password", this.getInitialValue("password"));
		AssertUtil.assertNotEmpty("terms.label.url", this.getInitialValue("domain_url"));

		if (!this.initialParams.containsKey("locale")) {
			this.initialParams.put("locale", SettingUtil.getValue(CoreConfigConstants.DEFAULT_LOCALE, "ko-KR"));
		}

		// 2. 도메인 체크
		Long domainId = SysValueUtil.toLong(this.getInitialValue("domain_id"));
		String domainName = this.getInitialValue("domain_name");
		String domainUrl = this.getInitialValue("domain_url");
		this.validateDomain(domainId, domainName, domainUrl);

		// 3. 사용자 체크
		String userId = this.getInitialValue("admin_id");
		String userEmail = this.getInitialValue("admin_email");
		String userName = this.getInitialValue("admin_name");
		String password = this.getInitialValue("password");
		this.validateUser(userId, userEmail, userName, password);
	}

	/**
	 * 도메인 & 사용자 데이터 생성
	 * 
	 * @return
	 */
	private void createDomainUserData() {
		// 1. 도메인 생성
		QueryDsl dsl = new QueryDsl(Domain.class);
		this.initialDomain = dsl.findBy("name", INITIAL_TEMP_DOMAIN_NAME);
		this.initialDomain.setName(this.getInitialValue("domain_name"));
		this.initialDomain.setBrandName(this.getInitialValue("brand_name"));
		this.initialDomain.setSubdomain(this.getInitialValue("domain_url"));
		dsl.update(this.initialDomain);

		// 2. 사용자 생성
		String password = this.initialParams.containsKey("password") ? this.getInitialValue("password") : this.getInitialValue("admin_name");
		PasswordEncoder passwordEncoder = BeanUtil.get(PasswordEncoder.class);
		String encrypted = passwordEncoder.encode(password);

		dsl.initByEntityClass(User.class);
		dsl.createByKeyValues(
				"domainId,id,login,name,email,adminFlag,activeFlag,superUser,password,encryptedPassword,locale",
				this.initialDomain.getId(),
				this.getInitialValue("admin_id"),
				this.getInitialValue("admin_id"),
				this.getInitialValue("admin_name"),
				this.getInitialValue("admin_email"),
				true,
				true,
				true,
				password,
				encrypted,
				this.getInitialValue("locale"));
	}

	
	/**
	 * Seed 파일로 부터 데이터 생성
	 * 
	 * @return
	 */
	private boolean createDataBySeedFiles(List<Resource> seedList) {
		try {
			for(Resource resource : seedList) {
				String resourcePath = resource.getURL().getPath();
				String fileName = resource.getFilename();
				
				if(fileName.equalsIgnoreCase("meta.json")) {
					continue;
				}
				
				String entityPath = resourcePath.substring(resourcePath.indexOf("/seeds/")+ 7) ;
				entityPath = entityPath.replaceAll(fileName, "").replaceAll("/", ".") + "entity";
				fileName = fileName.replaceAll(".json", "");
				entityPath = entityPath + "." + fileName;
				
				Class<?> entityClass = ClassUtil.forName(entityPath);
				
				String entityName = entityClass.getSimpleName();
				Table table = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);
				
				String content = IOUtils.toString(resource.getInputStream(), BaseConstants.CHAR_SET_UTF8);

				// Seed 데이터 파일로 데이터를 생성하여 데이터베이스에 데이터 추가
				if (content != null && content.length() > 6) {
					List<Field> fieldList = ClassUtil.getAllFields(new ArrayList<Field>(), entityClass);
					List<?> dataList = this.parseData(entityClass, table, fieldList, content);
					if (dataList != null && !dataList.isEmpty()) {
						int successCount = this.restoreData(dataList);
						logger.info("Restored [" + entityName + "] Data [" + successCount + "] Count!");
					}
				} else {
					logger.info("Restored [" + entityName + "] Data [0] Count!");
				}
			}
		} catch (Exception e) {
			throw new ElidomServiceException(e);
		}

		return true;
	}

	
	/**
	 * Seed 파일로 부터 데이터 생성
	 * 
	 * @return
	 */
	private boolean createDataBySeedFiles() {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		List<IModuleProperties> orderedModules = configSet.allOrderedModules();

		try {
			URI uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
			String seedRootPath = SysValueUtil.checkValue(uri.getPath(), new String()).concat(SEED_DEFAULT_CLASSPATH);

			// Seed File 목록 가져오기 (Project & External Files)
			Map<String, Object> seedDataMap = this.getSeedFile(seedRootPath);

			// 모듈별로 순회하며 Seed Data 생성.
			for (IModuleProperties module : orderedModules) {
				ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
				scanner.addIncludeFilter(new AnnotationTypeFilter(xyz.elidom.dbist.annotation.Table.class));

				for (BeanDefinition bd : scanner.findCandidateComponents(module.getScanEntityPackage())) {
					Class<?> entityClass = ClassUtil.forName(bd.getBeanClassName());
					String entityName = entityClass.getSimpleName();
					Table table = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);

					// Jar내에 존재하는 Seed 정보를 seedDataMap에 Inputstream 형태로 저장.
					if (!seedDataMap.containsKey(entityName) && !table.isRef()) {
						try {
							StringJoiner seedFileName = new StringJoiner(OrmConstants.SLASH);
							seedFileName.add(SEED_DEFAULT_CLASSPATH).add(entityName + ".json");

							InputStream inputStream = getClass().getClassLoader().getResourceAsStream(seedFileName.toString());
							if (SysValueUtil.isNotEmpty(inputStream))
								seedDataMap.put(entityName, inputStream);
						} catch (Exception e) {
							logger.warn(e.getMessage());
						}
					}

					// Entity에 해당하는 Seed Data가 존재하지 않을 경우 Continue.
					Object data = seedDataMap.get(entityName);
					if (SysValueUtil.isEmpty(data)) {
						continue;
					}

					// Seed Data 생성.
					String content;
					if (data instanceof File) {
						content = new String(Files.readAllBytes(Paths.get(((File) data).toString())), BaseConstants.CHAR_SET_UTF8);
					} else {
						content = IOUtils.toString((InputStream) data, BaseConstants.CHAR_SET_UTF8);
					}

					// Seed 데이터 파일로 데이터를 생성하여 데이터베이스에 데이터 추가
					if (content != null && content.length() > 6) {
						List<Field> fieldList = ClassUtil.getAllFields(new ArrayList<Field>(), entityClass);
						List<?> dataList = this.parseData(entityClass, table, fieldList, content);
						if (dataList != null && !dataList.isEmpty()) {
							int successCount = this.restoreData(dataList);
							logger.info("Restored [" + entityName + "] Data [" + successCount + "] Count!");
						}
					} else {
						logger.info("Restored [" + entityName + "] Data [0] Count!");
					}
				}
			}
		} catch (Exception e) {
			throw new ElidomServiceException(e);
		}

		return true;
	}

	/**
	 * Seed 서버로 부터 데이터 생성
	 * 
	 * @return
	 */
	private boolean createDataBySeedServer() {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		List<IModuleProperties> orderedModules = configSet.allOrderedModules();

		// 모듈별로 순회하며
		for (IModuleProperties module : orderedModules) {
			this.createModuleDataBySeedServer(module);
		}

		return true;
	}

	/**
	 * 경로이 있는 Seed File 목록 가져오기 실행.
	 * 
	 * @param rootPath
	 * @param subPath
	 * @return <EntityName, File>
	 */
	private Map<String, Object> getSeedFile(String rootPath) {
		Map<String, Object> fileMap = new HashMap<String, Object>();

		File file = new File(rootPath);
		if (!file.isDirectory()) { return fileMap; }

		File[] remainderFiles = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String extension = name.substring(name.lastIndexOf(OrmConstants.DOT) + 1, name.length());
				return SysValueUtil.isEqualIgnoreCase(extension, "json");
			}
		});

		if (SysValueUtil.isEmpty(remainderFiles)) { return fileMap; }

		for (File seedFile : remainderFiles) {
			String name = StringUtils.substring(seedFile.getName(), 0, seedFile.getName().lastIndexOf(OrmConstants.DOT));
			fileMap.put(name, seedFile);
		}

		return fileMap;
	}

	/**
	 * 모듈별로 Seed 템플릿 데이터를 다운로드 받아서 Seed 데이터를 생성
	 * 
	 * @param module
	 */
	@SuppressWarnings("unchecked")
	private void createModuleDataBySeedServer(IModuleProperties module) {
		// 1. 서버에 Seed 리스트 호출
		String bundle = module.getName();
		String seedBundleInfoUrl = SetupUtil.getSeedBundleInfoUrl(bundle);
		RestTemplate rest = new RestTemplate();
		Object result = rest.getForObject(seedBundleInfoUrl, Object.class);
		List<Map<String, String>> seedInfoList = (List<Map<String, String>>) result;

		// 2. Seed 정보를 순회하면서 실제 Seed Data를 서버로 부터 받아와서 Seed 수행
		for (Map<String, String> seedInfo : seedInfoList) {
			String seedDetailId = SysValueUtil.toString(seedInfo.get("id"));
			String entityName = seedInfo.get("entity_name");
			String entityClassName = BundleUtil.getEntityClassName(bundle, entityName);
			Class<?> entityClass = ClassUtil.forName(entityClassName);
			Table table = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);

			String seedDataUrl = SetupUtil.getSeedDataUrl(seedDetailId);
			String content = rest.getForObject(seedDataUrl, String.class);

			if (table != null) {
				// user 중복 문제로 인해 삭제 후진행
				if(SysValueUtil.isEqual(entityClass, User.class)) {
					this.queryManager.deleteByCondition(entityClass, SysValueUtil.newMap(""));
				}
				
				List<Field> fieldList = ClassUtil.getAllFields(new ArrayList<Field>(), entityClass);
				List<?> dataList = this.parseData(entityClass, table, fieldList, content);
				if (dataList != null && !dataList.isEmpty()) {
					int successCount = this.restoreData(dataList);
					logger.info("Exporting [" + entityName + "] Data [" + successCount + "] Count!");
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// Private Methods
	/////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * JSON Data를 파싱하여 데이터 생성
	 * 
	 * @param entityClass
	 * @param table
	 * @param fieldList
	 * @param content
	 * @return
	 */
	private List<?> parseData(Class<?> entityClass, Table table, List<Field> fieldList, String content) {
		JSONArray dataArray = FormatUtil.parseJsonArray(content);
		return this.parseMasterOnly(dataArray, entityClass, table, fieldList);
	}

	/**
	 * Master Entity로만 구성된 데이터 파싱
	 * 
	 * @param dataArray
	 * @param entityClass
	 * @param table
	 * @param fieldList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Object> parseMasterOnly(JSONArray dataArray, Class<?> entityClass, Table table, List<Field> fieldList) {
		List<Object> dataList = new ArrayList<Object>();

		for (int i = 0; i < dataArray.size(); i++) {
			String jsonString = dataArray.get(i).toString();
			Map<String, Object> master = FormatUtil.jsonToObject(jsonString, HashMap.class);
			Object instance = convertObjectBeforeRestore(entityClass, table, fieldList, master);
			dataList.add(instance);
		}

		return dataList;
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

			// 1. master 처리
			Map<String, Object> master = (Map<String, Object>) source.get("master");
			Object masterInstance = this.convertObjectBeforeRestore(entityClass, table, masterFieldList, master);
			target.put("master", masterInstance);

			// 2. details 처리
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
	private void parseDetailWithSubEntity(
			ChildEntity[] childEntities,
			Map<String, List<Field>> detailClassFieldsMap,
			Map<String, Table> detailClassTablesMap,
			Map<String, Object> source,
			Map<String, Object> target) {

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
						detailList.add(this.convertObjectBeforeRestore(detailClass, detailTable, detailFieldList, subData));
					}

					target.put(dataProp, detailList);

					// Case 2 - Master - Detail 1 : 1 관계
				} else {
					Map<String, Object> subData = (Map<String, Object>) source.get(dataProp);
					Object detailInstance = this.convertObjectBeforeRestore(detailClass, detailTable, detailFieldList, subData);
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
	 * @param dataList
	 * @return
	 */
	private int restoreData(List<?> dataList) {
		if (SysValueUtil.isEmpty(dataList)) { return 0; }

		for (Object data : dataList) {
			this.restoreInstance(data);
		}

		return dataList.size();
	}

	/**
	 * DomainId, ID를 새롭게 설정하고 데이터베이스에 저장
	 * 
	 * @param data
	 * @return object의 id를 리턴
	 */
	private Object restoreInstance(Object data) {
		ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_DOMAIN_ID, this.initialDomain.getId());

		this.onBeforeRestoreInstance(data);
		this.queryManager.insert(data);
		this.onAfterRestoreInstance(data);

		return ClassUtil.getFieldValue(data, OrmConstants.ENTITY_FIELD_ID);
	}

	/**
	 * 저장 전에 해야 하는 일을 ...
	 * 
	 * @param data
	 */
	private void onBeforeRestoreInstance(Object data) {}

	/**
	 * 저장 후에 해야 하는 일을 ...
	 * 
	 * @param data
	 */
	private void onAfterRestoreInstance(Object data) {}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// Validation Check
	/////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 도메인 관련 정보 유효성 체크
	 * 
	 * @param domainSetup
	 * @param domainId
	 * @param domainName
	 * @param domainUrl
	 * @return
	 */
	private Boolean validateDomain(Long domainId, String domainName, String domainUrl) {
		// 1. domainName으로 도메인이 이미 존재하는지 체크
		Domain condition = new Domain();
		condition.setName(domainName);
		Domain domain = this.queryManager.selectByCondition(Domain.class, condition);
		if (domain != null) { throw ThrowUtil.newDataDuplicated("terms.label.domain", domainName); }

		// 2. domainUrl로 도메인이 이미 존재하는지 체크
		condition.setName(null);
		condition.setSubdomain(domainUrl);
		domain = this.queryManager.selectByCondition(Domain.class, condition);

		if (domain != null) { throw ThrowUtil.newDataDuplicated("terms.label.domain_url", domainUrl); }

		// 3. Domain URL 정규표현식으로 체크
		/*
		 * Pattern urlPattern = Pattern.compile("([^:\\/\\s]+)");
		 * Matcher matcher = urlPattern.matcher(domainUrl);
		 * if (!matcher.matches()) {
		 * AssertUtil.throwInvalidParams("Domain URL", domainUrl);
		 * }
		 */

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
		if (null != queryManager.select(User.class, userId)) { throw ThrowUtil.newDataDuplicated("terms.label.user_id", userId); }

		// 2. email 정규표현식으로 체크
		/*Pattern emailPattern = Pattern.compile("[_0-9a-zA-Z-]+[_a-z0-9-.]{2,}@[a-z0-9-]{2,}(.[a-z0-9]{2,})*");
		Matcher matcher = emailPattern.matcher(email);
		if (!matcher.matches()) {
			AssertUtil.throwInvalidParams("terms.label.email", email);
		}*/

		// 3. email 중복 체크
		if (null != User.getUserByEmail(email)) { throw ThrowUtil.newDataDuplicated("terms.label.user", email); }

		return true;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 기 타
	/////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * initialParmas에 포함된 key 데이터를 찾아 리턴
	 * 
	 * @param key
	 * @return
	 */
	private String getInitialValue(String key) {
		return this.initialParams.containsKey(key) ? (String) this.initialParams.get(key) : null;
	}

	/**
	 * Storage Root 설정을 파라미터로 받은 값으로 업데이트
	 */
	private void updateStorageRootSetting() {
		String storageRoot = SetupUtil.getStorageRoot();

		if (SysValueUtil.isNotEmpty(storageRoot)) {
			Setting condition = new Setting("file.root.path");
			Setting storageRootSetting = this.queryManager.selectByCondition(Setting.class, condition);

			if (storageRootSetting == null) {
				condition.setValue(storageRoot);
				this.queryManager.insert(condition);
			} else {
				storageRootSetting.setValue(storageRoot);
				this.queryManager.update(storageRootSetting);
			}
		}
	}
}
