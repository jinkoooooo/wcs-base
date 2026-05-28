package operato.logis.connector.plc.melsec;

public interface IMelsecFrame {
    String ReadBit(String deviceCode, int firstDeviceCode, int deviceCount);
    String ReadWord(String deviceCode, int firstDeviceCode, int deviceCount);
    String ReadWord(String deviceCode, String firstDeviceCode, int deviceCount);
    String WriteBit(String deviceCode, int firstDeviceCode, int[] value);
    String WriteBit(String deviceCode, String firstDeviceCode, int[] value);
    String WriteWord(String deviceCode, int firstDeviceCode, int[] value);
    String WriteWord(String deviceCode, String firstDeviceCode, int[] value);
}
