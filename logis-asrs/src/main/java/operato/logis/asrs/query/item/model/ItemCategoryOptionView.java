package operato.logis.asrs.query.item.model;

import lombok.Data;

/**
 * 상품 카테고리 선택 옵션 View.
 *
 * 목적:
 * - 상품 등록/수정 화면의 카테고리 콤보박스 데이터 제공
 * - active_yn = 'Y' 인 카테고리만 내려주는 용도
 */
@Data
public class ItemCategoryOptionView {

    /** 카테고리 ID */
    private String id;

    /** 카테고리 코드 */
    private String categoryCode;

    /** 카테고리명 */
    private String categoryName;

    /** 기본 운영 프로파일 ID */
    private String defaultOperationProfileId;

    /** 설명 */
    private String description;

    /** 사용 여부 */
    private String activeYn;
}