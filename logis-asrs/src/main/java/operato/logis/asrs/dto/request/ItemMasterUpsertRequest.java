package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 상품마스터 등록/수정 요청 DTO.
 *
 * 규칙:
 * - 외부 입력은 categoryCode 기준으로 받음
 * - 내부 저장 시 categoryCode -> itemCategoryId resolve
 * - volumeMm3는 입력받지 않고 서버에서 계산
 *
 * 추가 정책:
 * - 프론트/외부 연동에서 camelCase, snake_case 둘 다 수용
 */
@Data
public class ItemMasterUpsertRequest {

    @JsonAlias({"itemCode", "item_code"})
    private String itemCode;

    @JsonAlias({"itemName", "item_name"})
    private String itemName;

    /** FK 입력은 code 기준으로 받음 */
    @JsonAlias({"categoryCode", "category_code"})
    private String categoryCode;

    /** 현재는 ID 기준 저장 */
    @JsonAlias({"operationProfileId", "operation_profile_id"})
    private String operationProfileId;

    @JsonAlias({"industryType", "industry_type"})
    private String industryType;

    @JsonAlias({"baseUom", "base_uom"})
    private String baseUom;

    @JsonAlias({"handlingUnitType", "handling_unit_type"})
    private String handlingUnitType;

    @JsonAlias({"outboundUnitType", "outbound_unit_type"})
    private String outboundUnitType;

    @JsonAlias({"lengthMm", "length_mm"})
    private Integer lengthMm;

    @JsonAlias({"widthMm", "width_mm"})
    private Integer widthMm;

    @JsonAlias({"heightMm", "height_mm"})
    private Integer heightMm;

    @JsonAlias({"weightG", "weight_g"})
    private Integer weightG;

    @JsonAlias({"storageTempType", "storage_temp_type"})
    private String storageTempType;

    @JsonAlias({"lotControlYn", "lot_control_yn"})
    private String lotControlYn;

    @JsonAlias({"expiryControlYn", "expiry_control_yn"})
    private String expiryControlYn;

    @JsonAlias({"serialControlYn", "serial_control_yn"})
    private String serialControlYn;

    @JsonAlias({"partialPickYn", "partial_pick_yn"})
    private String partialPickYn;

    @JsonAlias({"mixedLoadYn", "mixed_load_yn"})
    private String mixedLoadYn;

    @JsonAlias({"fragileYn", "fragile_yn"})
    private String fragileYn;

    @JsonAlias({"heavyYn", "heavy_yn"})
    private String heavyYn;

    @JsonAlias({"quarantineRequiredYn", "quarantine_required_yn"})
    private String quarantineRequiredYn;

    @JsonAlias({"allocationRuleCode", "allocation_rule_code"})
    private String allocationRuleCode;

    @JsonAlias({"rotationProfileCode", "rotation_profile_code"})
    private String rotationProfileCode;

    @JsonAlias({"storageGradeSeed", "storage_grade_seed"})
    private String storageGradeSeed;

    @JsonAlias({"extAttr", "ext_attr"})
    private String extAttr;

    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}