package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

/**
 * 공통 코드 인터페이스
 * - code + desc는 무조건 존재해야 한다.
 * - code는 String/Int 혼재가 가능하므로 Object로 받되, 편의 메서드를 제공한다.
 */
public interface EnumCode {
    Object code();
    String desc();

    default String codeAsString() {
        Object c = code();
        return c == null ? null : String.valueOf(c);
    }

    default Integer codeAsIntOrNull() {
        Object c = code();
        if (c == null) return null;
        if (c instanceof Integer) return (Integer) c;
        try {
            return Integer.parseInt(String.valueOf(c));
        } catch (Exception e) {
            return null;
        }
    }
}