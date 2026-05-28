package operato.logis.asrs.core.location;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.dto.request.OperationProfileUpsertRequest;
import operato.logis.asrs.dto.response.OperationProfileSaveResult;
import operato.logis.asrs.entity.TbAcOperationProfile;
import operato.logis.asrs.enums.AcIndustryType;
import operato.logis.asrs.query.location.OperationProfileQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 오퍼레이션 프로필 명령 Core.
 */
@Service
public class OperationProfileCommandCore extends AbstractQueryService {

    private final OperationProfileQueryService operationProfileQueryService;

    public OperationProfileCommandCore(OperationProfileQueryService operationProfileQueryService) {
        this.operationProfileQueryService = operationProfileQueryService;
    }

    /**
     * 신규 생성.
     */
    @Transactional
    public OperationProfileSaveResult create(OperationProfileUpsertRequest request) {
        validateRequest(request);

        TbAcOperationProfile exists = operationProfileQueryService.findEntityByCode(request.getProfileCode());
        if (exists != null) {
            throw new IllegalArgumentException("이미 존재하는 profile_code 입니다. profileCode=" + request.getProfileCode());
        }

        TbAcOperationProfile entity = new TbAcOperationProfile();
        entity.setProfileCode(request.getProfileCode().trim());
        entity.setProfileName(request.getProfileName().trim());
        entity.setIndustryType(normalizeIndustryType(request.getIndustryType()));
        entity.setDescription(normalizeText(request.getDescription()));
        entity.setActiveYn(normalizeYn(request.getActiveYn()));

        this.queryManager.insert(entity);

        OperationProfileSaveResult result = new OperationProfileSaveResult();
        result.setId(entity.getId());
        result.setProfileCode(entity.getProfileCode());
        result.setAction("CREATED");
        result.setMessage("오퍼레이션 프로필이 생성되었습니다.");
        return result;
    }

    /**
     * 수정.
     *
     * <p>
     * path 의 originalProfileCode 를 기준으로 대상 찾기.
     * request.profileCode 는 변경 허용.
     * </p>
     */
    @Transactional
    public OperationProfileSaveResult update(String originalProfileCode, OperationProfileUpsertRequest request) {
        validateRequest(request);

        TbAcOperationProfile entity = operationProfileQueryService.findEntityByCode(originalProfileCode);
        if (entity == null) {
            throw new IllegalArgumentException("오퍼레이션 프로필을 찾을 수 없습니다. profileCode=" + originalProfileCode);
        }

        if (!originalProfileCode.equalsIgnoreCase(request.getProfileCode().trim())) {
            TbAcOperationProfile duplicate = operationProfileQueryService.findEntityByCode(request.getProfileCode().trim());
            if (duplicate != null) {
                throw new IllegalArgumentException("이미 존재하는 profile_code 입니다. profileCode=" + request.getProfileCode());
            }
        }

        entity.setProfileCode(request.getProfileCode().trim());
        entity.setProfileName(request.getProfileName().trim());
        entity.setIndustryType(normalizeIndustryType(request.getIndustryType()));
        entity.setDescription(normalizeText(request.getDescription()));
        entity.setActiveYn(normalizeYn(request.getActiveYn()));

        this.queryManager.update(entity);

        OperationProfileSaveResult result = new OperationProfileSaveResult();
        result.setId(entity.getId());
        result.setProfileCode(entity.getProfileCode());
        result.setAction("UPDATED");
        result.setMessage("오퍼레이션 프로필이 수정되었습니다.");
        return result;
    }

    /**
     * 삭제.
     *
     * <p>
     * 아레아가 참조 중이면 삭제 불가.
     * </p>
     */
    @Transactional
    public OperationProfileSaveResult delete(String profileCode) {
        TbAcOperationProfile entity = operationProfileQueryService.findEntityByCode(profileCode);
        if (entity == null) {
            throw new IllegalArgumentException("오퍼레이션 프로필을 찾을 수 없습니다. profileCode=" + profileCode);
        }

        long linkedAreaCount = operationProfileQueryService.countLinkedAreas(entity.getId());
        if (linkedAreaCount > 0) {
            throw new IllegalArgumentException(
                    "참조 중인 아레아가 있어 삭제할 수 없습니다. linkedAreaCount=" + linkedAreaCount
            );
        }

        this.queryManager.delete(entity);

        OperationProfileSaveResult result = new OperationProfileSaveResult();
        result.setId(entity.getId());
        result.setProfileCode(entity.getProfileCode());
        result.setAction("DELETED");
        result.setMessage("오퍼레이션 프로필이 삭제되었습니다.");
        return result;
    }

    /**
     * 요청값 검증.
     */
    private void validateRequest(OperationProfileUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 없습니다.");
        }
        if (request.getProfileCode() == null || request.getProfileCode().trim().isEmpty()) {
            throw new IllegalArgumentException("profileCode 는 필수입니다.");
        }
        if (request.getProfileName() == null || request.getProfileName().trim().isEmpty()) {
            throw new IllegalArgumentException("profileName 는 필수입니다.");
        }
        if (request.getIndustryType() == null || request.getIndustryType().trim().isEmpty()) {
            throw new IllegalArgumentException("industryType 는 필수입니다.");
        }
        if (request.getActiveYn() == null || request.getActiveYn().trim().isEmpty()) {
            throw new IllegalArgumentException("activeYn 는 필수입니다.");
        }
    }

    /**
     * 산업군 코드 정규화.
     */
    private String normalizeIndustryType(String value) {
        return AcIndustryType.fromCode(value).getCode();
    }

    /**
     * Y/N 정규화.
     */
    private String normalizeYn(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new IllegalArgumentException("activeYn 는 Y 또는 N 이어야 합니다.");
        }
        return normalized;
    }

    /**
     * 빈 문자열 -> null 정규화.
     */
    private String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}