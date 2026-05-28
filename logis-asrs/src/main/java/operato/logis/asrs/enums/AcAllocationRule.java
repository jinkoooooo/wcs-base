package operato.logis.asrs.enums;

/**
 * 출고 할당 규칙 공통 코드.
 *
 * <p>
 * 상품, 상품군, 운영 프로파일 정책에 따라 재고를 어떤 순서로 선택할지 정의한다.
 * </p>
 */
public enum AcAllocationRule {

    /** 선입선출 */
    FIFO,

    /** 유통기한 임박 순 출고 */
    FEFO,

    /** 지정 위치/고정 정책 우선 */
    FIXED,

    /** 상품 등급 우선 */
    GRADE_FIRST;

    /**
     * 문자열을 할당 규칙 Enum 으로 변환한다.
     *
     * <p>
     * 미지정 시 1차 기본값은 FIFO 로 본다.
     * </p>
     *
     * @param value 입력 문자열
     * @return AcAllocationRule
     */
    public static AcAllocationRule from(String value) {
        if (value == null || value.isBlank()) {
            return FIFO;
        }

        for (AcAllocationRule rule : values()) {
            if (rule.name().equalsIgnoreCase(value.trim())) {
                return rule;
            }
        }

        throw new IllegalArgumentException("Invalid allocation rule: " + value);
    }
}