package operato.logis.asrs.core.item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.request.ItemActiveToggleRequest;
import operato.logis.asrs.dto.request.ItemMasterBulkUpsertRequest;
import operato.logis.asrs.dto.request.ItemMasterUpsertRequest;
import operato.logis.asrs.dto.response.ItemMasterBulkSaveResult;
import operato.logis.asrs.dto.response.ItemMasterSaveResult;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.query.item.ItemCategoryQueryService;
import operato.logis.asrs.query.item.ItemMasterQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 상품마스터 명령 서비스.
 *
 * 구성 원칙:
 * - CRUD / active toggle / bulk upsert 를 한 서비스로 통합
 * - 과도한 파일 분할 대신, 내부 private 메서드로 검증/resolve/매핑을 분리
 *
 * 주의:
 * - queryManager.insert / update / delete 메서드 사용 기준으로 작성
 * - 프로젝트 ORM 메서드명이 다르면 그 부분만 맞춰서 조정
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ItemMasterCommandCore extends AbstractQueryService {

    private final ItemMasterQueryService itemMasterQueryService;
    private final ItemCategoryQueryService itemCategoryQueryService;

    /**
     * 상품 신규 등록.
     */
    public ItemMasterSaveResult createItemMaster(ItemMasterUpsertRequest request) {
        validateUpsertRequest(request, true);

        if (itemMasterQueryService.existsByItemCode(normalizeRequiredText(request.getItemCode(), "itemCode"))) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.DUPLICATE_DATA,
                    "이미 존재하는 상품코드입니다. itemCode=" + request.getItemCode()
            );
        }

        TbAcItemMaster entity = new TbAcItemMaster();

        // 내부 PK는 UUID 사용
        entity.setId(UUID.randomUUID().toString());

        applyRequestToEntity(entity, request, true);

        this.queryManager.insert(entity);

        return ItemMasterSaveResult.builder()
                .id(entity.getId())
                .itemCode(entity.getItemCode())
                .action("CREATED")
                .message("상품이 등록되었습니다.")
                .build();
    }

    /**
     * 상품 수정.
     *
     * 규칙:
     * - path itemCode 와 body itemCode 가 다르면 허용하지 않음
     * - 현재 구조에서는 itemCode 변경은 비허용으로 두는 것이 안전
     */
    public ItemMasterSaveResult updateItemMaster(String itemCode, ItemMasterUpsertRequest request) {
        String normalizedPathItemCode = normalizeRequiredText(itemCode, "itemCode");
        validateUpsertRequest(request, false);

        String bodyItemCode = normalizeRequiredText(request.getItemCode(), "itemCode");
        if (!normalizedPathItemCode.equals(bodyItemCode)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "수정 시 path itemCode 와 body itemCode 는 동일해야 합니다."
            );
        }

        TbAcItemMaster entity = itemMasterQueryService.findEntityByItemCodeOrThrow(normalizedPathItemCode);

        applyRequestToEntity(entity, request, false);

        this.queryManager.update(entity);

        return ItemMasterSaveResult.builder()
                .id(entity.getId())
                .itemCode(entity.getItemCode())
                .action("UPDATED")
                .message("상품이 수정되었습니다.")
                .build();
    }

    /**
     * 상품 삭제.
     *
     * 주의:
     * - 현재는 물리 삭제 기준으로 구현
     * - 운영 정책상 soft delete 가 필요하면 activeYn='N' 업데이트 방식으로 변경 가능
     */
    public void deleteItemMaster(String itemCode) {
        TbAcItemMaster entity = itemMasterQueryService.findEntityByItemCodeOrThrow(
                normalizeRequiredText(itemCode, "itemCode")
        );

        this.queryManager.delete(entity);
    }

    /**
     * 사용 여부 변경.
     */
    public ItemMasterSaveResult changeItemActiveYn(String itemCode, ItemActiveToggleRequest request) {
        TbAcItemMaster entity = itemMasterQueryService.findEntityByItemCodeOrThrow(
                normalizeRequiredText(itemCode, "itemCode")
        );

        String activeYn = normalizeYn(request.getActiveYn(), "activeYn");
        entity.setActiveYn(activeYn);

        this.queryManager.update(entity);

        return ItemMasterSaveResult.builder()
                .id(entity.getId())
                .itemCode(entity.getItemCode())
                .action("ACTIVE_CHANGED")
                .message("사용 여부가 변경되었습니다.")
                .build();
    }

    /**
     * 상품 일괄 저장.
     *
     * 규칙:
     * - itemCode 존재 시 update
     * - 미존재 시 create
     * - row 단위 오류는 누적하고 전체 처리는 계속 진행
     */
    public ItemMasterBulkSaveResult bulkUpsertItemMasters(ItemMasterBulkUpsertRequest request) {
        ItemMasterBulkSaveResult result = new ItemMasterBulkSaveResult();
        List<ItemMasterBulkSaveResult.BulkErrorRow> errors = new ArrayList<ItemMasterBulkSaveResult.BulkErrorRow>();

        List<ItemMasterUpsertRequest> rows = request != null ? request.getRows() : null;
        if (rows == null || rows.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "일괄 저장 대상 rows 가 비어 있습니다."
            );
        }

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            ItemMasterUpsertRequest row = rows.get(i);
            int rowNo = i + 1;

            try {
                String itemCode = normalizeRequiredText(row.getItemCode(), "itemCode");

                if (itemMasterQueryService.existsByItemCode(itemCode)) {
                    updateItemMaster(itemCode, row);
                } else {
                    createItemMaster(row);
                }

                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add(new ItemMasterBulkSaveResult.BulkErrorRow(
                        rowNo,
                        row != null ? row.getItemCode() : null,
                        e.getMessage()
                ));
            }
        }

        result.setTotalCount(rows.size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setErrors(errors);

        return result;
    }

    /**
     * 등록/수정 공통 검증.
     *
     * create 여부에 따라 정책이 달라질 수 있어 boolean 파라미터 유지
     */
    private void validateUpsertRequest(ItemMasterUpsertRequest request, boolean create) {
        if (request == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "요청 데이터가 없습니다."
            );
        }

        normalizeRequiredText(request.getItemCode(), "itemCode");
        normalizeRequiredText(request.getItemName(), "itemName");
        normalizeRequiredText(request.getCategoryCode(), "categoryCode");
        normalizeRequiredText(request.getIndustryType(), "industryType");
        normalizeRequiredText(request.getBaseUom(), "baseUom");
        normalizeRequiredText(request.getHandlingUnitType(), "handlingUnitType");
        normalizeRequiredText(request.getOutboundUnitType(), "outboundUnitType");
        normalizeRequiredText(request.getStorageTempType(), "storageTempType");
        normalizeRequiredText(request.getAllocationRuleCode(), "allocationRuleCode");
        normalizeRequiredText(request.getRotationProfileCode(), "rotationProfileCode");
        normalizeRequiredText(request.getStorageGradeSeed(), "storageGradeSeed");

        validatePositiveNumber(request.getLengthMm(), "lengthMm");
        validatePositiveNumber(request.getWidthMm(), "widthMm");
        validatePositiveNumber(request.getHeightMm(), "heightMm");
        validatePositiveNumber(request.getWeightG(), "weightG");

        normalizeYn(request.getLotControlYn(), "lotControlYn");
        normalizeYn(request.getExpiryControlYn(), "expiryControlYn");
        normalizeYn(request.getSerialControlYn(), "serialControlYn");
        normalizeYn(request.getPartialPickYn(), "partialPickYn");
        normalizeYn(request.getMixedLoadYn(), "mixedLoadYn");
        normalizeYn(request.getFragileYn(), "fragileYn");
        normalizeYn(request.getHeavyYn(), "heavyYn");
        normalizeYn(request.getQuarantineRequiredYn(), "quarantineRequiredYn");
        normalizeYn(request.getActiveYn(), "activeYn");
    }

    /**
     * 요청 DTO 값을 엔티티에 반영.
     *
     * create / update 공통 적용.
     */
    private void applyRequestToEntity(TbAcItemMaster entity,
                                      ItemMasterUpsertRequest request,
                                      boolean create) {

        String categoryId = resolveCategoryIdByCode(request.getCategoryCode());

        entity.setItemCode(normalizeRequiredText(request.getItemCode(), "itemCode"));
        entity.setItemName(normalizeRequiredText(request.getItemName(), "itemName"));
        entity.setItemCategoryId(categoryId);

        // 선택값은 blank 입력 시 null 저장
        entity.setOperationProfileId(normalizeNullableText(request.getOperationProfileId()));

        entity.setIndustryType(normalizeRequiredText(request.getIndustryType(), "industryType"));
        entity.setBaseUom(normalizeRequiredText(request.getBaseUom(), "baseUom"));
        entity.setHandlingUnitType(normalizeRequiredText(request.getHandlingUnitType(), "handlingUnitType"));
        entity.setOutboundUnitType(normalizeRequiredText(request.getOutboundUnitType(), "outboundUnitType"));

        entity.setLengthMm(request.getLengthMm());
        entity.setWidthMm(request.getWidthMm());
        entity.setHeightMm(request.getHeightMm());
        entity.setWeightG(request.getWeightG());
        entity.setVolumeMm3(calculateVolumeMm3(
                request.getLengthMm(),
                request.getWidthMm(),
                request.getHeightMm()
        ));

        entity.setStorageTempType(normalizeRequiredText(request.getStorageTempType(), "storageTempType"));

        entity.setLotControlYn(normalizeYn(request.getLotControlYn(), "lotControlYn"));
        entity.setExpiryControlYn(normalizeYn(request.getExpiryControlYn(), "expiryControlYn"));
        entity.setSerialControlYn(normalizeYn(request.getSerialControlYn(), "serialControlYn"));
        entity.setPartialPickYn(normalizeYn(request.getPartialPickYn(), "partialPickYn"));
        entity.setMixedLoadYn(normalizeYn(request.getMixedLoadYn(), "mixedLoadYn"));
        entity.setFragileYn(normalizeYn(request.getFragileYn(), "fragileYn"));
        entity.setHeavyYn(normalizeYn(request.getHeavyYn(), "heavyYn"));
        entity.setQuarantineRequiredYn(normalizeYn(request.getQuarantineRequiredYn(), "quarantineRequiredYn"));

        entity.setAllocationRuleCode(normalizeRequiredText(request.getAllocationRuleCode(), "allocationRuleCode"));
        entity.setRotationProfileCode(normalizeRequiredText(request.getRotationProfileCode(), "rotationProfileCode"));
        entity.setStorageGradeSeed(normalizeRequiredText(request.getStorageGradeSeed(), "storageGradeSeed"));

        entity.setExtAttr(normalizeNullableText(request.getExtAttr()));
        entity.setActiveYn(normalizeYn(request.getActiveYn(), "activeYn"));
    }

    /**
     * categoryCode -> categoryId resolve.
     */
    private String resolveCategoryIdByCode(String categoryCode) {
        String normalizedCategoryCode = normalizeRequiredText(categoryCode, "categoryCode");
        String categoryId = itemCategoryQueryService.findCategoryIdByCode(normalizedCategoryCode);

        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "카테고리를 찾을 수 없습니다. categoryCode=" + normalizedCategoryCode
            );
        }

        return categoryId;
    }

    /**
     * 체적 자동 계산.
     *
     * 주의:
     * - 서버 계산 기준으로 고정
     * - int 범위를 초과하면 INVALID_REQUEST 예외
     */
    private Integer calculateVolumeMm3(Integer lengthMm, Integer widthMm, Integer heightMm) {
        long volume = (long) lengthMm.intValue()
                * (long) widthMm.intValue()
                * (long) heightMm.intValue();

        if (volume > Integer.MAX_VALUE) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "volumeMm3 계산 결과가 int 범위를 초과했습니다."
            );
        }

        return Integer.valueOf((int) volume);
    }

    /**
     * 필수 문자열 정규화.
     */
    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    fieldName + " 는 필수입니다."
            );
        }

        return value.trim();
    }

    /**
     * nullable 문자열 정규화.
     *
     * blank 입력은 null 저장
     */
    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Y / N 값 검증 및 정규화.
     */
    private String normalizeYn(String value, String fieldName) {
        String normalized = normalizeRequiredText(value, fieldName).toUpperCase();

        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    fieldName + " 는 Y 또는 N 이어야 합니다."
            );
        }

        return normalized;
    }

    /**
     * 양수 숫자 검증.
     */
    private void validatePositiveNumber(Integer value, String fieldName) {
        if (value == null || value.intValue() <= 0) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    fieldName + " 는 1 이상이어야 합니다."
            );
        }
    }
}