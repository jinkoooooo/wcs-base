package operato.logis.asrs.query.item.model;

import lombok.Data;

/**
 * 상품마스터 목록 조회용 View.
 *
 * 목적:
 * - 좌측 상품 목록 그리드에 필요한 컬럼만 우선 제공
 * - 상세 편집 전 목록 성능을 위해 핵심 컬럼 위주로 구성
 */
@Data
public class ItemMasterListView {

    /** 내부 PK */
    private String id;

    /** 상품 코드 */
    private String itemCode;

    /** 상품명 */
    private String itemName;

    /** 카테고리 ID */
    private String itemCategoryId;

    /** 카테고리 코드 */
    private String categoryCode;

    /** 카테고리명 */
    private String categoryName;

    /** 운영 프로파일 ID */
    private String operationProfileId;

    /** 업종 타입 */
    private String industryType;

    /** 기본 단위 */
    private String baseUom;

    /** 취급 단위 */
    private String handlingUnitType;

    /** 출고 단위 */
    private String outboundUnitType;

    /** 보관 온도 타입 */
    private String storageTempType;

    /** Lot 관리 여부 */
    private String lotControlYn;

    /** 유통기한 관리 여부 */
    private String expiryControlYn;

    /** 시리얼 관리 여부 */
    private String serialControlYn;

    /** 사용 여부 */
    private String activeYn;

    /** 최종 수정 시각 */
    private String updatedAt;
}