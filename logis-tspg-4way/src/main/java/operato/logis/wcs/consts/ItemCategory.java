package operato.logis.wcs.consts;

/**
 * 자재 분류. QC 부적합 후처리 분기 키로 사용된다.
 *
 * 원료/자재 → 반품, 제품/자사제품 → 폐기, 반제품 → 운영자 선택.
 * 값이 비어 있으면 운영자 선택으로 폴백.
 */
public enum ItemCategory implements EnumCode {

    RAW_MATERIAL("RAW_MATERIAL", "원료"),
    MATERIAL("MATERIAL",         "자재"),
    SEMI_PRODUCT("SEMI_PRODUCT", "반제품"),
    PRODUCT("PRODUCT",           "제품"),
    OWN_PRODUCT("OWN_PRODUCT",   "자사제품");

    private final String code;
    private final String desc;

    ItemCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static ItemCategory from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(ItemCategory.class, codeOrAlias);
    }
}
