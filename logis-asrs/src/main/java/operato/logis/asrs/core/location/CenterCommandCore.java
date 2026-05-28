package operato.logis.asrs.core.location;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.dto.request.CenterUpsertRequest;
import operato.logis.asrs.dto.response.CenterSaveResult;
import operato.logis.asrs.entity.TbAcCenter;
import operato.logis.asrs.enums.AcCenterType;
import operato.logis.asrs.query.location.CenterQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 센터 명령 Core.
 */
@Service
public class CenterCommandCore extends AbstractQueryService {

    private final CenterQueryService centerQueryService;

    public CenterCommandCore(CenterQueryService centerQueryService) {
        this.centerQueryService = centerQueryService;
    }

    /**
     * 신규 생성.
     */
    @Transactional
    public CenterSaveResult create(CenterUpsertRequest request) {
        validateRequest(request);

        TbAcCenter duplicate = centerQueryService.findEntityByCode(request.getCenterCode().trim());
        if (duplicate != null) {
            throw new IllegalArgumentException("이미 존재하는 center_code 입니다. centerCode=" + request.getCenterCode());
        }

        TbAcCenter entity = new TbAcCenter();
        entity.setCenterCode(request.getCenterCode().trim());
        entity.setCenterName(request.getCenterName().trim());
        entity.setCenterType(normalizeCenterType(request.getCenterType()));
        entity.setTimezone(request.getTimezone().trim());
        entity.setDescription(normalizeText(request.getDescription()));
        entity.setActiveYn(normalizeYn(request.getActiveYn()));

        this.queryManager.insert(entity);

        CenterSaveResult result = new CenterSaveResult();
        result.setId(entity.getId());
        result.setCenterCode(entity.getCenterCode());
        result.setAction("CREATED");
        result.setMessage("센터가 생성되었습니다.");
        return result;
    }

    /**
     * 수정.
     */
    @Transactional
    public CenterSaveResult update(String originalCenterCode, CenterUpsertRequest request) {
        validateRequest(request);

        TbAcCenter entity = centerQueryService.findEntityByCode(originalCenterCode);
        if (entity == null) {
            throw new IllegalArgumentException("센터를 찾을 수 없습니다. centerCode=" + originalCenterCode);
        }

        if (!originalCenterCode.equalsIgnoreCase(request.getCenterCode().trim())) {
            TbAcCenter duplicate = centerQueryService.findEntityByCode(request.getCenterCode().trim());
            if (duplicate != null) {
                throw new IllegalArgumentException("이미 존재하는 center_code 입니다. centerCode=" + request.getCenterCode());
            }
        }

        entity.setCenterCode(request.getCenterCode().trim());
        entity.setCenterName(request.getCenterName().trim());
        entity.setCenterType(normalizeCenterType(request.getCenterType()));
        entity.setTimezone(request.getTimezone().trim());
        entity.setDescription(normalizeText(request.getDescription()));
        entity.setActiveYn(normalizeYn(request.getActiveYn()));

        this.queryManager.update(entity);

        CenterSaveResult result = new CenterSaveResult();
        result.setId(entity.getId());
        result.setCenterCode(entity.getCenterCode());
        result.setAction("UPDATED");
        result.setMessage("센터가 수정되었습니다.");
        return result;
    }

    /**
     * 삭제.
     *
     * <p>
     * 참조 중인 아레아가 있으면 삭제 불가.
     * </p>
     */
    @Transactional
    public CenterSaveResult delete(String centerCode) {
        TbAcCenter entity = centerQueryService.findEntityByCode(centerCode);
        if (entity == null) {
            throw new IllegalArgumentException("센터를 찾을 수 없습니다. centerCode=" + centerCode);
        }

        long linkedAreaCount = centerQueryService.countLinkedAreas(entity.getId());
        if (linkedAreaCount > 0) {
            throw new IllegalArgumentException(
                    "참조 중인 아레아가 있어 삭제할 수 없습니다. linkedAreaCount=" + linkedAreaCount
            );
        }

        this.queryManager.delete(entity);

        CenterSaveResult result = new CenterSaveResult();
        result.setId(entity.getId());
        result.setCenterCode(entity.getCenterCode());
        result.setAction("DELETED");
        result.setMessage("센터가 삭제되었습니다.");
        return result;
    }

    /**
     * 요청 검증.
     */
    private void validateRequest(CenterUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 없습니다.");
        }
        if (request.getCenterCode() == null || request.getCenterCode().trim().isEmpty()) {
            throw new IllegalArgumentException("centerCode 는 필수입니다.");
        }
        if (request.getCenterName() == null || request.getCenterName().trim().isEmpty()) {
            throw new IllegalArgumentException("centerName 는 필수입니다.");
        }
        if (request.getCenterType() == null || request.getCenterType().trim().isEmpty()) {
            throw new IllegalArgumentException("centerType 는 필수입니다.");
        }
        if (request.getTimezone() == null || request.getTimezone().trim().isEmpty()) {
            throw new IllegalArgumentException("timezone 는 필수입니다.");
        }
        if (request.getActiveYn() == null || request.getActiveYn().trim().isEmpty()) {
            throw new IllegalArgumentException("activeYn 는 필수입니다.");
        }
    }

    /**
     * 센터 유형 정규화.
     */
    private String normalizeCenterType(String value) {
        return AcCenterType.fromCode(value).getCode();
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