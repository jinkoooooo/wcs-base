package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 상품 카테고리 선택 옵션 모델.
 */
@Data
public class ItemCategoryOptionView {

    private String id;
    private String categoryCode;
    private String categoryName;
    private String activeYn;
}