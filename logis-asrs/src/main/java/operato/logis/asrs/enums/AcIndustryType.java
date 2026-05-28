package operato.logis.asrs.enums;

/**
 * 운영 프로필 산업군 코드.
 *
 * <p>
 * 운영 프로필이 어느 산업군/센터 유형을 대상으로 하는지 구분한다.
 * </p>
 */
public enum AcIndustryType {

    FOOD("FOOD", "식품 일반"),
    FROZEN_FOOD("FROZEN_FOOD", "냉동식품"),
    CHILLED_FOOD("CHILLED_FOOD", "냉장식품"),
    AMBIENT_FOOD("AMBIENT_FOOD", "상온식품"),
    GENERAL("GENERAL", "범용 / 기타"),
    BEVERAGE("BEVERAGE", "음료"),
    FRESH("FRESH", "신선식품"),
    MEAT("MEAT", "축산 / 육가공"),
    DAIRY("DAIRY", "유제품"),
    PHARMACEUTICAL("PHARMACEUTICAL", "의약 / 의약외품"),
    COSMETIC("COSMETIC", "화장품"),
    APPAREL("APPAREL", "의류"),
    E_COMMERCE("E_COMMERCE", "이커머스 범용");

    private final String code;
    private final String description;

    AcIndustryType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * DB 저장 코드 반환.
     */
    public String getCode() {
        return code;
    }

    /**
     * 화면 표시용 설명 반환.
     */
    public String getDescription() {
        return description;
    }

    /**
     * 문자열 코드로 enum 조회.
     */
    public static AcIndustryType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Industry type is empty.");
        }

        for (AcIndustryType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported industry type code: " + code);
    }
}