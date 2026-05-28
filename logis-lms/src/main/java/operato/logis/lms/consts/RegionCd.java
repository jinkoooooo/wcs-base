package operato.logis.lms.consts;

import java.util.HashMap;
import java.util.Map;

public enum RegionCd implements BaseEnum<String> {
    // 기본
    ETC(new EnumHelper<>("", "UNKNOWN")),

    // 광역시 / 특별시
    SEL(new EnumHelper<>("서울", "SEOUL")),
    ICN(new EnumHelper<>("인천", "INCHEON")),
    PUS(new EnumHelper<>("부산", "BUSAN")),
    TAE(new EnumHelper<>("대구", "DAEGU")),
    GWJ(new EnumHelper<>("광주", "GWANGJU")),
    DJN(new EnumHelper<>("대전", "DAEJEON")),
    USN(new EnumHelper<>("울산", "ULSAN")),
    SJG(new EnumHelper<>("세종", "SEJONG")),

    // 도
    GYG(new EnumHelper<>("경기", "GYEONGGI")),
    GWD(new EnumHelper<>("강원", "GANGWON")),
    CBK(new EnumHelper<>("충북", "CHUNGBUK")),
    CNM(new EnumHelper<>("충남", "CHUNGNAM")),
    JBK(new EnumHelper<>("전북", "JEONBUK")),
    JNM(new EnumHelper<>("전남", "JEONNAM")),
    GBK(new EnumHelper<>("경북", "GYEONGBUK")),
    GNM(new EnumHelper<>("경남", "GYEONGNAM")),
    CJU(new EnumHelper<>("제주", "JEJU"));

    private static final Map<String, RegionCd> VALUE_MAP = BaseEnum.createLookupMap(RegionCd.class);

    private final EnumHelper<String> helper;

    RegionCd(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    /**
     * 주어진 값을 기반으로 Enum을 찾는 메서드 (O(1) 조회)
     */
    public static RegionCd fromValue(String value) {
        return VALUE_MAP.getOrDefault(value, ETC);
    }

    // 주소 prefix 매핑맵
    private static final Map<String, RegionCd> PREFIX_MAP;

    static {
        Map<String, RegionCd> map = new HashMap<>(VALUE_MAP);
        map.put("충청북", CBK);
        map.put("충청남", CNM);
        map.put("전라북", CNM);
        map.put("전라남", CNM);
        map.put("경상북", GBK);
        map.put("경상남", GNM);
        PREFIX_MAP = map;
    }

    // 주소에서 지역 코드 추출
    public static RegionCd fromAddress(String address) {
        if (address == null || address.length() < 2) {
            return ETC;
        }
        // 2~3글자 prefix 검사
        for (int len = 2; len < 4; ++len) {
            if (address.length() >= len) {
                String prefix = address.substring(0, len);
                RegionCd cd = PREFIX_MAP.get(prefix);
                if (cd != null) {
                    return cd;
                }
            }
        }
        return ETC;
    }
}