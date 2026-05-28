package operato.logis.asrs.query.location;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.entity.*;
import org.springframework.stereotype.Service;

import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 영역 / 로케이션 / 로케이션 프로파일 조회 전용 서비스.
 *
 * <p>
 * 신규 업무 로직은 code 기반 조회를 사용한다.
 * </p>
 *
 * <ul>
 *   <li>영역 조회: area_code</li>
 *   <li>로케이션 조회: area_code + location_code</li>
 *   <li>로케이션 프로파일 조회: area_code + profile_code</li>
 * </ul>
 *
 * <p>
 * id 기반 메서드는 하위호환 목적으로만 유지하며, 신규 코드에서는 사용하지 않는다.
 * </p>
 */
@Service
@Slf4j
public class LocationQueryService extends AbstractQueryService {

    /* =========================================================
     * 신규 표준: code 기반 조회
     * ========================================================= */

    /**
     * area_code 로 활성 영역을 조회한다.
     *
     * <p>
     * 현재 테이블 제약은 center_id + area_code 유니크이므로,
     * area_code 만으로 중복될 수 있다. 중복 시 예외를 발생시켜
     * 향후 center_code 기반 확장 필요성을 명확히 드러낸다.
     * </p>
     *
     * @param areaCode 영역 코드
     * @return TbAcStorageArea
     */
    public TbAcStorageArea findAreaByCode(String areaCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "areaCode is empty."
            );
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_code", areaCode);
        condition.addFilter("active_yn", "Y");

        List<TbAcStorageArea> list = this.queryManager.selectList(TbAcStorageArea.class, condition);

        if (list == null || list.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Storage area not found. areaCode=" + areaCode
            );
        }

        if (list.size() > 1) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.DUPLICATE_DATA,
                    "Duplicated areaCode exists. Please use center-specific search. areaCode=" + areaCode
            );
        }

        return list.get(0);
    }

    /**
     * area_code + profile_code 로 활성 로케이션 프로파일을 조회한다.
     *
     * @param areaCode 영역 코드
     * @param profileCode 로케이션 프로파일 코드
     * @return TbAcLocationProfile
     */
    public TbAcLocationProfile findLocationProfileByCode(String areaCode, String profileCode) {
        if (ValueUtil.isEmpty(profileCode)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "profileCode is empty."
            );
        }

        TbAcStorageArea area = findAreaByCode(areaCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", area.getId());
        condition.addFilter("profile_code", profileCode);
        condition.addFilter("active_yn", "Y");

        TbAcLocationProfile profile = this.queryManager.select(TbAcLocationProfile.class, condition);
        if (profile == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Location profile not found. areaCode=" + areaCode + ", profileCode=" + profileCode
            );
        }

        return profile;
    }

    /**
     * area_code + location_code 로 활성 로케이션을 조회한다.
     *
     * @param areaCode 영역 코드
     * @param locationCode 로케이션 코드
     * @return TbAcLocation
     */
    public TbAcLocation findLocationByCode(String areaCode, String locationCode) {
        if (ValueUtil.isEmpty(locationCode)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "locationCode is empty."
            );
        }

        TbAcStorageArea area = findAreaByCode(areaCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", area.getId());
        condition.addFilter("location_code", locationCode);
        condition.addFilter("active_yn", "Y");

        TbAcLocation location = this.queryManager.select(TbAcLocation.class, condition);
        if (location == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Location not found. areaCode=" + areaCode + ", locationCode=" + locationCode
            );
        }

        return location;
    }

    /**
     * 특정 영역의 전체 활성 로케이션 조회.
     *
     * @param areaCode 영역 코드
     * @return 활성 로케이션 목록
     */
    public List<TbAcLocation> findLocationsByAreaCode(String areaCode) {
        TbAcStorageArea area = findAreaByCode(areaCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", area.getId());
        condition.addFilter("active_yn", "Y");

        condition.addOrder("aisle_no", true);
        condition.addOrder("side_code", true);
        condition.addOrder("bay_no", true);
        condition.addOrder("level_no", true);
        condition.addOrder("depth_no", true);

        return this.queryManager.selectList(TbAcLocation.class, condition);
    }

    /**
     * 특정 영역의 전체 로케이션 조회.
     *
     * <p>
     * unique 제약 충돌 방지를 위해 active_yn 여부와 관계없이 전체 조회가 필요한 경우 사용한다.
     * 로케이션 생성 시 기존 비활성 로케이션도 중복 판정 대상이어야 하므로 별도 메서드로 분리한다.
     * </p>
     *
     * @param areaCode 영역 코드
     * @return 전체 로케이션 목록
     */
    public List<TbAcLocation> findAllLocationsByAreaCode(String areaCode) {
        TbAcStorageArea area = findAreaByCode(areaCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", area.getId());

        condition.addOrder("aisle_no", true);
        condition.addOrder("side_code", true);
        condition.addOrder("bay_no", true);
        condition.addOrder("level_no", true);
        condition.addOrder("depth_no", true);

        return this.queryManager.selectList(TbAcLocation.class, condition);
    }

    /**
     * 사용 가능한 로케이션 목록 조회.
     *
     * @param areaCode 영역 코드
     * @return 후보 로케이션 목록
     */
    public List<TbAcLocation> findAvailableLocationsByAreaCode(String areaCode) {
        TbAcStorageArea area = findAreaByCode(areaCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", area.getId());
        condition.addFilter("active_yn", "Y");
        condition.addFilter("usage_status_code", "ENABLED");
        condition.addFilter("inbound_allowed_yn", "Y");

        condition.addOrder("front_priority_yn", false);
        condition.addOrder("aisle_no", true);
        condition.addOrder("side_code", true);
        condition.addOrder("bay_no", true);
        condition.addOrder("level_no", true);
        condition.addOrder("depth_no", true);

        return this.queryManager.selectList(TbAcLocation.class, condition);
    }

    /**
     * 적치 후보 로케이션 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @return 적치 후보 목록
     */
    public List<TbAcLocation> findCandidateLocationsByCode(String areaCode, String itemCode) {
        return findAvailableLocationsByAreaCode(areaCode);
    }

    /**
     * 동일 좌표 로케이션 존재 여부 확인.
     *
     * @param areaCode 영역 코드
     * @param aisleNo aisle 번호
     * @param sideCode side 코드
     * @param bayNo bay 번호
     * @param levelNo level 번호
     * @param depthNo depth 번호
     * @return 존재 여부
     */
    public boolean existsLocationByCoordinate(String areaCode,
                                              Integer aisleNo,
                                              String sideCode,
                                              Integer bayNo,
                                              Integer levelNo,
                                              Integer depthNo) {

        TbAcStorageArea area = findAreaByCode(areaCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", area.getId());
        condition.addFilter("aisle_no", aisleNo);
        condition.addFilter("side_code", sideCode);
        condition.addFilter("bay_no", bayNo);
        condition.addFilter("level_no", levelNo);
        condition.addFilter("depth_no", depthNo);

        TbAcLocation location = this.queryManager.select(TbAcLocation.class, condition);
        return location != null;
    }

    /* =========================================================
     * 하위호환: id 기반 조회 (신규 사용 금지)
     * ========================================================= */

    /**
     * @deprecated 신규 업무 로직에서는 사용하지 말고 findAreaByCode 를 사용할 것.
     */
    @Deprecated
    public TbAcStorageArea findArea(String areaId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("id", areaId);
        condition.addFilter("active_yn", "Y");

        TbAcStorageArea area = this.queryManager.select(TbAcStorageArea.class, condition);
        if (area == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Storage area not found. areaId=" + areaId
            );
        }
        return area;
    }

    /**
     * @deprecated 신규 업무 로직에서는 사용하지 말고 findLocationProfileByCode 를 사용할 것.
     */
    @Deprecated
    public TbAcLocationProfile findLocationProfile(String profileId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("id", profileId);
        condition.addFilter("active_yn", "Y");

        TbAcLocationProfile profile = this.queryManager.select(TbAcLocationProfile.class, condition);
        if (profile == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Location profile not found. profileId=" + profileId
            );
        }
        return profile;
    }

    /**
     * @deprecated 신규 업무 로직에서는 사용하지 말고 findLocationByCode 를 사용할 것.
     */
    @Deprecated
    public TbAcLocation findLocation(String locationId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("id", locationId);
        condition.addFilter("active_yn", "Y");

        TbAcLocation location = this.queryManager.select(TbAcLocation.class, condition);
        if (location == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Location not found. locationId=" + locationId
            );
        }
        return location;
    }


    @Transactional
    public void saveStorageArea(TbAcStorageArea inputEntity) {
        try {
            if (inputEntity.getAreaType() == null) inputEntity.setAreaType("ASRS");
            if (inputEntity.getOperationProfileId() == null) inputEntity.setOperationProfileId("DEFAULT_OP");
            if (inputEntity.getActiveYn() == null) inputEntity.setActiveYn("Y");

            String safeCenterId = this.ensureCenterExists(inputEntity.getCenterId());
            inputEntity.setCenterId(safeCenterId);

            String safeProfileId = this.ensureOperationProfileExists(inputEntity.getOperationProfileId());
            inputEntity.setOperationProfileId(safeProfileId);

            Query query = new Query();
            query.addFilter("centerId", inputEntity.getCenterId());
            query.addFilter("areaCode", inputEntity.getAreaCode());

            TbAcStorageArea existingArea = this.queryManager.select(TbAcStorageArea.class, query);

            if (existingArea != null) {
                existingArea.setAreaName(inputEntity.getAreaName());
                existingArea.setAreaType(inputEntity.getAreaType());
                existingArea.setOperationProfileId(inputEntity.getOperationProfileId());
                existingArea.setDescription(inputEntity.getDescription());
                existingArea.setActiveYn(inputEntity.getActiveYn());

                this.queryManager.update(existingArea);
                log.info("Storage Area DB 업데이트 완료 (중복 코드 덮어쓰기): AreaCode={}", existingArea.getAreaCode());
            } else {
                this.queryManager.insert(inputEntity);
                log.info("Storage Area DB 신규 저장 완료: AreaCode={}", inputEntity.getAreaCode());
            }

        } catch (Exception e) {
            log.error("Storage Area 데이터를 DB에 저장/수정하는 중 오류 발생", e);
            throw new RuntimeException("구역(Area) 마스터 저장 실패: " + e.getMessage());
        }
    }

    private String ensureCenterExists(String centerIdOrCode) {
        if (ValueUtil.isEmpty(centerIdOrCode)) {
            throw new RuntimeException("요청 데이터에 Center ID/Code가 없습니다.");
        }

        String targetCode = centerIdOrCode.length() > 30 ? centerIdOrCode.substring(0, 30) : centerIdOrCode;

        Query query = new Query();
        query.addFilter("centerCode", targetCode);
        TbAcCenter existingCenter = this.queryManager.select(TbAcCenter.class, query);

        if (existingCenter != null) {
            return existingCenter.getId();
        }

        log.warn("부모 테이블(tb_ac_center)에 Center Code [{}]가 없어 자동 생성을 시도합니다.", targetCode);
        TbAcCenter newCenter = new TbAcCenter();

        newCenter.setCenterCode(targetCode);
        newCenter.setCenterName(centerIdOrCode + " 자동생성 센터");
        newCenter.setCenterType("ASRS");
        newCenter.setTimezone("Asia/Seoul");
        newCenter.setActiveYn("Y");

        this.queryManager.insert(newCenter);
        log.info("Center 자동 생성 완료: Code [{}], 최종 발급된 UUID [{}]", targetCode, newCenter.getId());

        return newCenter.getId();
    }

    private String ensureOperationProfileExists(String profileIdOrCode) {
        if (ValueUtil.isEmpty(profileIdOrCode)) {
            throw new RuntimeException("요청 데이터에 Operation Profile ID/Code가 없습니다.");
        }

        String targetCode = profileIdOrCode.length() > 30 ? profileIdOrCode.substring(0, 30) : profileIdOrCode;

        Query query = new Query();
        query.addFilter("profileCode", targetCode);
        TbAcOperationProfile existingProfile = this.queryManager.select(TbAcOperationProfile.class, query);

        if (existingProfile != null) {
            return existingProfile.getId();
        }

        log.warn("부모 테이블(tb_ac_operation_profile)에 Profile Code [{}]가 없어 자동 생성을 시도합니다.", targetCode);
        TbAcOperationProfile newProfile = new TbAcOperationProfile();

        newProfile.setProfileCode(targetCode);
        newProfile.setProfileName(profileIdOrCode + " 자동생성 프로파일");
        newProfile.setIndustryType("GENERAL");
        newProfile.setActiveYn("Y");

        this.queryManager.insert(newProfile);
        log.info("Operation Profile 자동 생성 완료: Code [{}], 최종 발급된 UUID [{}]", targetCode, newProfile.getId());

        return newProfile.getId();
    }

    @Transactional
    public void saveLocationProfileCustom(Map<String, Object> param) {
        String areaCode = (String) param.get("area_id");
        if (ValueUtil.isEmpty(areaCode)) {
            throw new RuntimeException("요청 데이터에 areaCode가 누락되었습니다.");
        }

        TbAcStorageArea area = this.findAreaByCode(areaCode);

        TbAcLocationProfile profile = new TbAcLocationProfile();
        profile.setAreaId(area.getId());

        profile.setProfileCode((String) param.get("profile_code"));
        profile.setProfileName((String) param.get("profile_name"));

        profile.setAisleStart(Integer.valueOf(param.get("aisle_start").toString()));
        profile.setAisleEnd(Integer.valueOf(param.get("aisle_end").toString()));
        profile.setBayStart(Integer.valueOf(param.get("bay_start").toString()));
        profile.setBayEnd(Integer.valueOf(param.get("bay_end").toString()));
        profile.setLevelStart(Integer.valueOf(param.get("level_start").toString()));
        profile.setLevelEnd(Integer.valueOf(param.get("level_end").toString()));
        profile.setDepthStart(Integer.valueOf(param.get("depth_start").toString()));
        profile.setDepthEnd(Integer.valueOf(param.get("depth_end").toString()));

        profile.setSideCodes((String) param.get("side_codes"));
        profile.setLocationType((String) param.get("location_type"));
        profile.setCodePattern((String) param.get("code_pattern"));
        profile.setMixedLoadYn((String) param.get("mixed_load_yn"));
        profile.setInboundAllowedYn((String) param.get("inbound_allowed_yn"));
        profile.setOutboundAllowedYn((String) param.get("outbound_allowed_yn"));
        profile.setActiveYn((String) param.get("active_yn"));

        // 3. 중복 체크 및 Insert/Update
        Query condition = new Query();
        condition.addFilter("areaId", profile.getAreaId());
        condition.addFilter("profileCode", profile.getProfileCode());

        TbAcLocationProfile existingProfile = this.queryManager.select(TbAcLocationProfile.class, condition);

        if (existingProfile != null) {
            profile.setId(existingProfile.getId());
            this.queryManager.update(profile);
        } else {
            this.queryManager.insert(profile);
        }
    }

    /**
     * AreaCode 조회
     *
     * @return TbAcStorageArea 목록
     */
    public List<TbAcStorageArea> findActiveAreaList() {
        String sql =
                "select " +
                        "    a.id, " +
                        "    a.center_id, " +
                        "    a.area_code, " +
                        "    a.area_name, " +
                        "    a.area_type, " +
                        "    a.operation_profile_id, " +
                        "    a.description, " +
                        "    a.active_yn, " +
                        "    a.domain_id, " +
                        "    a.creator_id, " +
                        "    a.updater_id, " +
                        "    a.created_at, " +
                        "    a.updated_at " +
                        "  from logis_asrs.tb_ac_storage_area a " +
                        " where a.domain_id = :domainId " +
                        "   and a.active_yn = 'Y' " +
                        " order by a.area_code asc ";

        java.util.Map<String, Object> param = ValueUtil.newMap("domainId", Domain.currentDomainId());

        return this.queryManager.selectListBySql(sql, param, TbAcStorageArea.class, 0, 0);
    }
}