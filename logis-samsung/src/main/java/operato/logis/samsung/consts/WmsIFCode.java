package operato.logis.samsung.consts;

//import BaseEnum;
//import EnumHelper;

import java.util.Map;

/**
 * Legacy-WCS I/F - WCS 응답 코드
 */
public enum WmsIFCode implements BaseEnum<String> {

    // 비정상
    UNKNOWN(new EnumHelper<>("-1", "UNKNOWN")),
    ERR_INVALID_FORMAT(new EnumHelper<>("1001", "잘못된 요청 포맷")), // Invalid foramt, JSON schema error
    ERR_MISSING_PARAMS(new EnumHelper<>("1002", "필수 파라미터 누락")),
    ERR_DUPLICATE_ORDER(new EnumHelper<>("1003", "동일 지시 중복")),
    ERR_UNSUPPORTED_COMMAND(new EnumHelper<>("2001", "알 수 없는 명령타입")),
    ERR_EQUIP_NOT_FOUND(new EnumHelper<>("2002", "유효하지않은 라인/설비")),
    ERR_ITEM_NOT_FOUND(new EnumHelper<>("2003", "유효하지않은 품목")),
    ERR_TIMEOUT(new EnumHelper<>("3001", "설비 응답 지연")),
    ERR_SYSTEM(new EnumHelper<>("3002", "설비 오류")), // PLC 장치 응답 불가
    // 정상
    SUCCESSFUL(new EnumHelper<>("0000", "successful"));

    private static final Map<String, WmsIFCode> VALUE_MAP = BaseEnum.createLookupMap(WmsIFCode.class);

    private final EnumHelper<String> helper;

    WmsIFCode(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    public static WmsIFCode fromValue(String value) { return VALUE_MAP.getOrDefault(value, UNKNOWN); }
}