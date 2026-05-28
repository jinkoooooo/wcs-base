package operato.logis.asrs.enums;

/**
 * Y/N 공통 코드 Enum.
 *
 * <p>
 * 현재 프로젝트의 테이블 구조가 boolean 대신 문자열 Y/N 을 사용하는 경우가 많아
 * 이를 코드 차원에서 안전하게 처리하기 위한 공통 Enum 이다.
 * </p>
 */
public enum AcYn {

    /** 예 / 사용 / 활성 / 허용 */
    Y,

    /** 아니오 / 미사용 / 비활성 / 비허용 */
    N;

    /**
     * 문자열 값을 Enum 으로 변환한다.
     *
     * <p>
     * null 또는 공백은 기본적으로 N 으로 해석한다.
     * </p>
     *
     * @param value 원본 문자열
     * @return AcYn
     */
    public static AcYn from(String value) {
        if (value == null || value.isBlank()) {
            return N;
        }
        return "Y".equalsIgnoreCase(value) ? Y : N;
    }

    /**
     * boolean 값을 Y/N 문자열 기준으로 변환한다.
     *
     * @param value boolean 값
     * @return "Y" 또는 "N"
     */
    public static String fromBoolean(boolean value) {
        return value ? Y.name() : N.name();
    }

    /**
     * 현재 값이 Y 인지 여부.
     */
    public boolean isYes() {
        return this == Y;
    }

    /**
     * 현재 값이 N 인지 여부.
     */
    public boolean isNo() {
        return this == N;
    }

    /**
     * boolean 으로 변환한다.
     */
    public boolean toBoolean() {
        return this == Y;
    }
}