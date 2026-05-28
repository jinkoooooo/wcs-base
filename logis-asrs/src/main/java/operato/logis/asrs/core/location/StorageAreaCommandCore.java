package operato.logis.asrs.core.location;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.dto.request.StorageAreaUpsertRequest;
import operato.logis.asrs.dto.response.StorageAreaSaveResult;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.enums.AcAreaType;
import operato.logis.asrs.query.location.StorageAreaQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 아레아 명령 Core.
 */
@Service
public class StorageAreaCommandCore extends AbstractQueryService {

    private final StorageAreaQueryService storageAreaQueryService;

    public StorageAreaCommandCore(StorageAreaQueryService storageAreaQueryService) {
        this.storageAreaQueryService = storageAreaQueryService;
    }

    /**
     * 신규 생성.
     */
    @Transactional
    public StorageAreaSaveResult create(StorageAreaUpsertRequest request) {
        validateRequest(request);

        TbAcStorageArea duplicate = storageAreaQueryService.findEntityByCenterAndAreaCode(
                request.getCenterCode().trim(),
                request.getAreaCode().trim()
        );
        if (duplicate != null) {
            throw new IllegalArgumentException(
                    "이미 존재하는 area_code 입니다. centerCode=" + request.getCenterCode()
                            + ", areaCode=" + request.getAreaCode()
            );
        }

        String centerId = storageAreaQueryService.resolveCenterIdByCode(request.getCenterCode().trim());
        if (centerId == null || centerId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 centerCode 입니다. centerCode=" + request.getCenterCode());
        }

        String operationProfileId = storageAreaQueryService.resolveOperationProfileIdByCode(
                request.getOperationProfileCode().trim()
        );
        if (operationProfileId == null || operationProfileId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "유효하지 않은 operationProfileCode 입니다. operationProfileCode=" + request.getOperationProfileCode()
            );
        }

        TbAcStorageArea entity = new TbAcStorageArea();
        entity.setCenterId(centerId);
        entity.setAreaCode(request.getAreaCode().trim());
        entity.setAreaName(request.getAreaName().trim());
        entity.setAreaType(normalizeAreaType(request.getAreaType()));
        entity.setOperationProfileId(operationProfileId);
        entity.setDescription(normalizeText(request.getDescription()));
        entity.setActiveYn(normalizeYn(request.getActiveYn()));

        this.queryManager.insert(entity);

        StorageAreaSaveResult result = new StorageAreaSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(entity.getAreaCode());
        result.setAction("CREATED");
        result.setMessage("아레아가 생성되었습니다.");
        return result;
    }

    /**
     * 수정.
     */
    @Transactional
    public StorageAreaSaveResult update(
            String originalCenterCode,
            String originalAreaCode,
            StorageAreaUpsertRequest request
    ) {
        validateRequest(request);

        TbAcStorageArea entity = storageAreaQueryService.findEntityByCenterAndAreaCode(
                originalCenterCode,
                originalAreaCode
        );
        if (entity == null) {
            throw new IllegalArgumentException(
                    "아레아를 찾을 수 없습니다. centerCode=" + originalCenterCode + ", areaCode=" + originalAreaCode
            );
        }

        boolean keyChanged =
                !originalCenterCode.equalsIgnoreCase(request.getCenterCode().trim())
                        || !originalAreaCode.equalsIgnoreCase(request.getAreaCode().trim());

        if (keyChanged) {
            TbAcStorageArea duplicate = storageAreaQueryService.findEntityByCenterAndAreaCode(
                    request.getCenterCode().trim(),
                    request.getAreaCode().trim()
            );
            if (duplicate != null) {
                throw new IllegalArgumentException(
                        "이미 존재하는 area_code 입니다. centerCode=" + request.getCenterCode()
                                + ", areaCode=" + request.getAreaCode()
                );
            }
        }

        String centerId = storageAreaQueryService.resolveCenterIdByCode(request.getCenterCode().trim());
        if (centerId == null || centerId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 centerCode 입니다. centerCode=" + request.getCenterCode());
        }

        String operationProfileId = storageAreaQueryService.resolveOperationProfileIdByCode(
                request.getOperationProfileCode().trim()
        );
        if (operationProfileId == null || operationProfileId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "유효하지 않은 operationProfileCode 입니다. operationProfileCode=" + request.getOperationProfileCode()
            );
        }

        entity.setCenterId(centerId);
        entity.setAreaCode(request.getAreaCode().trim());
        entity.setAreaName(request.getAreaName().trim());
        entity.setAreaType(normalizeAreaType(request.getAreaType()));
        entity.setOperationProfileId(operationProfileId);
        entity.setDescription(normalizeText(request.getDescription()));
        entity.setActiveYn(normalizeYn(request.getActiveYn()));

        this.queryManager.update(entity);

        StorageAreaSaveResult result = new StorageAreaSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(entity.getAreaCode());
        result.setAction("UPDATED");
        result.setMessage("아레아가 수정되었습니다.");
        return result;
    }

    /**
     * 삭제.
     *
     * location_profile / location 참조가 있으면 삭제 불가.
     */
    @Transactional
    public StorageAreaSaveResult delete(String centerCode, String areaCode) {
        TbAcStorageArea entity = storageAreaQueryService.findEntityByCenterAndAreaCode(centerCode, areaCode);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "아레아를 찾을 수 없습니다. centerCode=" + centerCode + ", areaCode=" + areaCode
            );
        }

        long linkedLocationProfileCount = storageAreaQueryService.countLinkedLocationProfiles(entity.getId());
        if (linkedLocationProfileCount > 0) {
            throw new IllegalArgumentException(
                    "참조 중인 로케이션 프로필이 있어 삭제할 수 없습니다. linkedLocationProfileCount="
                            + linkedLocationProfileCount
            );
        }

        long linkedLocationCount = storageAreaQueryService.countLinkedLocations(entity.getId());
        if (linkedLocationCount > 0) {
            throw new IllegalArgumentException(
                    "참조 중인 로케이션이 있어 삭제할 수 없습니다. linkedLocationCount=" + linkedLocationCount
            );
        }

        this.queryManager.delete(entity);

        StorageAreaSaveResult result = new StorageAreaSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(entity.getAreaCode());
        result.setAction("DELETED");
        result.setMessage("아레아가 삭제되었습니다.");
        return result;
    }

    /**
     * 요청 검증.
     */
    private void validateRequest(StorageAreaUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 없습니다.");
        }
        if (request.getCenterCode() == null || request.getCenterCode().trim().isEmpty()) {
            throw new IllegalArgumentException("centerCode 는 필수입니다.");
        }
        if (request.getAreaCode() == null || request.getAreaCode().trim().isEmpty()) {
            throw new IllegalArgumentException("areaCode 는 필수입니다.");
        }
        if (request.getAreaName() == null || request.getAreaName().trim().isEmpty()) {
            throw new IllegalArgumentException("areaName 는 필수입니다.");
        }
        if (request.getAreaType() == null || request.getAreaType().trim().isEmpty()) {
            throw new IllegalArgumentException("areaType 는 필수입니다.");
        }
        if (request.getOperationProfileCode() == null || request.getOperationProfileCode().trim().isEmpty()) {
            throw new IllegalArgumentException("operationProfileCode 는 필수입니다.");
        }
        if (request.getActiveYn() == null || request.getActiveYn().trim().isEmpty()) {
            throw new IllegalArgumentException("activeYn 는 필수입니다.");
        }
    }

    private String normalizeAreaType(String value) {
        return AcAreaType.fromCode(value).getCode();
    }

    private String normalizeYn(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new IllegalArgumentException("activeYn 는 Y 또는 N 이어야 합니다.");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}