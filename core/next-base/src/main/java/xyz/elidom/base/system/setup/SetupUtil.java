package xyz.elidom.base.system.setup;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import xyz.elidom.base.model.SeedEntity;
import xyz.elidom.core.CoreConfigConstants;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.FormatUtil;

/**
 * Setup Utilities
 * 
 * @author shortstop
 */
public class SetupUtil {
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Seed 설정 관련 ...
	/////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * seeds 클래스패스 
	 */
	private static final String SEED_DEFAULT_CLASSPATH = "seeds";
	/**
	 * sys 모듈 클래스패스 
	 */
	private static final String SEED_SYS_MODULE_CLASSPATH = "seeds/xyz/elidom/sys";	
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Seed 설정 관련 ...
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Seed 첨부파일 루트 디렉토리를 설정 
	 * 
	 * @return
	 */
	public static String getStorageRoot() {
		Environment env = BeanUtil.get(Environment.class);
		return env.getProperty("elidom.initial.storage.root", "/storage");
	}
	
	/**
	 * Seed Server를 통한 Setup 작업 여부  
	 * 
	 * @return
	 */
	public static Boolean useSeedServer() {
		String seedType = SettingUtil.getValue("seed.source", "seed-file");
		return !SysValueUtil.isEqual(seedType, "seed-file");
	}
	
	/**
	 * 최초 설치시에 Seed Server를 통해서 작업할 지 여부 
	 * 
	 * @return
	 */
	public static Boolean initialSetupUseSeedServer() {
		Environment env = BeanUtil.get(Environment.class);
		String seedType = env.getProperty("elidom.initial.seed.source", "seed-file");
		return !SysValueUtil.isEqual(seedType, "seed-file");
	}
	
	/**
	 * seed repo id를 리턴 
	 * 
	 * @return
	 */
	public static String getSeedRepoId() {
		Environment env = BeanUtil.get(Environment.class);
		String seedRepoId = SettingUtil.getValue("seed.export.repo.id");
		if(seedRepoId == null) {
			seedRepoId = env.getProperty("elidom.initial.seed.id");
		}
		
		if(SysValueUtil.isEmpty(seedRepoId)) {
			throw ThrowUtil.newNotAllowedEmptyInfo("seed.export.repo.id");
		}
		
		return seedRepoId;
	}
	
	
	public static String getInitSeedrepoId() {
		Environment env = BeanUtil.get(Environment.class);
		String seedRepoId = env.getProperty("elidom.initial.seed.id");
		
		if(SysValueUtil.isEmpty(seedRepoId)) {
			throw ThrowUtil.newNotAllowedEmptyInfo("seed.export.repo.id");
		}
		
		return seedRepoId;
	}
	
	/**
	 * seed base url 리턴 
	 * 
	 * @return
	 */
	public static String getSeedBaseUrl() {
		Environment env = BeanUtil.get(Environment.class);
		String seedBaseUrl = SettingUtil.getValue("seed.server.base.url");
		if(seedBaseUrl == null) {
			seedBaseUrl = env.getProperty("elidom.initial.seed.base_url");
		}
		
		if(SysValueUtil.isEmpty(seedBaseUrl)) {
			throw ThrowUtil.newNotAllowedEmptyInfo("seed.server.base.url");
		}
		
		return seedBaseUrl;
	}
	
	/**
	 * entity data를 업로드를 위한 URL 리턴 
	 * 
	 * @param seedRepoId
	 * @param entityName
	 * @param bundle
	 * @param dataCount
	 * @return
	 */
	public static String getEntityDataUploadUrl(String seedRepoId, String entityName, String bundle, int dataCount) {
		String seedBaseUrl = getSeedBaseUrl();
		return new StringBuffer(seedBaseUrl).append("/seed_repos/").append(seedRepoId).append("/seeds_add/").append(entityName).append("?size=").append(dataCount).append("&bundle=").append(bundle).toString();
	}
	
	/**
	 * Seed Server에서 bundle 정보를 조회하는 URL
	 *  
	 * @param bundle
	 * @return
	 */
	public static String getSeedBundleInfoUrl(String bundle) {
		return new StringBuffer(SetupUtil.getSeedBaseUrl()).append("/seed_repos/").append(SetupUtil.getInitSeedrepoId()).append("/seed_repo_details/").append(bundle).toString();
	}
	
	/**
	 * Seed Data 내용 조회를 위한 URL 
	 * 
	 * @param seedDetailId
	 * @return
	 */
	public static String getSeedDataUrl(String seedDetailId) {
		return new StringBuffer(SetupUtil.getSeedBaseUrl()).append("/seed_repos/").append(SetupUtil.getInitSeedrepoId()).append("/seeds/").append(seedDetailId).toString();
	}
	
	/**
	 * profile 설정 리턴 
	 * 
	 * @return
	 */
	public static String getProfile() {
		return BeanUtil.get(Environment.class).getProperty(CoreConfigConstants.SPRING_PROFILES);
	}
	
	/**
	 * 로컬 파일로 export 하는 모드인지 체크  
	 * 
	 * @return
	 */
	public static boolean isSeedLocalMode() {
		String profile = SetupUtil.getProfile();
		return !SetupUtil.useSeedServer() && SysValueUtil.isEqualIgnoreCase(profile, "development");
	}
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Seed Meta File Read / Write / Upload
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Seed Meta Data 파일을 읽어서 SeedMeta로 파싱 
	 * 
	 * @return
	 */
	public static List<SeedEntity> parseSeedMetaFile() {
		String metaContent = FileUtil.readClasspathFile(SEED_DEFAULT_CLASSPATH, "meta.json");
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
	 * classpath와 fileName으로 파일 내용을 읽어서 content를 리턴한다. 
	 * 
	 * @param classpath
	 * @param fileName
	 * @return
	 */
	public static String readSeedFile(String classpath, String fileName) {
		return FileUtil.readClasspathFile(classpath.replace(OrmConstants.DOT, OrmConstants.SLASH), fileName); 
	}
	
	/**
	 * Seed Data를 Export (To Seed Server / To Seed File) 
	 * 
	 * @param isSeedLocalMode
	 * @param bundle
	 * @param entityClass
	 * @param dataCount
	 * @param content
	 */
	public static void exportSeed(boolean isSeedLocalMode, String bundle, Class<?> entityClass, int dataCount, String content) {
		// 1. Export to Seed Server 
		if(!isSeedLocalMode) {
			exportToSeedServer(bundle, entityClass, dataCount, content);
		// 2. Export to Seed File
		} else {
			exportToSeedFile(bundle, entityClass, content);
		}
	}
	
	/**
	 * Seed Server로 Seed Data Export
	 * 
	 * @param bundle
	 * @param entityClass
	 * @param dataCount
	 * @param content
	 */
	public static void exportToSeedServer(String bundle, Class<?> entityClass, int dataCount, String content) {
		String seedRepoId = SetupUtil.getSeedRepoId();
		String seedBundleInfoUrl = SetupUtil.getEntityDataUploadUrl(seedRepoId, entityClass.getSimpleName(), bundle, dataCount);
		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));
		HttpEntity<String> entity = new HttpEntity<String>(content, headers);
		rest.exchange(seedBundleInfoUrl, HttpMethod.POST, entity, String.class);
	}
	
	/**
	 * Seed File로 Seed Data Export
	 * 
	 * @param bundle
	 * @param entityClass
	 * @param content
	 */
	public static void exportToSeedFile(String bundle, Class<?> entityClass, String content) {
		String seedClasspath = SetupUtil.getSeedFileClassPath(entityClass);
		writeSeedFile(seedClasspath, entityClass.getSimpleName() + ".json", content);
	}
	
	/**
	 * entityClass의 seed classpath 정보를 리턴 
	 * 
	 * @param entityClass
	 * @return
	 */
	public static String getSeedFileClassPath(Class<?> entityClass) {
		return "seeds." + entityClass.getName().replace(".entity." + entityClass.getSimpleName(), OrmConstants.EMPTY_STRING);
	}
	
	/**
	 * classpath와 fileName으로 파일 내용을 읽어서 content를 리턴한다. 
	 * 
	 * @param classpath
	 * @param fileName
	 * @param content
	 * @return
	 */
	public static void writeSeedFile(String classpath, String fileName, String content) {
		classpath = classpath.replace(OrmConstants.DOT, OrmConstants.SLASH);
		URL resourceUrl = SetupUtil.class.getClassLoader().getResource(classpath);
		
		if(resourceUrl == null) {
			resourceUrl = SetupUtil.class.getClassLoader().getResource(SEED_SYS_MODULE_CLASSPATH);
			String tmpClasspath = resourceUrl.getFile();
			classpath = tmpClasspath.replaceAll(SEED_SYS_MODULE_CLASSPATH, classpath);
			
		} else {
			classpath = resourceUrl.getFile();
		}
		
		String seedClasspath = classpath.replace("bin/", "src/main/resources/");
		FileUtil.createFile(seedClasspath, fileName, content);
	}
}
