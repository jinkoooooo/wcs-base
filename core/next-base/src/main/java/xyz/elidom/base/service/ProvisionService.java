/* Copyright © HatioLab Inc. All rights reserved. */
package xyz.elidom.base.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.entity.Menu;
import xyz.elidom.base.entity.MenuButton;
import xyz.elidom.base.entity.MenuColumn;
import xyz.elidom.base.entity.MenuDetail;
import xyz.elidom.base.entity.MenuDetailButton;
import xyz.elidom.base.entity.MenuDetailColumn;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.metadata.Column;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sec.entity.Permission;
import xyz.elidom.sec.entity.Role;
import xyz.elidom.sec.entity.UsersRole;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.DomainUser;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.dsl.groovy.QueryDsl;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.base.query.SysQueryStore;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.sys.util.SysValueUtil;


/**
 * Dmain Crate Service
 *
 * @author
 */
@Service
public class ProvisionService extends AbstractSysQueryService {

    /**
     * 쿼리 매니저
     */
    @Autowired
    private IQueryManager queryManager;

    @Autowired
    private SysQueryStore sysQueryStore;
    
    /**
     * 도메인 생성
     *
     * @param
     * @param
     * @return
     */
    @Transactional
    public Long createDomain() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("systemFlag", true);
        condition.setMaxResultSize(1);
        Domain sourceDomain = queryManager.selectByCondition(Domain.class, condition);

        String maxDomainIdSql = sysQueryStore.getGetMaxDomainId();
        Long targetDomainId = this.queryManager.selectBySql(maxDomainIdSql, null, Long.class);

        Map<String, Object> params = SysValueUtil.newMap("sourceDomainId,targetDomainId,creatorId", sourceDomain.getId(), targetDomainId, "system");

        // 2. 테이블 복사
        // 설정
        copyData(params, "xyz.elidom.sys.entity.Setting");
        // Permit URL
        copyData(params, "xyz.elidom.sec.entity.PermitUrl");
        // 역할
        copyData(params, "xyz.elidom.sec.entity.Role");
        // 메시지
        copyData(params, "xyz.elidom.msg.entity.Message");
        // 용어
        copyData(params, "xyz.elidom.msg.entity.Terminology");
        // 스토리지
        copyData(params, "xyz.elidom.core.entity.Storage");
        // 커스텀 서비스
        copyData(params, "xyz.elidom.dev.entity.DiyService");
        // 커스텀 템플릿
        copyData(params, "xyz.elidom.dev.entity.DiyTemplate");
        // 공통 코드
        copyMasterDetails(params, "xyz.elidom.core.entity.Code", "xyz.elidom.core.entity.CodeDetail");
        // 엔티티
        copyMasterDetails(params, "xyz.elidom.base.entity.Resource", "xyz.elidom.base.entity.ResourceColumn");
        // 메뉴
        copyMasterDetails(params, "xyz.elidom.base.entity.Menu", "xyz.elidom.base.entity.MenuButton",
                "xyz.elidom.base.entity.MenuColumn", "xyz.elidom.base.entity.MenuParam");

        // 메뉴 Detail 복사
        copyMenuDetail(params);

        //설정 데이터를 타겟 도메인으로 복사
        copyConfigSet(params);

        sourceDomain.setId(targetDomainId);
        sourceDomain.setName("NewDomain" + targetDomainId);
        sourceDomain.setDescription("NewDomain" + targetDomainId);
        sourceDomain.setBrandName("NewDomain" + targetDomainId);
        sourceDomain.setMwSiteCd("NewDomain" + targetDomainId);
        sourceDomain.setDevSubdomain("NewDomain" + targetDomainId);
        sourceDomain.setSystemFlag(false);

        this.queryManager.insert(sourceDomain);
        return targetDomainId;
    }

    public void copyData(Map<String, Object> params, String entityClassName) {
        // 복사 스크립트
        String qryTemplate = "insert into <tableName> (<sourceColumns>) select <targetColumns> from <tableName> where domain_id = :sourceDomainId";
        QueryDsl queryDsl = new QueryDsl();
        queryDsl.init(entityClassName);

        Table table = queryDsl.getTable();
        List<Column> columns = table.getColumnList();
        List<String> sourceFields = new ArrayList<String>(columns.size());
        List<String> targetFields = new ArrayList<String>(columns.size());
        for (Column col : columns) {
            String colName = col.getName();

            if (colName == "domain" || colName == "creator" || colName == "updater" || colName == "parent"
                    || colName == "master" || colName == "menu" || colName == "entity") {
                continue;
            }

            sourceFields.add(colName);

            if (SysValueUtil.isEqual(colName, "domain_id")) {
                targetFields.add(":targetDomainId");

            } else if (SysValueUtil.isEqual(colName, "id")) {
                targetFields.add("uuid_generate_v4()");

            } else if (SysValueUtil.isEqual(colName, "creator_id") || SysValueUtil.isEqual(colName, "updater_id")) {
                targetFields.add(":creatorId");

            } else {
                targetFields.add(colName);
            }
        }

        // 테이블 명, 컬럼 명 파라미터 설정
        qryTemplate = qryTemplate.replaceAll("<tableName>", table.getName())
                .replaceAll("<sourceColumns>", SysValueUtil.listToString(sourceFields))
                .replaceAll("<targetColumns>", SysValueUtil.listToString(targetFields));
        this.queryManager.executeBySql(qryTemplate, params);
    }

    /**
     * 테이블(마스터 - 디테일)에 소스 도메인 데이터를 타겟 도메인으로 복사
     *
     * @param params                 파라미터
     * @param masterEntityClassName  엔티티 클래스
     * @param detailEntityClassNames 엔티티 클래스
     */
    @SuppressWarnings("unchecked")
	private void copyMasterDetails(Map<String, Object> params, String masterEntityClassName,
                                   String... detailEntityClassNames) {
        // 복사 스크립트
        Class<?> masterClass = ClassUtil.forName(masterEntityClassName);
        Table table = this.queryManager.getTable(masterClass);
        String script = "select * from <tableName> where domain_id = :sourceDomainId";
        script = script.replaceAll("<tableName>", table.getName());

        if ("xyz.elidom.base.entity.Menu" == masterEntityClassName) {
            //script = "select * from ${table.getName()} where domain_id = :sourceDomainId and category = "OPERATO" and (parent_id is null or parent_id in("1fa3294b-ffc5-4e62-baad-b3dc27aa9593", "34fd5ebd-8e8a-4664-ac69-05b362ab4985", "7202990d-9090-4421-8de1-fc0cbc2239dc"))";
            //   script = "select * from ${table.getName()} where domain_id = :sourceDomainId and category = "OPERATO" and (parent_id is null or parent_id in (select id from menus where domain_id = :sourceDomainId and parent_id is null and (hidden_flag is null or hidden_flag = false)))";
        }
        @SuppressWarnings("rawtypes")
		List<Map> masterDataList = this.queryManager.selectListBySql(script, params, Map.class, 0, 0);

        // 엔티티로 부터 필드명 추출
        List<Column> columns = table.getColumnList();
        List<String> sourceFields = new ArrayList<String>(columns.size());
        List<String> targetFields = new ArrayList<String>(columns.size());

        for (Column col : columns) {
            String colName = col.getName();

            if (SysValueUtil.isEqual(colName, "domain") || SysValueUtil.isEqual(colName, "creator") || SysValueUtil.isEqual(
                    colName, "updater") || SysValueUtil.isEqual(colName, "master") || SysValueUtil.isEqual(colName, "entity")
                    || SysValueUtil.isEqual(colName, "parent") || SysValueUtil.isEqual(colName, "menu")) {
                continue;
            }

            sourceFields.add(colName);

            if (SysValueUtil.isEqual(colName, "domain_id")) {
                targetFields.add(":targetDomainId");

            } else if (SysValueUtil.isEqual(colName, "id")) {
                targetFields.add(":idValue");

            } else if (SysValueUtil.isEqual(colName, "creator_id") || SysValueUtil.isEqual(colName, "updater_id")) {
                targetFields.add(":creatorId");

            } else {
                targetFields.add(":" + colName);
            }
        }

        // 테이블 명, 컬럼 명 파라미터 설정
        script = "insert into <tableName> (<sourceColumns>) values (<targetColumns>)";
        script = script.replaceAll("<tableName>", table.getName())
                .replaceAll("<sourceColumns>", SysValueUtil.listToString(sourceFields))
                .replaceAll("<targetColumns>", SysValueUtil.listToString(targetFields));

        // 소스 도메인 마스터 ID와 타겟 도메인 마스터 ID 매핑
        Map<String, String> idMappings = new HashMap<String, String>();

        // 마스터 데이터 Insert 쿼리 실행
        for (Map<String,Object> masterData : masterDataList) {
            String sourceMasterId = SysValueUtil.toString(masterData.get("id"));
            String targetMasterId = UUID.randomUUID().toString();
            idMappings.put(sourceMasterId, targetMasterId);
            masterData.put("idValue", targetMasterId);
            masterData.put("targetDomainId", params.get("targetDomainId"));
            masterData.put("creatorId", params.get("creatorId"));
            this.queryManager.executeBySql(script, masterData);
        }

        // 메뉴인 경우는 parent_id를 업데이트 해야 한다.
        if (SysValueUtil.isEqual(masterEntityClassName, "xyz.elidom.base.entity.Menu")) {
            String qry = "update menus set parent_id = :targetParentId where domain_id = :targetDomainId and parent_id = :sourceParentId";

            Iterator<String> sourceIdIter = idMappings.keySet().iterator();
            while (sourceIdIter.hasNext()) {
                String sourceId = sourceIdIter.next();
                String targetId = idMappings.get(sourceId);
                Map<String, Object> mParams = SysValueUtil.newMap("targetDomainId,sourceParentId,targetParentId",
                        params.get("targetDomainId"), sourceId, targetId);
                this.queryManager.executeBySql(qry, mParams);
            }
        }

        // 리소스인 경우는 master_id를 업데이트 해야 한다.
        if (SysValueUtil.isEqual(masterEntityClassName, "xyz.elidom.base.entity.Resource")) {
            String qry = "update entities set master_id = :targetParentId where domain_id = :targetDomainId and master_id = :sourceParentId";

            Iterator<String> sourceIdIter = idMappings.keySet().iterator();
            while (sourceIdIter.hasNext()) {
                String sourceId = sourceIdIter.next();
                String targetId = idMappings.get(sourceId);
                Map<String, Object> mParams = SysValueUtil.newMap("targetDomainId,sourceParentId,targetParentId",
                        params.get("targetDomainId"), sourceId, targetId);
                this.queryManager.executeBySql(qry, mParams);
            }
        }

        // 리소스인 경우는 master_id를 업데이트해야 한다.
        if (SysValueUtil.isEqual(masterEntityClassName, "xyz.elidom.base.entity.MenuDetail")) {
            String qry = "update menu_details set master_id = :targetParentId where domain_id = :targetDomainId and master_id = :sourceParentId";

            Iterator<String> sourceIdIter = idMappings.keySet().iterator();
            while (sourceIdIter.hasNext()) {
                String sourceId = sourceIdIter.next();
                String targetId = idMappings.get(sourceId);
                Map<String, Object> mParams = SysValueUtil.newMap("targetDomainId,sourceParentId,targetParentId",
                        params.get("targetDomainId"), sourceId, targetId);
                this.queryManager.executeBySql(qry, mParams);
            }
        }

        // 디테일 데이터 복사
        for (String detailEntityClassName : detailEntityClassNames) {
            this.copyDetails(params, detailEntityClassName, idMappings);
        }

    }


    /**
     * 메뉴 Detail를 타겟 도메인으로 복사
     *
     * @param params 파라미터
     */
    private void copyMenuDetail(Map<String, Object> params) {
        // 복사 스크립트
        String scriptDetail = sysQueryStore.getQueryMenuDetail();

        List<MenuDetail> menuDetailList = this.queryManager.selectListBySql(scriptDetail, params, MenuDetail.class, 0, 0);
        this.queryManager.insertBatch(menuDetailList);

        String scriptMenuDetailColumn = sysQueryStore.getQueryMenuDetailColumn();
        List<MenuDetailColumn> menuDetailColumnlList = this.queryManager.selectListBySql(scriptMenuDetailColumn, params, MenuDetailColumn.class, 0, 0);
        this.queryManager.insertBatch(menuDetailColumnlList);

        String scriptMenuDetailButton = sysQueryStore.getQueryMenuDetailButton();
        List<MenuDetailButton> menuDetailButtonlList = this.queryManager.selectListBySql(scriptMenuDetailButton, params, MenuDetailButton.class, 0, 0);
        this.queryManager.insertBatch(menuDetailButtonlList);
    }

    /**
     * 설정 데이터를 타겟 도메인으로 복사
     *
     * @param params 파라미터
     */
    private void copyConfigSet(Map<String, Object> params) {
        // 복사 스크립트
        String scriptIndConfigSet = sysQueryStore.getInsertIndConfigSet();
        this.queryManager.executeBySql(scriptIndConfigSet, params);

        String scriptJobConfigSet = sysQueryStore.getInsertJobConfigSet();
        this.queryManager.executeBySql(scriptJobConfigSet, params);

        String scriptDeviceProfiles = sysQueryStore.getInsertDeviceProfilesSet();
        this.queryManager.executeBySql(scriptDeviceProfiles, params);

        String scriptIndConfigs = sysQueryStore.getInsertIndConfigs();
        this.queryManager.executeBySql(scriptIndConfigs, params);

        String scriptJobConfigs = sysQueryStore.getInsertJobConfigs();
        this.queryManager.executeBySql(scriptJobConfigs, params);

        String scriptDeviceConfs = sysQueryStore.getInsertDeviceConfigs();
        this.queryManager.executeBySql(scriptDeviceConfs, params);


    }

    /**
     * 디테일 복사 후 부모 ID 업데이트 ...
     *
     * @param params          파라미터
     * @param entityClassName 엔티티 클래스
     * @param idMappings      소스 도메인의 마스터 ID와 타겟 도메인의 마스터 ID 매핑 값
     */
    private void copyDetails(Map<String, Object> params, String entityClassName, Map<String, String> idMappings) {
        // 테이블 추출
        Class<?> entityClass = ClassUtil.forName(entityClassName);
        Table table = this.queryManager.getTable(entityClass);

        // 디테일 데이터 복사
        this.copyData(params, entityClassName);

        // 디테일 데이터의 부모 ID를 업데이트
        Iterator<String> sourceIdIter = idMappings.keySet().iterator();
        while (sourceIdIter.hasNext()) {
            String sourceId = sourceIdIter.next();
            String targetId = idMappings.get(sourceId);

            Map<String, Object> detailParams = SysValueUtil.newMap("targetDomainId,sourceParentId,targetParentId",
                    params.get("targetDomainId"), sourceId, targetId);
            String sql = "";

            if (SysValueUtil.isEqual("xyz.elidom.base.entity.ResourceColumn", entityClassName)) {
                sql = "update <tableName> set entity_id = :targetParentId where domain_id = :targetDomainId and entity_id = :sourceParentId";
                sql = sql.replaceAll("<tableName>", table.getName());
            } else if (SysValueUtil.isEqual("xyz.elidom.base.entity.MenuButton", entityClassName) || SysValueUtil.isEqual(
                    "xyz.elidom.base.entity.MenuColumn", entityClassName) || SysValueUtil.isEqual(
                    "xyz.elidom.base.entity.MenuParam", entityClassName)) {
                sql = "update <tableName> set menu_id = :targetParentId where domain_id = :targetDomainId and menu_id = :sourceParentId";
                sql = sql.replaceAll("<tableName>", table.getName());
            } else if (SysValueUtil.isEqual("xyz.elidom.base.entity.MenuDetailColumn", entityClassName)
                    || SysValueUtil.isEqual("xyz.elidom.base.entity.MenuDetailButton", entityClassName)) {
                sql = "update <tableName> set menu_detail_id = :targetParentId where domain_id = :targetDomainId and menu_detail_id = :sourceParentId";
                sql = sql.replaceAll("<tableName>", table.getName());
            } else {
                sql = "update <tableName> set parent_id = :targetParentId where domain_id = :targetDomainId and parent_id = :sourceParentId";
                sql = sql.replaceAll("<tableName>", table.getName());
            }

            this.queryManager.executeBySql(sql, detailParams);
        }
    }


    /**
     * ID 생성
     */
    @SuppressWarnings("unused")
	private String generateId() {
        String sql = "select uuid_generate_v4() from dual ";
        Map<String, Object> qParams = new HashMap<String, Object>();
        return this.queryManager.selectBySql(sql, qParams, String.class);
    }

    /**
     * provisioning start
     * @param provision
     * @return
     */

    @SuppressWarnings("unchecked")
	@Transactional
    public Object provisioning(Map<String, Object> provision, String method) throws UnsupportedEncodingException {
        Map<String, Object> userInfo = (Map<String, Object>) provision.get("user");
        Map<String, Object> domainInfo = (Map<String, Object>) provision.get("domain");
        Map<String, Object> serviceList = (Map<String, Object>) provision.get("service");
        List<String> service = (List<String>) serviceList.get("details");
        service.add("com");

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("systemFlag", true);
        condition.setMaxResultSize(1);
        Domain sourceDomain = queryManager.selectByCondition(Domain.class, condition);

        sourceDomain.setProvisionId(domainInfo.get("id").toString());
        if(method.equalsIgnoreCase(ProvisionServiceContants.CREATE)) {
            Domain targetDomain = this.provisionDomain(sourceDomain, domainInfo, service);
            User user = this.provisionUser(sourceDomain.getId(), targetDomain, userInfo);	
            this.provisionPermissions(sourceDomain.getId(), targetDomain);
            return user;
        }else if(method.equalsIgnoreCase(ProvisionServiceContants.UPDATE)){
        	Long sourceDomainId = sourceDomain.getId();
            condition = OrmUtil.newConditionForExecution();
            condition.addFilter("provisionId", domainInfo.get("id").toString());
            condition.setMaxResultSize(1);
            Domain targetDomain = queryManager.selectByCondition(Domain.class, condition);
            
            if(SysValueUtil.isEmpty(targetDomain)) {
                targetDomain = this.provisionDomain(sourceDomain, domainInfo, service);
                User user = this.provisionUser(sourceDomain.getId(), targetDomain, userInfo);	
                this.provisionPermissions(sourceDomain.getId(), targetDomain);
                return user;            	
            }else {
            	this.provisionMenu(sourceDomainId, targetDomain, service);
            	this.provisionPermissions(sourceDomain.getId(), targetDomain);
            }
        }
        return this.getUser(userInfo);
    }

    private Domain provisionDomain(Domain sourceDomain, Map<String, Object> domainInfo, List<String> service) {

        String maxDomainIdSql = "select max(id)+1 from domains";
        Long targetDomainId = queryManager.selectBySql(maxDomainIdSql, null, Long.class);
        Long sourceDomainId = sourceDomain.getId();
        Map<String, Object> params = SysValueUtil.newMap("sourceDomainId,targetDomainId,creatorId", sourceDomain.getId(),
                targetDomainId, "system");

        // 2. 테이블 복사
        // 설정
        copyData(params, "xyz.elidom.sys.entity.Setting");
        // Permit URL
        copyData(params, "xyz.elidom.sec.entity.PermitUrl");
        // 역할
        copyData(params, "xyz.elidom.sec.entity.Role");
        // 메시지
        copyData(params, "xyz.elidom.msg.entity.Message");
        // 용어
        copyData(params, "xyz.elidom.msg.entity.Terminology");
        // 스토리지
        copyData(params, "xyz.elidom.core.entity.Storage");
        // 커스텀 서비스
        copyData(params, "xyz.elidom.dev.entity.DiyService");
        // 커스텀 템플릿
        copyData(params, "xyz.elidom.dev.entity.DiyTemplate");
        // 스케쥴은 sysdomain에서 만 관리한다. 
//        copyData(params, "xyz.elidom.job.entity.Job");7
        // 공통 코드
        copyMasterDetails(params, "xyz.elidom.core.entity.Code", "xyz.elidom.core.entity.CodeDetail");
        // 엔티티
        copyMasterDetails(params, "xyz.elidom.base.entity.Resource", "xyz.elidom.base.entity.ResourceColumn");

        //설정 데이터를 타겟 도메인으로 복사
        copyConfigSet(params);

        //도메인 생성
        Domain targetDomain = new Domain();
        targetDomain.setId(targetDomainId);
        targetDomain.setName(domainInfo.get("name").toString());
        targetDomain.setDescription(domainInfo.get("name").toString());
        targetDomain.setBrandName(domainInfo.get("name").toString());
        targetDomain.setMwSiteCd(domainInfo.get("name").toString());
        targetDomain.setSubdomain(domainInfo.get("name").toString());
        targetDomain.setDevSubdomain(domainInfo.get("name").toString());
        targetDomain.setSystemFlag(false);
        targetDomain.setProvisionId(domainInfo.get("id").toString());

        this.queryManager.insert(targetDomain);

        // 메뉴 생성
		this.provisionMenu(sourceDomainId, targetDomain, service);
        return targetDomain;
    }

    private User provisionUser(Long sourceDomainId, Domain targetDomain, Map<String, Object> userInfo)
            throws UnsupportedEncodingException {
        String password = userInfo.get("pw").toString();


        String encrypted = new String(Base64.decodeBase64(password), "UTF-8");
        encrypted = SecurityUtil.encodePassword(encrypted);
                // 동일 유저 체크
        String userId = userInfo.get("id").toString();
    	User user = this.getUser(userInfo);
        if (SysValueUtil.isEmpty(user)) {
            user = new User();
            user.setDomainId(targetDomain.getId());
            user.setId(userId);
            user.setLogin(userId);
            user.setName(userId);
            user.setEmail("");
            user.setEncryptedPassword(encrypted);
            user.setLocale("ko-KR");

            queryManager.insert(user);
        } else {
            user = new User();
            user.setDomainId(targetDomain.getId());
            user.setId(userId);
            user.setLogin(userId);
            user.setName(userId);
            user.setEmail("");
            user.setEncryptedPassword(encrypted);
            user.setLocale("ko-KR");
        }

        Query condition = OrmUtil.newConditionForExecution(targetDomain.getId());
        condition.setSelect(SysValueUtil.toList("id"));
        condition.addFilter("name", "Manager");
        condition.setMaxResultSize(1);
        Role role = queryManager.selectByCondition(Role.class, condition);
        String rolesId = role.getId();

        UsersRole usersRole = new UsersRole();

        usersRole.setId(UUID.randomUUID().toString());
        usersRole.setUserId(userId);
        usersRole.setRoleId(rolesId);
        usersRole.setDomainId(targetDomain.getId());
        queryManager.insert(usersRole);

        //강제로 업데이트 처리
        String userUpdateSql = sysQueryStore.getUserActiveFlagUpdateQuery();
        Map<String, Object> updateParam = SysValueUtil.newMap("userId,domainId", userId, targetDomain.getId());
        queryManager.executeBySql(userUpdateSql, updateParam);

        DomainUser domainUser = new DomainUser();
        domainUser.setId(UUID.randomUUID().toString());
        domainUser.setUserId(userId);
        domainUser.setDomainId(targetDomain.getId());
        queryManager.insert(domainUser);
        return user;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Menu provisionMenu(Long sourceDomainId, Domain targetDomain, List<String> menuInfo) {
        Menu menu = new Menu();
        Map<String, Object> clearParams = SysValueUtil.newMap("domainId", targetDomain.getId());
        this.queryManager.deleteList(MenuDetailButton.class, clearParams);
        this.queryManager.deleteList(MenuDetailColumn.class, clearParams);
        this.queryManager.deleteList(MenuDetail.class, clearParams);
        this.queryManager.deleteList(MenuButton.class, clearParams);
        this.queryManager.deleteList(MenuColumn.class, clearParams);
        this.queryManager.deleteList(Menu.class, clearParams);
        
        Map<String, Object> comParams = SysValueUtil.newMap("targetDomainId,sourceDomainId,service", targetDomain.getId(),
        		sourceDomainId, menuInfo);
        String comMenuInsert = sysQueryStore.getInsertMenuQuery();
        this.queryManager.executeBySql(comMenuInsert, comParams);

        String menuParentIdUpdate = sysQueryStore.getUpdateMenuParentIdQuery();

        //UPDATE
        String parentListSql = sysQueryStore.getParentMenuListQuery();
        List<HashMap> parentList = queryManager.selectListBySql(parentListSql, comParams, HashMap.class, 0, 0);
        
        for (Map<String, Object> parent : parentList) {
            queryManager.executeBySql(menuParentIdUpdate,
                    SysValueUtil.newMap("toid,fromid,domainId", parent.get("toid"), parent.get("fromid"),
                            targetDomain.getId()));
        }
        
        //menu colomun inserting
        String menuColumnsInsert = sysQueryStore.getMenuColumnsInsertQuery();        
        this.queryManager.executeBySql(menuColumnsInsert, comParams);

        //menu button inserting
        String scriptMenuButtons = sysQueryStore.getMenuButtonsInsertQuery();
        this.queryManager.executeBySql(scriptMenuButtons, comParams);

        //menu detail inserting
        String scriptMenuDetails = sysQueryStore.getMenuDetailsInsertQuery();
        this.queryManager.executeBySql(scriptMenuDetails, comParams);
        
        //menu detail column inserting
        String scriptMenuDetailColumns = sysQueryStore.getMenuDetailColumnsInsertQuery();
        this.queryManager.executeBySql(scriptMenuDetailColumns, comParams);

        //menu detail button inserting
        String scriptMenuDetailButtons = sysQueryStore.getMenuDetailButtonsInsertQuery();
        this.queryManager.executeBySql(scriptMenuDetailButtons, comParams);

        return menu;
    }

    private void provisionPermissions(Long sourceDomainId, Domain targetDomain) {
        Map<String, Object> clearParams = SysValueUtil.newMap("domainId", targetDomain.getId());
        this.queryManager.deleteList(Permission.class, clearParams);
        
        
        Map<String, Object> comParame = SysValueUtil.newMap("targetDomainId,sourceDomainId", targetDomain.getId(),
                sourceDomainId);
        //permission inserting
        String scriptPermissions = sysQueryStore.getPermissionsInsertQuery();
        this.queryManager.executeBySql(scriptPermissions, comParame);
    }
    
    private User getUser(Map<String, Object> userInfo) {
        String userId = userInfo.get("id").toString();
        Query userCondition = OrmUtil.newConditionForExecution();
        userCondition.addFilter("id", userId);
        userCondition.setMaxResultSize(1);
        User user = queryManager.selectByCondition(User.class, userCondition);
        return user;
    }
    /**
     * provisioning end
     * @param provision
     * @return
     */
}
