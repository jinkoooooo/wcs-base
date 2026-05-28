package operato.logis.asrs.query.item.model;

import lombok.Data;

/**
 * 상품마스터 상세 조회용 View.
 *
 * 목적:
 * - 우측 상세 편집 패널에서 사용하는 전체 필드 제공
 * - 수정 화면 진입 시 단건 상세 응답 DTO 역할
 */
@Data
public class ItemMasterDetailView {

    private String id;
    private String itemCode;
    private String itemName;

    private String itemCategoryId;
    private String categoryCode;
    private String categoryName;

    private String operationProfileId;

    private String industryType;
    private String baseUom;
    private String handlingUnitType;
    private String outboundUnitType;

    private Integer lengthMm;
    private Integer widthMm;
    private Integer heightMm;
    private Integer weightG;
    private Integer volumeMm3;

    private String storageTempType;

    private String lotControlYn;
    private String expiryControlYn;
    private String serialControlYn;
    private String partialPickYn;
    private String mixedLoadYn;
    private String fragileYn;
    private String heavyYn;
    private String quarantineRequiredYn;

    private String allocationRuleCode;
    private String rotationProfileCode;
    private String storageGradeSeed;

    private String extAttr;
    private String activeYn;

    private Long domainId;
    private String creatorId;
    private String updaterId;
    private String createdAt;
    private String updatedAt;
}