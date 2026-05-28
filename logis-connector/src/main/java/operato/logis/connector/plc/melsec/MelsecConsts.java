package operato.logis.connector.plc.melsec;

import java.util.Arrays;

public class MelsecConsts {
    public enum InterfaceType {
        ASCII("ASCII"),
        BINARY("BINARY"),
        UNKNOWN("UNKNOWN");
        private String value;
        InterfaceType(String value)
        {
            this.value = value;
        }
        public String getValue(){
            return value;
        }
        public static InterfaceType find(String value){
            return Arrays.stream(InterfaceType.values())
                    .filter(v -> v.getValue().equals(value))
                    .findAny()
                    .orElse(InterfaceType.UNKNOWN   );
        }
    }

    public enum DeviceCode {
        M("90", "M*"),   // 내부릴레이 (10진수)
        L("92", "L*"),   // 래치릴레이 (10진수)
        F("93", "F*"),   // 어넌시에이터 (10진수)
        V("94", "V*"),   // 에지릴레이 (10진수)
        D("A8", "D*"),   // 데이터 레지스터 (10진수)
        TS("C1", "TS"),  // 타이머-접점 (10진수)
        TC("C0", "TC"),  // 타이머-코일 (10진수)
        SS("C7", "SS"),  // 적산타이머-접점 (10진수)
        SC("C6", "SC"),  // 적산타이머-코일 (10진수)
        CS("C4", "CS"),  // 카운터-접점 (10진수)
        CC("C3", "CC"),  // 카운터-코일 (10진수)
        S("98", "S*"),   // 스텝릴레이 (10진수)
        Z("CC", "Z*"),   // 인덱스레지스터 (10진수)
        R("AF", "R*"),   // 파일레지스터-일반 (10진수)

        X("9C", "X*"),   // 입력릴레이 (16진수)
        Y("9D", "Y*"),   // 출력릴레이 (16진수)
        B("A0", "B*"),   // 링크릴레이 (16진수)
        W("B4", "W*"),   // 데이터레지스터 (16진수)
        TN("C2", "TN"),  // 타이머-현재값 (16진수)
        SN("C8", "SN"),  // 적산타이머-현재값 (16진수)
        CN("C5", "CN"),  // 카운터-현재값 (16진수)
        SB("A1", "SB"),  // 링크 특수 릴레이 (16진수)
        SW("B5", "SW"),  // 링크 특수 레지스터 (16진수)
        DX("A2", "DX"),  // 다이렉트 입력 (16진수)
        DY("A3", "DY"),  // 다이렉트 출력 (16진수)
        ZR("B0", "ZR"),  // 파일레지스터-연번 (16진수)
        UNKNOWN("UNKNOWN DeviceCode", "");

        private String binaryValue;
        private String asciiValue;
        DeviceCode(String binaryValue, String asciiValue )
        {
            this.binaryValue = binaryValue;
            this.asciiValue = asciiValue;
        }
        public String getBinaryValue(){
            return binaryValue;
        }
        public String getAsciiValue(){ return asciiValue; }
        public static DeviceCode findBinary(String binaryValue){
            return Arrays.stream(DeviceCode.values())
                    .filter(v -> v.getBinaryValue().equals(binaryValue))
                    .findAny()
                    .orElse(DeviceCode.UNKNOWN   );
        }
        public static DeviceCode findAscii(String asciiValue){
            return Arrays.stream(DeviceCode.values())
                    .filter(v -> v.getAsciiValue().equals(asciiValue))
                    .findAny()
                    .orElse(DeviceCode.UNKNOWN   );
        }
    }
}
