package operato.logis.kmat_2026.consts;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EnumCodeUtil;

import java.util.List;

public enum LocTypeEnumCode implements EnumCode {

    ROUTE("ROUTE", "주행 경로"),
    CROSS("CROSS", "횡 주행 경로"),
    FOUR_WAY("FOUR_WAY", "4WAY 주행 경로"),
    RACK("RACK", "Rack 적재 로케이션"),
    OUTBOUND("OUTBOUND", "출고 로케이션"),
    INBOUND("INBOUND", "입고 로케이션");

    private final String code;
    private final String desc;

    LocTypeEnumCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Object code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }

    public List<String> aliases() {
        return List.of();
    }

    public static LocTypeEnumCode from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(
                LocTypeEnumCode.class,
                codeOrAlias,
                true,
                LocTypeEnumCode::aliases
        );
    }
}