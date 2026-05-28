package operato.logis.lms.consts;

import java.util.Map;

public enum EncryptionDomain implements BaseEnum<String> {
    ADDRESS(new EnumHelper<>("address", "주소 암호화")),
    EDGE_SERVER(new EnumHelper<>("edgeServer", "엣지서버 암호화")),
    PERSONAL_INFO(new EnumHelper<>("personalInfo", "개인정보 암호화"));

    private static final Map<String, SupportCategory> VALUE_MAP = BaseEnum.createLookupMap(SupportCategory.class);
    private final EnumHelper<String> helper;

    EncryptionDomain(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    @Override
    public String getValue() { return helper.getValue(); }
}