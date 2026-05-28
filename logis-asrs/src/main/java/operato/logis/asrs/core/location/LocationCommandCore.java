package operato.logis.asrs.core.location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.dto.request.LocationBulkDeleteRequest;
import operato.logis.asrs.dto.request.LocationUpsertRequest;
import operato.logis.asrs.dto.response.LocationSaveResult;
import operato.logis.asrs.entity.TbAcLocation;
import operato.logis.asrs.enums.AcLocationGrade;
import operato.logis.asrs.enums.AcLocationSide;
import operato.logis.asrs.enums.AcLocationType;
import operato.logis.asrs.enums.AcLocationUsageStatus;
import operato.logis.asrs.enums.AcTxnType;
import operato.logis.asrs.query.location.LocationManageQueryService;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 로케이션 명령 Core.
 */
@Service
public class LocationCommandCore extends AbstractQueryService {

    private final LocationManageQueryService locationManageQueryService;

    public LocationCommandCore(LocationManageQueryService locationManageQueryService) {
        this.locationManageQueryService = locationManageQueryService;
    }

    // =========================================================
    // 기존 create / update 코드는 그대로 유지
    // =========================================================

    @Transactional
    public LocationSaveResult create(LocationUpsertRequest request) {
        validateRequest(request);

        TbAcLocation duplicate = locationManageQueryService.findEntityByAreaAndLocationCode(
                request.getAreaCode().trim(),
                request.getLocationCode().trim()
        );
        if (duplicate != null) {
            throw new IllegalArgumentException(
                    "이미 존재하는 location_code 입니다. areaCode=" + request.getAreaCode()
                            + ", locationCode=" + request.getLocationCode()
            );
        }

        String areaId = requireAreaId(request.getAreaCode());
        String itemCategoryId = resolveItemCategoryId(request);
        String accessPointId = resolveAccessPointId(request);

        TbAcLocation entity = new TbAcLocation();
        entity.setAreaId(areaId);
        entity.setLocationCode(request.getLocationCode().trim());
        entity.setAisleNo(request.getAisleNo());
        entity.setSideCode(normalizeSideCode(request.getSideCode()));
        entity.setBayNo(request.getBayNo());
        entity.setLevelNo(request.getLevelNo());
        entity.setDepthNo(request.getDepthNo());
        entity.setLocationType(normalizeLocationType(request.getLocationType()));
        entity.setUsageStatusCode(normalizeUsageStatus(request.getUsageStatusCode()));
        entity.setInboundAllowedYn(normalizeYn(request.getInboundAllowedYn(), "inboundAllowedYn"));
        entity.setOutboundAllowedYn(normalizeYn(request.getOutboundAllowedYn(), "outboundAllowedYn"));
        entity.setMixedLoadYn(normalizeYn(request.getMixedLoadYn(), "mixedLoadYn"));
        entity.setFrontPriorityYn(normalizeYn(request.getFrontPriorityYn(), "frontPriorityYn"));
        entity.setDedicatedItemCategoryId(itemCategoryId);
        entity.setMaxWeightG(request.getMaxWeightG());
        entity.setMaxVolumeMm3(request.getMaxVolumeMm3());
        entity.setSortSeq(request.getSortSeq() == null ? locationManageQueryService.nextSortSeq(areaId) : request.getSortSeq());
        entity.setActiveYn(normalizeYn(request.getActiveYn(), "activeYn"));
        entity.setLocationGrade(normalizeLocationGrade(request.getLocationGrade()));
        entity.setAccessScore(request.getAccessScore());
        entity.setPrimaryAccessPointId(accessPointId);

        this.queryManager.insert(entity);

        LocationSaveResult result = new LocationSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(request.getAreaCode().trim());
        result.setLocationCode(entity.getLocationCode());
        result.setAction("CREATED");
        result.setMessage("로케이션이 생성되었습니다.");
        return result;
    }

    @Transactional
    public LocationSaveResult update(
            String originalAreaCode,
            String originalLocationCode,
            LocationUpsertRequest request
    ) {
        validateRequest(request);

        TbAcLocation entity = locationManageQueryService.findEntityByAreaAndLocationCode(
                originalAreaCode,
                originalLocationCode
        );
        if (entity == null) {
            throw new IllegalArgumentException(
                    "로케이션을 찾을 수 없습니다. areaCode=" + originalAreaCode
                            + ", locationCode=" + originalLocationCode
            );
        }

        boolean keyChanged =
                !originalAreaCode.equalsIgnoreCase(request.getAreaCode().trim())
                        || !originalLocationCode.equalsIgnoreCase(request.getLocationCode().trim());

        if (keyChanged) {
            TbAcLocation duplicate = locationManageQueryService.findEntityByAreaAndLocationCode(
                    request.getAreaCode().trim(),
                    request.getLocationCode().trim()
            );
            if (duplicate != null) {
                throw new IllegalArgumentException(
                        "이미 존재하는 location_code 입니다. areaCode=" + request.getAreaCode()
                                + ", locationCode=" + request.getLocationCode()
                );
            }
        }

        String areaId = requireAreaId(request.getAreaCode());
        String itemCategoryId = resolveItemCategoryId(request);
        String accessPointId = resolveAccessPointId(request);

        entity.setAreaId(areaId);
        entity.setLocationCode(request.getLocationCode().trim());
        entity.setAisleNo(request.getAisleNo());
        entity.setSideCode(normalizeSideCode(request.getSideCode()));
        entity.setBayNo(request.getBayNo());
        entity.setLevelNo(request.getLevelNo());
        entity.setDepthNo(request.getDepthNo());
        entity.setLocationType(normalizeLocationType(request.getLocationType()));
        entity.setUsageStatusCode(normalizeUsageStatus(request.getUsageStatusCode()));
        entity.setInboundAllowedYn(normalizeYn(request.getInboundAllowedYn(), "inboundAllowedYn"));
        entity.setOutboundAllowedYn(normalizeYn(request.getOutboundAllowedYn(), "outboundAllowedYn"));
        entity.setMixedLoadYn(normalizeYn(request.getMixedLoadYn(), "mixedLoadYn"));
        entity.setFrontPriorityYn(normalizeYn(request.getFrontPriorityYn(), "frontPriorityYn"));
        entity.setDedicatedItemCategoryId(itemCategoryId);
        entity.setMaxWeightG(request.getMaxWeightG());
        entity.setMaxVolumeMm3(request.getMaxVolumeMm3());
        entity.setSortSeq(request.getSortSeq());
        entity.setActiveYn(normalizeYn(request.getActiveYn(), "activeYn"));
        entity.setLocationGrade(normalizeLocationGrade(request.getLocationGrade()));
        entity.setAccessScore(request.getAccessScore());
        entity.setPrimaryAccessPointId(accessPointId);

        this.queryManager.update(entity);

        LocationSaveResult result = new LocationSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(request.getAreaCode().trim());
        result.setLocationCode(entity.getLocationCode());
        result.setAction("UPDATED");
        result.setMessage("로케이션이 수정되었습니다.");
        return result;
    }

    // =========================================================
    // 단건 삭제
    // =========================================================

    @Transactional
    public LocationSaveResult delete(String areaCode, String locationCode) {
        List<String> locationIds = locationManageQueryService.findLocationIdsForSingleDelete(areaCode, locationCode);

        if (locationIds == null || locationIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "로케이션을 찾을 수 없습니다. areaCode=" + areaCode + ", locationCode=" + locationCode
            );
        }

        return deleteLocationsByIds(locationIds, "DELETED", areaCode, locationCode);
    }

    // =========================================================
    // 조회결과 일괄삭제
    // =========================================================

    @Transactional
    public LocationSaveResult bulkDelete(LocationBulkDeleteRequest request) {
        List<String> locationIds = locationManageQueryService.findLocationIdsForBulkDelete(
                request.getAreaCode(),
                request.getLocationCode(),
                request.getLocationType(),
                request.getActiveYn()
        );

        if (locationIds == null || locationIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 로케이션이 없습니다.");
        }

        return deleteLocationsByIds(locationIds, "BULK_DELETED", null, null);
    }

    /**
     * 삭제 대상 로케이션에 걸린 stock_unit 의 할당정보를 먼저 삭제.
     *
     * <p>
     * tb_ac_stock_allocation.stock_unit_id -> tb_ac_stock_unit FK 때문에
     * stock_unit 삭제 전에 반드시 제거해야 한다.
     * </p>
     */
    private String buildDeleteStockAllocationsSql() {
        return ""
                + "with recursive target_stock as ( "
                + "    select su.id "
                + "    from logis_asrs.tb_ac_stock_unit su "
                + "    where su.domain_id = :domainId "
                + "      and su.current_location_id in (:locationIds) "
                + "    union all "
                + "    select child.id "
                + "    from logis_asrs.tb_ac_stock_unit child "
                + "    inner join target_stock parent_ids "
                + "       on child.parent_stock_unit_id = parent_ids.id "
                + "    where child.domain_id = :domainId "
                + ") "
                + "delete from logis_asrs.tb_ac_stock_allocation sa "
                + "where sa.domain_id = :domainId "
                + "  and sa.stock_unit_id in ( "
                + "      select distinct id from target_stock "
                + "  ) ";
    }

    // =========================================================
    // 공통 SQL 기반 삭제
    // =========================================================

    /**
     * location id 목록 기준 재고이력 적재 + 재고 삭제 + 로케이션 삭제를 SQL 로 일괄 수행한다.
     *
     * <p>
     * 성능 목적:
     * - row by row delete 대신 SQL batch 형태로 round trip 최소화
     * </p>
     */
    @Transactional
    protected LocationSaveResult deleteLocationsByIds(
            List<String> locationIds,
            String action,
            String areaCode,
            String locationCode
    ) {
        if (locationIds == null || locationIds.isEmpty()) {
            throw new IllegalArgumentException("삭제 대상 location id 가 없습니다.");
        }

        int deletedStockCount = locationManageQueryService.countStockUnitsByLocationIds(locationIds);
        int deletedLocationCount = locationIds.size();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("locationIds", locationIds);
        params.put("txnType", AcTxnType.LOCATION_DELETE.name());

        // 1) 삭제 이력 적재
        this.queryManager.executeBySql(buildInsertLocationDeleteTxnSql(), params);

        // 2) 할당 해제(삭제)
        this.queryManager.executeBySql(buildDeleteStockAllocationsSql(), params);

        // 3) stock_unit 일괄삭제
        this.queryManager.executeBySql(buildDeleteStockUnitsSql(), params);

        // 4) location 일괄삭제
        this.queryManager.executeBySql(buildDeleteLocationsSql(), params);

        LocationSaveResult result = new LocationSaveResult();
        result.setAction(action);
        result.setAreaCode(areaCode);
        result.setLocationCode(locationCode);
        result.setDeletedStockCount(deletedStockCount);
        result.setDeletedLocationCount(deletedLocationCount);
        result.setMessage("로케이션 삭제가 완료되었습니다.");
        return result;
    }

    /**
     * 삭제 대상 재고를 기준으로 LOCATION_DELETE 이력을 일괄 적재.
     */
    private String buildInsertLocationDeleteTxnSql() {
        return ""
                + "insert into logis_asrs.tb_ac_stock_txn ( "
                + "    id, "
                + "    txn_no, "
                + "    txn_type, "
                + "    stock_unit_id, "
                + "    item_id, "
                + "    lot_id, "
                + "    from_location_id, "
                + "    to_location_id, "
                + "    qty, "
                + "    ref_doc_type, "
                + "    ref_doc_no, "
                + "    ref_line_no, "
                + "    reason_code, "
                + "    remark, "
                + "    txn_at, "
                + "    domain_id, "
                + "    creator_id, "
                + "    updater_id, "
                + "    created_at, "
                + "    updated_at "
                + ") "
                + "select "
                + "    replace(gen_random_uuid()::text, '-', ''), "
                + "    'TXN-LOC-DEL-' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 20), "
                + "    :txnType, "
                + "    su.id, "
                + "    su.item_id, "
                + "    su.lot_id, "
                + "    su.current_location_id, "
                + "    null, "
                + "    su.qty, "
                + "    'LOCATION', "
                + "    loc.location_code, "
                + "    null, "
                + "    'LOCATION_DELETE', "
                + "    'Location deleted. stock cleanup executed.', "
                + "    now(), "
                + "    su.domain_id, "
                + "    su.creator_id, "
                + "    su.updater_id, "
                + "    now(), "
                + "    now() "
                + "from logis_asrs.tb_ac_stock_unit su "
                + "inner join logis_asrs.tb_ac_location loc "
                + "   on loc.id = su.current_location_id "
                + "where su.domain_id = :domainId "
                + "  and su.current_location_id in (:locationIds) ";
    }

    /**
     * 삭제 대상 로케이션에 걸린 stock_unit 를 recursive cte 로 일괄 삭제.
     *
     * <p>
     * parent_stock_unit_id 자기참조 때문에 단순 delete 보다 recursive cte 방식이 안전하다.
     * </p>
     */
    private String buildDeleteStockUnitsSql() {
        return ""
                + "with recursive target_stock as ( "
                + "    select su.id "
                + "    from logis_asrs.tb_ac_stock_unit su "
                + "    where su.domain_id = :domainId "
                + "      and su.current_location_id in (:locationIds) "
                + "    union all "
                + "    select child.id "
                + "    from logis_asrs.tb_ac_stock_unit child "
                + "    inner join target_stock parent_ids "
                + "       on child.parent_stock_unit_id = parent_ids.id "
                + "    where child.domain_id = :domainId "
                + ") "
                + "delete from logis_asrs.tb_ac_stock_unit su "
                + "where su.id in ( "
                + "    select distinct id from target_stock "
                + ") ";
    }

    /**
     * location 일괄삭제.
     */
    private String buildDeleteLocationsSql() {
        return ""
                + "delete from logis_asrs.tb_ac_location "
                + "where domain_id = :domainId "
                + "  and id in (:locationIds) ";
    }

    // =========================================================
    // 기존 validate / resolve / normalize 메서드 그대로 유지
    // =========================================================

    private void validateRequest(LocationUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 없습니다.");
        }
        if (isBlank(request.getAreaCode())) {
            throw new IllegalArgumentException("areaCode 는 필수입니다.");
        }
        if (isBlank(request.getLocationCode())) {
            throw new IllegalArgumentException("locationCode 는 필수입니다.");
        }
        if (request.getAisleNo() == null) {
            throw new IllegalArgumentException("aisleNo 는 필수입니다.");
        }
        if (isBlank(request.getSideCode())) {
            throw new IllegalArgumentException("sideCode 는 필수입니다.");
        }
        if (request.getBayNo() == null) {
            throw new IllegalArgumentException("bayNo 는 필수입니다.");
        }
        if (request.getLevelNo() == null) {
            throw new IllegalArgumentException("levelNo 는 필수입니다.");
        }
        if (request.getDepthNo() == null) {
            throw new IllegalArgumentException("depthNo 는 필수입니다.");
        }
        if (isBlank(request.getLocationType())) {
            throw new IllegalArgumentException("locationType 는 필수입니다.");
        }
        if (isBlank(request.getUsageStatusCode())) {
            throw new IllegalArgumentException("usageStatusCode 는 필수입니다.");
        }
        if (isBlank(request.getInboundAllowedYn())) {
            throw new IllegalArgumentException("inboundAllowedYn 는 필수입니다.");
        }
        if (isBlank(request.getOutboundAllowedYn())) {
            throw new IllegalArgumentException("outboundAllowedYn 는 필수입니다.");
        }
        if (isBlank(request.getMixedLoadYn())) {
            throw new IllegalArgumentException("mixedLoadYn 는 필수입니다.");
        }
        if (isBlank(request.getFrontPriorityYn())) {
            throw new IllegalArgumentException("frontPriorityYn 는 필수입니다.");
        }
        if (request.getSortSeq() != null && request.getSortSeq() < 1) {
            throw new IllegalArgumentException("sortSeq 는 1 이상이어야 합니다.");
        }
        if (isBlank(request.getActiveYn())) {
            throw new IllegalArgumentException("activeYn 는 필수입니다.");
        }
        if (isBlank(request.getLocationGrade())) {
            throw new IllegalArgumentException("locationGrade 는 필수입니다.");
        }
    }

    private String requireAreaId(String areaCode) {
        String areaId = locationManageQueryService.resolveAreaIdByCode(areaCode.trim());
        if (areaId == null || areaId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 areaCode 입니다. areaCode=" + areaCode);
        }
        return areaId;
    }

    private String resolveItemCategoryId(LocationUpsertRequest request) {
        String itemCategoryId = locationManageQueryService.resolveItemCategoryIdByCode(
                request.getDedicatedItemCategoryCode()
        );

        if (!isBlank(request.getDedicatedItemCategoryCode()) && (itemCategoryId == null || itemCategoryId.trim().isEmpty())) {
            throw new IllegalArgumentException(
                    "유효하지 않은 dedicatedItemCategoryCode 입니다. dedicatedItemCategoryCode="
                            + request.getDedicatedItemCategoryCode()
            );
        }

        return itemCategoryId;
    }

    private String resolveAccessPointId(LocationUpsertRequest request) {
        String accessPointId = locationManageQueryService.resolveAccessPointIdByCode(
                request.getAreaCode().trim(),
                request.getPrimaryAccessPointCode()
        );

        if (!isBlank(request.getPrimaryAccessPointCode()) && (accessPointId == null || accessPointId.trim().isEmpty())) {
            throw new IllegalArgumentException(
                    "유효하지 않은 primaryAccessPointCode 입니다. primaryAccessPointCode="
                            + request.getPrimaryAccessPointCode()
            );
        }

        return accessPointId;
    }

    private String normalizeSideCode(String value) {
        return AcLocationSide.from(value).name();
    }

    private String normalizeLocationType(String value) {
        return AcLocationType.fromCode(value).getCode();
    }

    private String normalizeUsageStatus(String value) {
        return AcLocationUsageStatus.fromCode(value).getCode();
    }

    private String normalizeLocationGrade(String value) {
        return AcLocationGrade.fromCode(value).getCode();
    }

    private String normalizeYn(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new IllegalArgumentException(fieldName + " 는 Y 또는 N 이어야 합니다.");
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}