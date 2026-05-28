package operato.logis.wcs.consts;

import java.util.List;

import xyz.elidom.util.ValueUtil;

/**
 * 수량 단위. EA 가 재고 기준 단위이며 BOX/PLT 는 입고 시 EA 로 환산한다.
 */
public enum UomType implements EnumCode {

    EA("EA",  "낱개 - 재고/출고 관리의 기준 단위"),
    BOX("BOX","박스 - 입고 시 환산 필요 (itemMaster.boxQty)"),
    PLT("PLT","팔레트 - 입고 시 환산 필요 (itemMaster.boxQty × palletQty)");

    private final String code;
    private final String desc;

    UomType(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 외부 시스템이 보내는 동의어 코드 매핑. */
    @Override public List<String> aliases() {
        return switch (this) {
            case EA  -> List.of("EACH", "PCS", "PIECE", "UNIT");
            case BOX -> List.of("CTN", "CARTON", "CASE");
            case PLT -> List.of("PALLET", "PL");
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static UomType from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(UomType.class, codeOrAlias);
    }

    /** 코드/별칭으로 enum 해석. 미일치/null 이면 EA fallback. */
    public static UomType fromOrDefault(Object codeOrAlias) {
        UomType result = from(codeOrAlias);
        return ValueUtil.isNotEmpty(result) ? result : EA;
    }
}
