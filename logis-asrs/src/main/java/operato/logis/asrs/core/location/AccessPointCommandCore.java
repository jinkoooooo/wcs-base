package operato.logis.asrs.core.location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.request.AccessPointPurposeSaveRequest;
import operato.logis.asrs.dto.request.AccessPointSaveRequest;
import operato.logis.asrs.dto.response.AccessPointPurposeResponse;
import operato.logis.asrs.dto.response.AccessPointResponse;
import operato.logis.asrs.entity.TbAcAccessPoint;
import operato.logis.asrs.entity.TbAcAccessPointPurpose;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.location.AccessPointManageQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import operato.logis.asrs.query.location.model.AccessPointManageRow;
import operato.logis.asrs.query.location.model.AccessPointPurposeRow;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * Access Point 저장/수정/삭제 코어.
 */
@Service
public class AccessPointCommandCore extends AbstractQueryService {

    private static final String Y = "Y";
    private static final String N = "N";

    private final LocationQueryService locationQueryService;
    private final AccessPointManageQueryService accessPointManageQueryService;

    public AccessPointCommandCore(LocationQueryService locationQueryService, AccessPointManageQueryService accessPointManageQueryService) {
        this.locationQueryService = locationQueryService;
        this.accessPointManageQueryService = accessPointManageQueryService;
    }

    /**
     * Access Point 상세 조회.
     *
     * @param areaCode 영역 코드
     * @param pointCode 포인트 코드
     * @return 상세 응답
     */
    @Transactional(readOnly = true)
    public AccessPointResponse detail(String areaCode, String pointCode) {
        validateCodeKey(areaCode, pointCode);

        AccessPointManageRow row = accessPointManageQueryService.findByCode(areaCode, pointCode);
        if (row == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Access point not found. areaCode=" + areaCode + ", pointCode=" + pointCode
            );
        }

        AccessPointResponse response = toResponse(row);
        response.setPurposes(toPurposeResponses(accessPointManageQueryService.findPurposes(row.getId())));
        response.setMessage("Access point detail loaded.");

        return response;
    }

    /**
     * Access Point 신규 생성.
     *
     * @param request 저장 요청
     * @return 저장 결과
     */
    @Transactional
    public AccessPointResponse create(AccessPointSaveRequest request) {
        validateSaveRequest(request);

        TbAcStorageArea area = locationQueryService.findAreaByCode(request.getAreaCode());
        String pointCode = normalizeCode(request.getPointCode());

        AccessPointManageRow duplicated = accessPointManageQueryService.findByAreaIdAndPointCode(
                area.getId(),
                pointCode
        );

        if (duplicated != null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.DUPLICATE_DATA,
                    "Duplicated pointCode exists. areaCode=" + request.getAreaCode() + ", pointCode=" + pointCode
            );
        }

        TbAcAccessPoint entity = new TbAcAccessPoint();
        applyRequestToEntity(entity, area, request);
        entity.setPointCode(pointCode);

        this.queryManager.insert(entity);

        upsertPurposes(entity.getId(), request.getPurposes());

        AccessPointResponse response = detail(area.getAreaCode(), entity.getPointCode());
        response.setAction("created");
        response.setMessage("Access point created.");

        return response;
    }

    /**
     * Access Point 수정.
     *
     * @param originalAreaCode 기존 영역 코드
     * @param originalPointCode 기존 포인트 코드
     * @param request 수정 요청
     * @return 수정 결과
     */
    @Transactional
    public AccessPointResponse update(String originalAreaCode,
                                      String originalPointCode,
                                      AccessPointSaveRequest request) {
        validateCodeKey(originalAreaCode, originalPointCode);
        validateSaveRequest(request);

        AccessPointManageRow existingRow = accessPointManageQueryService.findByCode(
                originalAreaCode,
                originalPointCode
        );

        if (existingRow == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Access point not found. areaCode=" + originalAreaCode + ", pointCode=" + originalPointCode
            );
        }

        TbAcStorageArea targetArea = locationQueryService.findAreaByCode(request.getAreaCode());
        String nextPointCode = normalizeCode(request.getPointCode());

        AccessPointManageRow duplicated = accessPointManageQueryService.findByAreaIdAndPointCode(
                targetArea.getId(),
                nextPointCode
        );

        if (duplicated != null && !existingRow.getId().equals(duplicated.getId())) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.DUPLICATE_DATA,
                    "Duplicated pointCode exists. areaCode=" + request.getAreaCode() + ", pointCode=" + nextPointCode
            );
        }

        TbAcAccessPoint entity = this.queryManager.select(TbAcAccessPoint.class, existingRow.getId());
        if (entity == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Access point entity not found. id=" + existingRow.getId()
            );
        }

        applyRequestToEntity(entity, targetArea, request);
        entity.setPointCode(nextPointCode);

        this.queryManager.update(
                entity,
                "areaId",
                "pointCode",
                "pointName",
                "pointType",
                "aisleNo",
                "sideCode",
                "bayNo",
                "levelNo",
                "depthNo",
                "useForSortYn",
                "activeYn",
                "description"
        );

        upsertPurposes(entity.getId(), request.getPurposes());

        AccessPointResponse response = detail(targetArea.getAreaCode(), entity.getPointCode());
        response.setAction("updated");
        response.setMessage("Access point updated.");

        return response;
    }

    /**
     * Access Point 삭제.
     *
     * <p>
     * FK 참조 안정성을 위해 물리 삭제하지 않고 비활성 처리한다.
     * 목적 정보도 active_yn = 'N' 처리한다.
     * </p>
     *
     * @param areaCode 영역 코드
     * @param pointCode 포인트 코드
     * @return 삭제 결과
     */
    @Transactional
    public AccessPointResponse delete(String areaCode, String pointCode) {
        validateCodeKey(areaCode, pointCode);

        AccessPointManageRow existingRow = accessPointManageQueryService.findByCode(areaCode, pointCode);
        if (existingRow == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Access point not found. areaCode=" + areaCode + ", pointCode=" + pointCode
            );
        }

        TbAcAccessPoint entity = this.queryManager.select(TbAcAccessPoint.class, existingRow.getId());
        if (entity != null) {
            entity.setActiveYn(N);
            entity.setUseForSortYn(N);
            this.queryManager.update(entity, "activeYn", "useForSortYn");
        }

        List<AccessPointPurposeRow> purposes = accessPointManageQueryService.findPurposes(existingRow.getId());
        if (purposes != null) {
            for (AccessPointPurposeRow purpose : purposes) {
                TbAcAccessPointPurpose purposeEntity =
                        this.queryManager.select(TbAcAccessPointPurpose.class, purpose.getId());

                if (purposeEntity == null) {
                    continue;
                }

                purposeEntity.setActiveYn(N);
                this.queryManager.update(purposeEntity, "activeYn");
            }
        }

        AccessPointResponse response = toResponse(existingRow);
        response.setAction("deleted");
        response.setMessage("Access point disabled.");

        return response;
    }

    /**
     * 요청값을 Access Point 엔티티에 반영한다.
     *
     * @param entity Access Point 엔티티
     * @param area 영역 엔티티
     * @param request 요청 DTO
     */
    private void applyRequestToEntity(TbAcAccessPoint entity,
                                      TbAcStorageArea area,
                                      AccessPointSaveRequest request) {
        entity.setAreaId(area.getId());
        entity.setPointName(request.getPointName().trim());
        entity.setPointType(defaultString(request.getPointType(), "PORT"));
        entity.setAisleNo(request.getAisleNo());
        entity.setSideCode(request.getSideCode().trim().toUpperCase());
        entity.setBayNo(request.getBayNo());
        entity.setLevelNo(request.getLevelNo());
        entity.setDepthNo(request.getDepthNo());
        entity.setUseForSortYn(defaultYn(request.getUseForSortYn(), Y));
        entity.setActiveYn(defaultYn(request.getActiveYn(), Y));
        entity.setDescription(request.getDescription());

        validatePointType(entity.getPointType());
        validateYn("useForSortYn", entity.getUseForSortYn());
        validateYn("activeYn", entity.getActiveYn());
    }

    /**
     * 목적 정보 upsert.
     *
     * <p>
     * 요청에 없는 기존 목적은 active_yn = 'N' 처리한다.
     * </p>
     *
     * @param accessPointId Access Point ID
     * @param requests 목적 요청 목록
     */
    private void upsertPurposes(String accessPointId, List<AccessPointPurposeSaveRequest> requests) {
        List<AccessPointPurposeSaveRequest> safeRequests =
                requests == null ? new ArrayList<AccessPointPurposeSaveRequest>() : requests;

        Set<String> requestedPurposeCodes = new HashSet<String>();

        for (AccessPointPurposeSaveRequest request : safeRequests) {
            if (request == null || ValueUtil.isEmpty(request.getPurposeCode())) {
                continue;
            }

            String purposeCode = request.getPurposeCode().trim().toUpperCase();
            validatePurposeCode(purposeCode);

            String activeYn = defaultYn(request.getActiveYn(), N);
            validateYn("purpose.activeYn", activeYn);

            Integer priorityNo = request.getPriorityNo() == null || request.getPriorityNo() < 1
                    ? Integer.valueOf(1)
                    : request.getPriorityNo();

            TbAcAccessPointPurpose existing = findPurposeEntity(accessPointId, purposeCode);

            if (existing == null) {
                TbAcAccessPointPurpose purpose = new TbAcAccessPointPurpose();
                purpose.setAccessPointId(accessPointId);
                purpose.setPurposeCode(purposeCode);
                purpose.setPriorityNo(priorityNo);
                purpose.setActiveYn(activeYn);
                purpose.setDescription(request.getDescription());

                this.queryManager.insert(purpose);
            } else {
                existing.setPriorityNo(priorityNo);
                existing.setActiveYn(activeYn);
                existing.setDescription(request.getDescription());

                this.queryManager.update(
                        existing,
                        "priorityNo",
                        "activeYn",
                        "description"
                );
            }

            requestedPurposeCodes.add(purposeCode);
        }

        List<AccessPointPurposeRow> existingPurposes = accessPointManageQueryService.findPurposes(accessPointId);
        if (existingPurposes == null) {
            return;
        }

        for (AccessPointPurposeRow existing : existingPurposes) {
            if (requestedPurposeCodes.contains(existing.getPurposeCode())) {
                continue;
            }

            TbAcAccessPointPurpose entity = this.queryManager.select(
                    TbAcAccessPointPurpose.class,
                    existing.getId()
            );

            if (entity == null) {
                continue;
            }

            entity.setActiveYn(N);
            this.queryManager.update(entity, "activeYn");
        }
    }

    /**
     * 목적 엔티티 조회.
     */
    private TbAcAccessPointPurpose findPurposeEntity(String accessPointId, String purposeCode) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("access_point_id", accessPointId);
        condition.addFilter("purpose_code", purposeCode);

        return this.queryManager.select(TbAcAccessPointPurpose.class, condition);
    }

    /**
     * 조회 모델을 응답 DTO로 변환한다.
     */
    private AccessPointResponse toResponse(AccessPointManageRow row) {
        AccessPointResponse response = new AccessPointResponse();

        response.setId(row.getId());
        response.setAreaId(row.getAreaId());
        response.setAreaCode(row.getAreaCode());
        response.setAreaName(row.getAreaName());
        response.setPointCode(row.getPointCode());
        response.setPointName(row.getPointName());
        response.setPointType(row.getPointType());
        response.setAisleNo(row.getAisleNo());
        response.setSideCode(row.getSideCode());
        response.setBayNo(row.getBayNo());
        response.setLevelNo(row.getLevelNo());
        response.setDepthNo(row.getDepthNo());
        response.setUseForSortYn(row.getUseForSortYn());
        response.setActiveYn(row.getActiveYn());
        response.setDescription(row.getDescription());
        response.setPurposeCodes(row.getPurposeCodes());

        return response;
    }

    /**
     * 목적 조회 모델을 응답 DTO로 변환한다.
     */
    private List<AccessPointPurposeResponse> toPurposeResponses(List<AccessPointPurposeRow> rows) {
        List<AccessPointPurposeResponse> responses = new ArrayList<AccessPointPurposeResponse>();

        if (rows == null) {
            return responses;
        }

        for (AccessPointPurposeRow row : rows) {
            AccessPointPurposeResponse response = new AccessPointPurposeResponse();
            response.setId(row.getId());
            response.setAccessPointId(row.getAccessPointId());
            response.setPurposeCode(row.getPurposeCode());
            response.setPriorityNo(row.getPriorityNo());
            response.setActiveYn(row.getActiveYn());
            response.setDescription(row.getDescription());

            responses.add(response);
        }

        return responses;
    }

    private void validateSaveRequest(AccessPointSaveRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "request is null.");
        }

        if (ValueUtil.isEmpty(request.getAreaCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }

        if (ValueUtil.isEmpty(request.getPointCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "pointCode is empty.");
        }

        if (ValueUtil.isEmpty(request.getPointName())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "pointName is empty.");
        }

        if (request.getAisleNo() == null || request.getBayNo() == null
                || request.getLevelNo() == null || request.getDepthNo() == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "coordinate is required.");
        }

        if (ValueUtil.isEmpty(request.getSideCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "sideCode is empty.");
        }
    }

    private void validateCodeKey(String areaCode, String pointCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }

        if (ValueUtil.isEmpty(pointCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "pointCode is empty.");
        }
    }

    private void validatePointType(String pointType) {
        if ("PORT".equals(pointType)
                || "LIFT".equals(pointType)
                || "PICK_FACE".equals(pointType)
                || "BUFFER_POINT".equals(pointType)
                || "WORK_POINT".equals(pointType)
                || "CRANE_HOME".equals(pointType)) {
            return;
        }

        throw new AisleCoreException(
                AisleCoreErrorCode.INVALID_REQUEST,
                "Invalid pointType. pointType=" + pointType
        );
    }

    private void validatePurposeCode(String purposeCode) {
        if ("INBOUND".equals(purposeCode)
                || "OUTBOUND".equals(purposeCode)
                || "PICK".equals(purposeCode)
                || "RELOCATION".equals(purposeCode)) {
            return;
        }

        throw new AisleCoreException(
                AisleCoreErrorCode.INVALID_REQUEST,
                "Invalid purposeCode. purposeCode=" + purposeCode
        );
    }

    private void validateYn(String name, String value) {
        if (Y.equals(value) || N.equals(value)) {
            return;
        }

        throw new AisleCoreException(
                AisleCoreErrorCode.INVALID_REQUEST,
                "Invalid Y/N value. name=" + name + ", value=" + value
        );
    }

    private String defaultYn(String value, String defaultValue) {
        return ValueUtil.isEmpty(value) ? defaultValue : value.trim().toUpperCase();
    }

    private String defaultString(String value, String defaultValue) {
        return ValueUtil.isEmpty(value) ? defaultValue : value.trim();
    }

    private String normalizeCode(String value) {
        String normalized = value.trim().toUpperCase();
        return normalized.length() > 30 ? normalized.substring(0, 30) : normalized;
    }
}