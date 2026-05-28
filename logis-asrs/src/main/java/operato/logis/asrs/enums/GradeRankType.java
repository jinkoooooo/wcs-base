package operato.logis.asrs.enums;

/**
 * 상품/로케이션 등급 우선순위 랭크.
 *
 * <p>
 * A가 가장 우선이며 숫자가 작을수록 좋은 등급으로 간주한다.
 * 상품등급과 로케이션등급 비교 시 사용한다.
 * </p>
 */
public enum GradeRankType {

    A("A", 1),
    B("B", 2),
    C("C", 3),
    D("D", 4);

    private final String code;
    private final int rank;

    GradeRankType(String code, int rank) {
        this.code = code;
        this.rank = rank;
    }

    public String getCode() {
        return code;
    }

    public int getRank() {
        return rank;
    }

    /**
     * 등급 코드 → rank 반환.
     *
     * @param code 등급 코드
     * @return rank
     */
    public static int rankOf(String code) {
        if (code == null) {
            return Integer.MAX_VALUE;
        }

        for (GradeRankType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type.rank;
            }
        }
        return Integer.MAX_VALUE;
    }
}