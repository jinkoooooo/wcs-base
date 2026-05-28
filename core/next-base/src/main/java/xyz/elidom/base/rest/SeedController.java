package xyz.elidom.base.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import net.sf.common.util.ValueUtils;
import xyz.elidom.base.BaseConstants;
import xyz.elidom.base.system.setup.DomainDeleteFunc;
import xyz.elidom.base.system.setup.DomainExportFunc;
import xyz.elidom.base.system.setup.DomainSetupFunc;
import xyz.elidom.base.system.setup.InitialSetupFunc;
import xyz.elidom.base.system.setup.SyncTableFunc;
import xyz.elidom.core.CoreConfigConstants;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.ddl.InitialSetup;
import xyz.elidom.dbist.dml.impl.DmlJdbc2;
import xyz.elidom.exception.client.ElidomInvalidParamsException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * 데이터베이스 셋업이 안 된 상태에서 초기 셋업에 필요한 Seed 데이터를 생성하기 위한 컨트롤러
 * 
 * @author shortstop
 */
@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/seeds")
@ServiceDesc(description = "Data Initializer Service API")
public class SeedController implements InitialSetup {

	/**
	 * Logger
	 */
	protected Logger logger = LoggerFactory.getLogger(SeedController.class);

	@Autowired
	protected IQueryManager queryManager;
	
	@Autowired
	protected Ddl ddl;
	
	/**
	 * 초기에 셋업할 임시 도메인 이름  
	 */
	private static final String INITIAL_TEMP_DOMAIN_NAME = "[Empty]";

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Export Domain API
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	@ApiDesc(description = "Export Seed")
	@GetMapping(value = "/export/{module}", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean exportDomainData() {
		DomainExportFunc exportFunc = new DomainExportFunc(this.queryManager);
		return exportFunc.exportDomainData();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Delete Domain API
	/////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * 도메인 삭제 
	 * 	프로세스 
	 * 		1. /domain/{id}/validate_truncation (Return Token)
	 * 		2. /domain/{id}/truncate (Check Token)
	 * 
	 * @param id
	 * @param deleteParams
	 * @return
	 */
	@ApiDesc(description = "도메인 및 도메인 관련 데이터 삭제")
	@DeleteMapping(value = "/domain/{id}/truncate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public Boolean deleteDomain(@PathVariable("id") Long id, @RequestBody Map<String, Object> deleteParams) {
		// 1. Validation Check
		Domain domain = this.queryManager.select(true, new Domain(id));
		deleteParams.put("domain_name", domain.getName());
		this.checkBasicTransactionParams(deleteParams);

		// 2. 삭제 
		DomainDeleteFunc deleteFunc = new DomainDeleteFunc(this.queryManager);
		return deleteFunc.deleteDomain(id, deleteParams);
	}
	
	@ApiDesc(description = "도메인 삭제 전 유효성 체크 API")
	@GetMapping(value = "/domain/{id}/validate_truncation", produces = MediaType.APPLICATION_JSON_VALUE)
	public String validateDomainTruncation(@PathVariable("id") Long id) {
		Domain domain = this.queryManager.select(true, new Domain(id));
		
		if(domain.getSystemFlag()) {
			throw ThrowUtil.newCannotDeleteSystemDomain(domain.getName());
		}

		return "name";
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Domain Setup API
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	@ApiDesc(description = "도메인 셋업 전 유효성 체크 API")
	@PostMapping(value = "/domain/validate_setup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String validateDomainSetup(@RequestBody Map<String, Object> setupParams) {
		DomainSetupFunc setupFunc = new DomainSetupFunc(this.queryManager, setupParams);
		setupFunc.validateDomainSetup();
		return "name";
	}
	
	@PostMapping(value = "/domain/setup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ApiDesc(description = "데이터 초기화된 상태에서 새로운 도메인 셋업")
	public Boolean setupDomain(@RequestBody Map<String, Object> setupParams) {
		// 1. Validation Check
		this.checkBasicTransactionParams(setupParams);
		
		// 2. setup logic
		DomainSetupFunc setupFunc = new DomainSetupFunc(this.queryManager, setupParams);
		return setupFunc.setupDomain();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Initial Setup API
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean initialSetup(Environment env) {
		boolean ddlEnable = ValueUtils.toBoolean(env.getProperty("dbist.ddl.enable", "true"));
		if(ddlEnable) {
			new SyncTableFunc(env, this.ddl).sync();
		}
		
		boolean isSetup = ValueUtils.toBoolean(env.getProperty("elidom.initial.setup", "false"));
		if(isSetup) {
			return this.initialSetupByEnvironment(env, null);
		} else {
			return true;
		}
	}
	
	@ApiDesc(description = "데이터 초기화가 완료된 상태인지 체크")
	@GetMapping(value = "/is_initial_setup", produces = MediaType.APPLICATION_JSON_VALUE)
	public Boolean isInitialSetup() {
		try {
			return this.queryManager.selectSizeBySql("SELECT ID FROM DOMAINS", null) == 0 ? false : true;
		} catch (Exception e) {
			this.logger.error(e.getMessage());
			return false;
		}
	}
	
	@ApiDesc(description = "데이터 초기화가 안된 상태에서 최초로 데이터 초기화 API")
	@PostMapping(value = "/initial_setup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Boolean initialSetup(@RequestBody Map<String, Object> initialParams) {
		// 1. Validation Check
		this.checkBasicTransactionParams(initialParams);

		// 2. 초기 데이터 셋업 
		InitialSetupFunc setup = new InitialSetupFunc(queryManager, initialParams);
		return setup.initialSetup();
	}	
	
	@ApiDesc(description = "데이터 초기화 전 유효성 체크 API")
	@PostMapping(value = "/validate_initial_setup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String validateInitialSetup(@RequestBody Map<String, Object> initialParams) {
		// 1. 기본 Validation Check
		this.checkBasicTransactionParams(initialParams);

		// 2. 초기 데이터 셋업 체크 
		InitialSetupFunc setup = new InitialSetupFunc(queryManager, initialParams);
		setup.checkInitialParams();
		return "name";
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 									Private Method
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 환경설정 프로퍼티 파일로 부터 초기 셋업 정보를 읽어서 initial setup
	 * 
	 * @param env
	 * @param module
	 * @return
	 * @throws Exception 
	 */
	public Boolean initialSetupByEnvironment(Environment env, String module) {
		// 1. initial setup인지 체크 
		if (this.isInitialSetup()) {
			return false;
		}
		
		// 3. 초기 데이터 셋업을 위한 설정 체크 
		this.checkInitialSetupProperites(env);

		// 4. Seed 생성을 위한 파라미터 설정 
		Map<String, Object> initialParams = this.buildInitialParams(env);
		
		// 5. 기본 Validation Check
		this.checkBasicTransactionParams(initialParams);
		
		// 6. 속도를 위해 엔티티 저장 시 Validation Check를 할 지 여부를 설정한다. 
		boolean entityValidateBeforeSave = SysValueUtil.toBoolean(env.getProperty(CoreConfigConstants.DBIST_VALIDATE_BEFORE_SAVE, SysConstants.FALSE_STRING));
		SettingUtil.setEntityValidateBeforeSave(entityValidateBeforeSave);
		
		// 7. db Object 생성 
		boolean useObjectScript = SysValueUtil.toBoolean(env.getProperty("elidom.initial.setup.script", SysConstants.FALSE_STRING));
		if(useObjectScript) {
			this.createDatabaseExtensions();
		}
		
		// 8. 임시 도메인 생성
		Domain emptyDomain = new Domain(INITIAL_TEMP_DOMAIN_NAME);
		emptyDomain.setId(SysValueUtil.toLong(env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_ID)));
		emptyDomain.setSystemFlag(true);
		emptyDomain.setSubdomain(INITIAL_TEMP_DOMAIN_NAME);
		BeanUtil.get(IQueryManager.class).insert(emptyDomain);
		Domain.setCurrentDomain(emptyDomain);
		
		// 9. Setup initial data
		return this.initSeedData(initialParams);
	}
	
	/**
	 * Seed 데이터 셋업
	 * 
	 * @param initialParams
	 * @return
	 */
	boolean initSeedData(Map<String, Object> initialParams) {
		try {
			// 1. 전체 classpath의 스크립트 불러오기 
			List<Resource> mainScriptResources = this.getSeedScriptResources(true, null);
			List<Resource> subScriptResources = this.getSeedScriptResources(true, mainScriptResources);
			
			// 2. project 모듈 순서 리스트 
			List<String> projectOrderList = this.getProjectOrderdList();
			
			// 3. 모듈과 resource 병합
			List<Resource> executeResources = this.mergeScriptResource(projectOrderList, mainScriptResources, subScriptResources);
			
			// 4. initial setup
			InitialSetupFunc setup = new InitialSetupFunc(this.queryManager, initialParams);
			return setup.initialSetup(executeResources);
			
		} catch(Exception e) {
			throw new ElidomServiceException(e);
		}
	}
	
	/**
	 * resource script 실행
	 * Application Event 의 제일 마지막에 실행 한다. 
	 * 1. tables
	 * 2. sequences
	 * 3. dblinks
	 * 4. views
	 * 5. types
	 * 6. functions
	 * 7. procedures
	 * 8. schedulers
	 * 9. etc ( 기타 다른 것들 .... )
	 * @throws Exception
	 */
	void createDatabaseExtensions() {
		try {
			List<String> scriptSeqList = SysValueUtil.newStringList("/tables/","/sequences/","/dblinks/","/views/","/types/","/functions/","/procedures/","/schedulers/","/etc/");
			
			// 1. 전체 classpath 의 스크립트 불러오기 
			List<Resource> mainScriptResources = this.getDBScriptResources(true, null);
			List<Resource> subScriptResources = this.getDBScriptResources(false, mainScriptResources);
			
			// 2. project 모듈 순서 리스트 
			List<String> projectOrderList = this.getProjectOrderdList();
			
			
			// 3. 모듈과 resource 합
			List<Resource> executeResources = this.mergeScriptResource(projectOrderList, mainScriptResources, subScriptResources);
		
			// 4. script Sequence 에 따라 loop
			for(String scriptSeq : scriptSeqList) {
				logger.info(scriptSeq + " ================ ");
				for(Resource resource: executeResources) {
					if(resource.getURL().getPath().contains(scriptSeq)) {
						String script = IOUtils.toString(resource.getInputStream(), SysConstants.CHAR_SET_UTF8);
						try {
							this.queryManager.executeBySql(script, null);
						} catch(Exception e) {
							this.logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>");
							this.logger.error(scriptSeq, e);
							this.logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>");
						}
					}
				}
			}			
		} catch(Exception e) {
			throw new ElidomServiceException(e);
		}
	}
	
	/**
	 * db 하위 폴더의 리소스 script 파일을 검색한다.
	 * 
	 * @param isMainApps
	 * @param mainScriptResources
	 * @return
	 * @throws Exception
	 */
	private List<Resource> getSeedScriptResources(boolean isMainApps, List<Resource> mainScriptResources) throws Exception {
		String searchPattern = "classpath*:/seeds/**/*.json";
		if(isMainApps) searchPattern = "classpath:/seeds/**/*.json";

		if(isMainApps) return this.getScriptResources(searchPattern, null);
		else return this.getScriptResources(searchPattern, mainScriptResources);
	}
	
	/**
	 * db 하위 폴더의 리소스 script 파일을 검색한다.
	 * 
	 * @param isMainApps
	 * @param mainScriptResources
	 * @return
	 * @throws Exception
	 */
	private List<Resource> getDBScriptResources(boolean isMainApps, List<Resource> mainScriptResources) throws Exception {
		String dbType = this.queryManager.getDbType().toLowerCase();
		
		String searchPattern = "classpath*:/db/%s/**/*.script";
		if(isMainApps) searchPattern = "classpath:/db/%s/**/*.script";
		searchPattern = String.format(searchPattern, dbType);

		if(isMainApps) return this.getScriptResources(searchPattern, null);
		else return this.getScriptResources(searchPattern, mainScriptResources);
	}
	
	/**
	 * main 프로젝트와 서브 프로젝트의 스크립트를 머지한다.
	 * 
	 * @param projectOrderList
	 * @param mainScriptResources
	 * @param subScriptResources
	 * @return
	 * @throws Exception
	 */
	private List<Resource> mergeScriptResource(List<String> projectOrderList, List<Resource> mainScriptResources, List<Resource> subScriptResources) throws Exception {
		
		List<Resource> orderdList = new ArrayList<Resource>();
		List<Resource> result = new ArrayList<Resource>();
		List<String> filterFileNames = new ArrayList<String>();
		
		// 1. boot project 
		for(Resource resource : mainScriptResources) {
			filterFileNames.add(resource.getFilename());
			result.add(resource);
		}
		
		// 2. sub resource 정렬 
		for(int i = 1; i < projectOrderList.size() ;i++) {
			for(Resource resource : subScriptResources) {
				if(resource.getURL().getPath().contains(projectOrderList.get(i))) {
					orderdList.add(resource);
				}
			}
		}
		
		// 3. sub resource script filter 
		for(Resource resource : orderdList) {
			if(filterFileNames.stream().filter(x-> x.equals(resource.getFilename())).count() == 0) {
				filterFileNames.add(resource.getFilename());
				result.add(resource);
			}
		}
		
		return result;
	}

	/**
	 * 전체 프로젝트의 ( dependency jar 도 포함 ) script 파일을 검색한다.
	 * 
	 * @param mainScriptResources
	 * @return
	 * @throws Exception
	 */
	private List<Resource> getScriptResources(String searchPattern, List<Resource> mainScriptResources) throws Exception {
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources(searchPattern);
		List<Resource> resourceList = new ArrayList<Resource>();
		
		if(mainScriptResources == null) {
			for(Resource resource : resources) {
				if(resource == null || !resource.exists()) continue;
				resourceList.add(resource);
			}
			
		} else {
			for(Resource resource : resources) {
				if(resource == null || !resource.exists()) continue;
				String fullPath = resource.getURL().getPath();
				
				boolean isMainResource = false;
				for(Resource mainResource : mainScriptResources) {
					if(fullPath.equals(mainResource.getURL().getPath())) {
						isMainResource = true;
						break;
					}
				}
				
				if(!isMainResource) resourceList.add(resource);
			}
		}
		
		return resourceList;
	}
	
	/**
	 * 프로젝트의 순서를 구한다.
	 * index : 0 = main Project 
	 *         1 ~ = sub Project
	 * @return
	 */
	private List<String> getProjectOrderdList() {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		List<IModuleProperties> modules = configSet.allOrderedModules();
		List<String> projectOrder = new ArrayList<String> ();
		
		for(IModuleProperties module : modules) {
			projectOrder.add(module.getProjectName());
		}
		
		IModuleProperties mainModule = configSet.getApplicationModule();
		String mainProjectName = mainModule.getProjectName();
		
		if(!projectOrder.contains(mainProjectName)) {
			projectOrder.add(mainProjectName);
		}
		
		Collections.reverse(projectOrder);
		
		return projectOrder;
	}
	
	/**
	 * 초기 데이터 셋업을 위한 설정이 빠진 게 있는지 체크 
	 * 
	 * @param env
	 */
	private void checkInitialSetupProperites(Environment env) {
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_ID, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_ID));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_NAME, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_NAME));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_BRAND_NAME, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_BRAND_NAME));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_URL, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_URL));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_ID, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_ID));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_NAME, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_NAME));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_EMAIL, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_EMAIL));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_PASSWD, env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_PASSWD));
		AssertUtil.assertNotEmpty(CoreConfigConstants.ELIDOM_INITIAL_STORAGE_ROOT,	env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_STORAGE_ROOT));
		
	}
	
	/**
	 * 초기데이터 생성을 위해 Initial Parameters 생성 
	 * 
	 * @param env
	 * @return
	 */
	private Map<String, Object> buildInitialParams(Environment env) {
		Map<String, Object> seedParams = SysValueUtil.newMap("module", "all");
		seedParams.put("domain_id", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_ID));
		seedParams.put("domain_name", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_NAME));
		seedParams.put("brand_name", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_BRAND_NAME));
		seedParams.put("domain_url", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_DOMAIN_URL));
		seedParams.put("admin_id", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_ID));
		seedParams.put("admin_name", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_NAME));
		seedParams.put("admin_email", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_EMAIL));
		seedParams.put("password", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_ADMIN_PASSWD));
		seedParams.put("storage_root", env.getProperty(CoreConfigConstants.ELIDOM_INITIAL_STORAGE_ROOT));
		PasswordEncoder passwordEncoder = BeanUtil.get(PasswordEncoder.class);
		String password = passwordEncoder.encode((String) seedParams.get("domain_name"));
		seedParams.put("token",	password);
		return seedParams;
	}
	
	/**
	 * dbType, resourcePath, fileName으로 스크립트 파일을 읽어서 내용을 리턴 
	 * 
	 * @param dbType
	 * @param path
	 * @param fileName
	 * @return
	 */
	private String readScriptContent(String dbType, String resourcePath, String fileName) {
		resourcePath = resourcePath.replace(OrmConstants.DOT, OrmConstants.SLASH);
		String classpath = resourcePath + BaseConstants.SLASH + fileName;
		return FileUtil.readClassPathResource(classpath);
	}
	
	/**
	 * dbType, resourcePath, script fileName으로 classpath로 부터 파일 내용을 읽어서 ddl을 수행한다. 
	 * 
	 * @param dml
	 * @param dbType
	 * @param resourcePath
	 * @param fileName
	 */
	private void excuteScripts(DmlJdbc2 dml, String dbType, String resourcePath, String fileName) {
		String sql = null;
		try {
			sql = this.readScriptContent(dbType, resourcePath, fileName);
			if (sql == null)
				return;

		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			return;
		}
		
		String separator = "\n";
		StringBuffer scriptBuf = new StringBuffer();
		String[] sqlArr = sql.split(separator);
		boolean isSqlServerType = SysValueUtil.isEqual(dbType, "sqlserver");
		
		for(String sqlStr : sqlArr) {
			if(SysValueUtil.isEmpty(sqlStr)) {
				continue;
			}
			
			if(isSqlServerType && SysValueUtil.isEqual(sqlStr.trim().toUpperCase(), "GO")) {
				sqlStr = OrmConstants.SEMI_COLON;
			}
			
			// 주석 제거 
			if(!sqlStr.startsWith("--")) {
				scriptBuf.append(sqlStr).append(separator);
			}
		}
		
		String[] scriptArr = StringUtils.tokenizeToStringArray(scriptBuf.toString(), OrmConstants.SEMI_COLON);
		JdbcOperations operation = dml.getJdbcOperations();
		SeedController seedCtrl = BeanUtil.get(SeedController.class);
		
		for (String script : scriptArr) {
			seedCtrl.runDdlScript(operation, script);
		}
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void runDdlScript(JdbcOperations operation, String script) {
		try {
			operation.execute(script);
		} catch (Exception e) {
			this.logger.error("Failed to execute database ddl scripts", e);
		}		
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Validation
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 기본 트랜잭션 파라미터 체크 
	 * 
	 * @param setupParams
	 */
	private void checkBasicTransactionParams(Map<String, Object> setupParams) {
		String token = this.assertValidTrxParams(setupParams);
		String domainName = (String)setupParams.get("domain_name");
		this.assertValidToken(token, domainName);
	}
	
	/**
	 * 트랜잭션 처리 전 파라미터가 유효한 지 체크 
	 * 
	 * @param setupParams
	 */
	private String assertValidTrxParams(Map<String, Object> setupParams) {
		if (SysValueUtil.isEmpty(setupParams)) {
			throw new ElidomInvalidParamsException("Empty Invalid initialization parameters!");
		}

		AssertUtil.assertNotEmpty("terms.label.token", setupParams.get("token"));
		return (String)setupParams.get("token");
	}
	
	/**
	 * token이 유효한 지 체크 
	 * 
	 * @param token
	 * @param encryptSource
	 */
	private void assertValidToken(String token, String encryptSource) {
		PasswordEncoder passwordEncoder = BeanUtil.get(PasswordEncoder.class);
		String encrypted = passwordEncoder.encode(encryptSource);

		if(SysValueUtil.isNotEqual(token, encrypted)) {
			throw new ElidomInvalidParamsException("Invalid token!");
		}
	}
	
	/**
	 * 서버 모드가 개발 모드인지 체크 
	 * 
	 * @return
	 */
	/*private void assertDevelopmentServerMode() {
		String profile = BeanUtil.get(Environment.class).getProperty(CoreConfigConstants.SPRING_PROFILES);
		String mode = ValueUtil.isNotEmpty(profile) ? profile : CoreConfigConstants.PROFILE_DEVELOPMENT_MODE;
		
		if(ValueUtil.isNotEqual(mode, CoreConfigConstants.PROFILE_DEVELOPMENT_MODE)) {
			throw ThrowUtil.newNotSupportFunctionOnlyDevMode();
		}
	}*/
	
}
