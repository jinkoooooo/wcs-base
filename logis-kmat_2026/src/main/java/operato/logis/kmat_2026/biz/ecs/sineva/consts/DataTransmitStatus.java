package operato.logis.kmat_2026.biz.ecs.sineva.consts;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.EnumCode;

/**
 * 데이터 전송 상태
 */
public enum DataTransmitStatus implements EnumCode<Integer> {

    UNKNOWN(0, "알 수 없음"),
    CREATED(10, "생성"),
    SENT(20, "발신"),
    RECEIVED(30, "수신"),
    NG(90, "실패");

    private final Integer code;
    private final String desc;

    DataTransmitStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }

    public static DataTransmitStatus fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(DataTransmitStatus.class, code, false, null);
    }

    public static DataTransmitStatus fromCode(Object code) {
        DataTransmitStatus result = fromCodeOrNull(code);
        return result != null ? result : UNKNOWN;
    }

    public static DataTransmitStatus fromValueOrNull(Object value) {
        return fromCodeOrNull(value);
    }

    public static DataTransmitStatus fromValue(Object value) {
        return fromCode(value);
    }
}