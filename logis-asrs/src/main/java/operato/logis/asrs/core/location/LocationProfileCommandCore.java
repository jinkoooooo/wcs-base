package operato.logis.asrs.core.location;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.dto.request.LocationProfileUpsertRequest;
import operato.logis.asrs.dto.response.LocationProfileSaveResult;
import operato.logis.asrs.entity.TbAcLocationProfile;
import operato.logis.asrs.enums.AcLocationSide;
import operato.logis.asrs.enums.AcLocationType;
import operato.logis.asrs.query.location.LocationProfileManageQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 로케이션 프로필 명령 Core.
 */
@Service
public class LocationProfileCommandCore extends AbstractQueryService {

    private final LocationProfileManageQueryService locationProfileManageQueryService;

    public LocationProfileCommandCore(LocationProfileManageQueryService locationProfileManageQueryService) {
        this.locationProfileManageQueryService = locationProfileManageQueryService;
    }

    @Transactional
    public LocationProfileSaveResult create(LocationProfileUpsertRequest request) {
        validateRequest(request);

        TbAcLocationProfile duplicate = locationProfileManageQueryService.findEntityByAreaAndProfileCode(
                request.getAreaCode().trim(),
                request.getProfileCode().trim()
        );
        if (duplicate != null) {
            throw new IllegalArgumentException(
                    "이미 존재하는 location profile 입니다. areaCode=" + request.getAreaCode()
                            + ", profileCode=" + request.getProfileCode()
            );
        }

        String areaId = locationProfileManageQueryService.resolveAreaIdByCode(request.getAreaCode().trim());
        if (areaId == null || areaId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 areaCode 입니다. areaCode=" + request.getAreaCode());
        }

        TbAcLocationProfile entity = new TbAcLocationProfile();
        entity.setAreaId(areaId);
        entity.setProfileCode(request.getProfileCode().trim());
        entity.setProfileName(request.getProfileName().trim());
        entity.setAisleStart(request.getAisleStart());
        entity.setAisleEnd(request.getAisleEnd());
        entity.setSideCodes(normalizeSideCodes(request.getSideCodes()));
        entity.setBayStart(request.getBayStart());
        entity.setBayEnd(request.getBayEnd());
        entity.setLevelStart(request.getLevelStart());
        entity.setLevelEnd(request.getLevelEnd());
        entity.setDepthStart(request.getDepthStart());
        entity.setDepthEnd(request.getDepthEnd());
        entity.setLocationType(normalizeLocationType(request.getLocationType()));
        entity.setCodePattern(request.getCodePattern().trim());
        entity.setMixedLoadYn(normalizeYn(request.getMixedLoadYn(), "mixedLoadYn"));
        entity.setInboundAllowedYn(normalizeYn(request.getInboundAllowedYn(), "inboundAllowedYn"));
        entity.setOutboundAllowedYn(normalizeYn(request.getOutboundAllowedYn(), "outboundAllowedYn"));
        entity.setActiveYn(normalizeYn(request.getActiveYn(), "activeYn"));

        this.queryManager.insert(entity);

        LocationProfileSaveResult result = new LocationProfileSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(request.getAreaCode().trim());
        result.setProfileCode(entity.getProfileCode());
        result.setAction("CREATED");
        result.setMessage("로케이션 프로필이 생성되었습니다.");
        return result;
    }

    @Transactional
    public LocationProfileSaveResult update(
            String originalAreaCode,
            String originalProfileCode,
            LocationProfileUpsertRequest request
    ) {
        validateRequest(request);

        TbAcLocationProfile entity = locationProfileManageQueryService.findEntityByAreaAndProfileCode(
                originalAreaCode,
                originalProfileCode
        );
        if (entity == null) {
            throw new IllegalArgumentException(
                    "로케이션 프로필을 찾을 수 없습니다. areaCode=" + originalAreaCode
                            + ", profileCode=" + originalProfileCode
            );
        }

        boolean keyChanged =
                !originalAreaCode.equalsIgnoreCase(request.getAreaCode().trim())
                        || !originalProfileCode.equalsIgnoreCase(request.getProfileCode().trim());

        if (keyChanged) {
            TbAcLocationProfile duplicate = locationProfileManageQueryService.findEntityByAreaAndProfileCode(
                    request.getAreaCode().trim(),
                    request.getProfileCode().trim()
            );
            if (duplicate != null) {
                throw new IllegalArgumentException(
                        "이미 존재하는 location profile 입니다. areaCode=" + request.getAreaCode()
                                + ", profileCode=" + request.getProfileCode()
                );
            }
        }

        String areaId = locationProfileManageQueryService.resolveAreaIdByCode(request.getAreaCode().trim());
        if (areaId == null || areaId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 areaCode 입니다. areaCode=" + request.getAreaCode());
        }

        entity.setAreaId(areaId);
        entity.setProfileCode(request.getProfileCode().trim());
        entity.setProfileName(request.getProfileName().trim());
        entity.setAisleStart(request.getAisleStart());
        entity.setAisleEnd(request.getAisleEnd());
        entity.setSideCodes(normalizeSideCodes(request.getSideCodes()));
        entity.setBayStart(request.getBayStart());
        entity.setBayEnd(request.getBayEnd());
        entity.setLevelStart(request.getLevelStart());
        entity.setLevelEnd(request.getLevelEnd());
        entity.setDepthStart(request.getDepthStart());
        entity.setDepthEnd(request.getDepthEnd());
        entity.setLocationType(normalizeLocationType(request.getLocationType()));
        entity.setCodePattern(request.getCodePattern().trim());
        entity.setMixedLoadYn(normalizeYn(request.getMixedLoadYn(), "mixedLoadYn"));
        entity.setInboundAllowedYn(normalizeYn(request.getInboundAllowedYn(), "inboundAllowedYn"));
        entity.setOutboundAllowedYn(normalizeYn(request.getOutboundAllowedYn(), "outboundAllowedYn"));
        entity.setActiveYn(normalizeYn(request.getActiveYn(), "activeYn"));

        this.queryManager.update(entity);

        LocationProfileSaveResult result = new LocationProfileSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(request.getAreaCode().trim());
        result.setProfileCode(entity.getProfileCode());
        result.setAction("UPDATED");
        result.setMessage("로케이션 프로필이 수정되었습니다.");
        return result;
    }

    @Transactional
    public LocationProfileSaveResult delete(String areaCode, String profileCode) {
        TbAcLocationProfile entity = locationProfileManageQueryService.findEntityByAreaAndProfileCode(areaCode, profileCode);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "로케이션 프로필을 찾을 수 없습니다. areaCode=" + areaCode + ", profileCode=" + profileCode
            );
        }

        long linkedLocationCount = locationProfileManageQueryService.countLinkedLocations(entity.getAreaId());
        if (linkedLocationCount > 0) {
            throw new IllegalArgumentException(
                    "참조 중인 로케이션이 있어 삭제할 수 없습니다. linkedLocationCount=" + linkedLocationCount
            );
        }

        this.queryManager.delete(entity);

        LocationProfileSaveResult result = new LocationProfileSaveResult();
        result.setId(entity.getId());
        result.setAreaCode(areaCode);
        result.setProfileCode(profileCode);
        result.setAction("DELETED");
        result.setMessage("로케이션 프로필이 삭제되었습니다.");
        return result;
    }

    private void validateRequest(LocationProfileUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 없습니다.");
        }
        if (isBlank(request.getAreaCode())) {
            throw new IllegalArgumentException("areaCode 는 필수입니다.");
        }
        if (isBlank(request.getProfileCode())) {
            throw new IllegalArgumentException("profileCode 는 필수입니다.");
        }
        if (isBlank(request.getProfileName())) {
            throw new IllegalArgumentException("profileName 는 필수입니다.");
        }
        if (request.getAisleStart() == null || request.getAisleEnd() == null) {
            throw new IllegalArgumentException("aisleStart / aisleEnd 는 필수입니다.");
        }
        if (request.getBayStart() == null || request.getBayEnd() == null) {
            throw new IllegalArgumentException("bayStart / bayEnd 는 필수입니다.");
        }
        if (request.getLevelStart() == null || request.getLevelEnd() == null) {
            throw new IllegalArgumentException("levelStart / levelEnd 는 필수입니다.");
        }
        if (request.getDepthStart() == null || request.getDepthEnd() == null) {
            throw new IllegalArgumentException("depthStart / depthEnd 는 필수입니다.");
        }
        if (isBlank(request.getSideCodes())) {
            throw new IllegalArgumentException("sideCodes 는 필수입니다.");
        }
        if (isBlank(request.getLocationType())) {
            throw new IllegalArgumentException("locationType 는 필수입니다.");
        }
        if (isBlank(request.getCodePattern())) {
            throw new IllegalArgumentException("codePattern 는 필수입니다.");
        }
        if (isBlank(request.getMixedLoadYn())) {
            throw new IllegalArgumentException("mixedLoadYn 는 필수입니다.");
        }
        if (isBlank(request.getInboundAllowedYn())) {
            throw new IllegalArgumentException("inboundAllowedYn 는 필수입니다.");
        }
        if (isBlank(request.getOutboundAllowedYn())) {
            throw new IllegalArgumentException("outboundAllowedYn 는 필수입니다.");
        }
        if (isBlank(request.getActiveYn())) {
            throw new IllegalArgumentException("activeYn 는 필수입니다.");
        }

        validateRange("aisle", request.getAisleStart(), request.getAisleEnd());
        validateRange("bay", request.getBayStart(), request.getBayEnd());
        validateRange("level", request.getLevelStart(), request.getLevelEnd());
        validateRange("depth", request.getDepthStart(), request.getDepthEnd());
    }

    private void validateRange(String name, Integer start, Integer end) {
        if (start < 0 || end < 0 || start > end) {
            throw new IllegalArgumentException(
                    "유효하지 않은 범위입니다. name=" + name + ", start=" + start + ", end=" + end
            );
        }
    }

    private String normalizeLocationType(String value) {
        return AcLocationType.fromCode(value).getCode();
    }

    private String normalizeYn(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new IllegalArgumentException(fieldName + " 는 Y 또는 N 이어야 합니다.");
        }
        return normalized;
    }

    private String normalizeSideCodes(String rawSideCodes) {
        String[] tokens = rawSideCodes.split(",");
        StringBuilder normalized = new StringBuilder();

        for (String token : tokens) {
            String side = token == null ? "" : token.trim().toUpperCase();
            if (side.isEmpty()) {
                continue;
            }

            AcLocationSide.from(side);

            if (normalized.length() > 0) {
                normalized.append(",");
            }
            normalized.append(side);
        }

        if (normalized.length() == 0) {
            throw new IllegalArgumentException("유효한 sideCodes 가 없습니다.");
        }

        return normalized.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}