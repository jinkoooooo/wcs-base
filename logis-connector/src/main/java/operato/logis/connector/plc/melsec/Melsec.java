package operato.logis.connector.plc.melsec;

public class Melsec {
    private IMelsecFrame melsecFrame;
    public Melsec(IMelsecFrame frame){
        melsecFrame = frame;
    }

    public String ReadBit(String deviceCode, int firstDeviceCode, int deviceLength)
    {
        return melsecFrame.ReadBit(deviceCode, firstDeviceCode, deviceLength);
    }
    public String ReadWord(String deviceCode, int firstDeviceCode, int deviceLength)
    {
        return melsecFrame.ReadWord(deviceCode, firstDeviceCode, deviceLength);
    }
    public String ReadWord(String deviceCode, String firstDeviceCode, int deviceLength)
    {
        return melsecFrame.ReadWord(deviceCode, firstDeviceCode, deviceLength);
    }
    public String WriteBit(String deviceCode, int firstDeviceCode, int[] value)
    {
        return melsecFrame.WriteBit(deviceCode, firstDeviceCode, value);
    }
    public String WriteBit(String deviceCode, String firstDeviceCode, int[] value)
    {
        return melsecFrame.WriteBit(deviceCode, firstDeviceCode, value);
    }
    public String WriteWord(String deviceCode, int firstDeviceCode, int[] value)
    {
        return melsecFrame.WriteWord(deviceCode, firstDeviceCode, value);
    }
    public String WriteWord(String deviceCode, String firstDeviceCode, int[] value)
    {
        return melsecFrame.WriteWord(deviceCode, firstDeviceCode, value);
    }
}
