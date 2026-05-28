package xyz.elidom.base.system.setup;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;

import net.sf.common.util.ValueUtils;
import xyz.elidom.dbist.annotation.DataSourceType;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.orm.util.DdlUtil;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.ValueUtil;

/**
 * 테이블 동기화를 위한 유틸리티 클래스 
 * 
 * @author shortstop
 */
public class SyncTableFunc {

	private Environment env;
	
	private Ddl ddl;
	
	public SyncTableFunc(Environment env, Ddl ddl) {
		this.env = env;
		this.ddl = ddl;
	}
	
	/**
	 * 테이블 싱크 
	 */
	public void sync() {
		// 1. dbist.ddl.enable 설정을 application properties 파일에서 체크하여 false이면 리턴
		if(!ValueUtil.toBoolean(env.getProperty("dbist.ddl.enable", "false"))) {
			return;
		}
		
		// 2. Base Package로 부터 @Table 구현체 Entity Scan
		Table domainTable = this.ddl.isTableExist("domains") ? this.ddl.getTable(Domain.class) : null;
		String basePackagePath = env.getProperty("dbist.base.entity.path");
		
		if (ValueUtils.isEmpty(basePackagePath)) {
			return;
		}

		// 3. 스캔한 Entity 클래스들에 대한 동기화 진행 
		List<Class<?>> classList = this.ddl.scanEntity(basePackagePath);
		for (Class<?> entityClass : classList) {
			Annotation tableAnn = AnnotationUtils.findAnnotation(entityClass, xyz.elidom.dbist.annotation.Table.class);
			Map<String, Object> tableInfo = (tableAnn == null) ? null : AnnotationUtils.getAnnotationAttributes(tableAnn);		
			
			// 1. Table Annotation이 없거나 ignoreDdl로 설정되어 있다면 스킵 
			if(tableAnn == null || tableInfo.get("name") == null || (boolean)tableInfo.get("ignoreDdl") == true) {
				continue;
			} 

			// 2. sourceType 확인 (self 인 것만 ddl 처리) 
			String sourceType = (String)tableInfo.get("sourceType");
			
			if(sourceType == null) {
				sourceType = (String)tableInfo.get("dataSourceType");
			}

			// 기본 데이터 소스외의 엔티티는 자동으로 테이블 형상 관리 지원하지 않음.
			if(ValueUtil.isEqualIgnoreCase(sourceType, DataSourceType.SELF) == false) {
				continue;
			}
			
			// 3. 테이블 명 추출
			String tableName = (String)tableInfo.get("name");
			
			// 4. 테이블 존재 체크
			boolean tableExist = this.ddl.isTableExist(tableName);
			
			// 5. 테이블이 존재하지 않으면 테이블 생성
			if(!tableExist) {
				this.ddl.createTable(entityClass);
				
			// 6. 테이블이 존재하면 컬럼 체크
			} else if(tableExist && domainTable != null) {
				DdlUtil.syncEntityColumns(domainTable.getDomain(), entityClass);
			}
		}
	}
}
