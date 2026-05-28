package operato.logis.connector.plc.melsec;


public class MelsecQ3E implements IMelsecFrame{

    private MelsecConsts.InterfaceType type;
    public MelsecQ3E(MelsecConsts.InterfaceType type){
        this.type = type;
    }

    // 서브 머리글
    private final String SUB_HEADER_COMMAND = "5000";
    private final String SUB_HEADER_RESPONSE = "D000";

    // 네트워크 번호
    private final String NETWORK_NUMBER = "00";

    // PLC 번호
    private final String PLC_NUMBER = "FF";

    // I/O 번호
    private final String IO_CODE_ASCII = "03FF";
    private final String IO_CODE_BINARY = "FF03";

    // 국번호
    private final String LOCAL_CODE = "00";

    // 요구 데이터 길이
    private final String Q_DATA_DEFAULT_LENGTH_ASCII = "0018";
    private final String Q_DATA_DEFAULT_LENGTH_BINARY = "0C00";
    private static final int Q_DATA_DEFAULT_LENGTH_INTEGER_ASCII = 24;
    private static final int Q_DATA_DEFAULT_LENGTH_INTEGER_BINARY = 12;
    /** 요구 데이터 길이 (CPU 감시 타이머 ~ 요구 데이터부의 마지막)*/
    public static String Q_DATA_LENGTH_ADD(int valueLength, int defaultLength)
    {
        return String.format("%4s", Integer.toHexString(defaultLength + valueLength)).replace(" ","0");
    }

    // CPU 감시 타이머
    private final String CPU_DEFAULT_TIMER_ASCII = "0010";
    private final String CPU_DEFAULT_TIMER_BINARY = "1000";
    /** CPU 감시 타이머 (단위: 250MS)*/
    public static String CPU_TIMER(int cpuTime)
    {
        return String.format("%4s", Integer.toHexString(cpuTime)).replace(" ","0");
    }

    // 커맨드
    private final String COMMAND_READ_ASCII = "0401";
    private final String COMMAND_WRITE_ASCII = "1401";
    private final String COMMAND_READ_BINARY = "0104";
    private final String COMMAND_WRITE_BINARY = "0114";

    // 서브 커맨드
    private final String SUB_COMMAND_BIT_ASCII = "0001";
    private final String SUB_COMMAND_WORD_ASCII = "0000";
    private final String SUB_COMMAND_BIT_BINARY = "0100";
    private final String SUB_COMMAND_WORD_BINARY = "0000";


    // 바이너리 타입일때 디바이스코드가 10진수표현이면 16진수 변환 후 전송 (아스키타입 상관없음)
    public String firstDeviceCodeConvert(String deviceCode, int firstDeviceCode)
    {
        switch (MelsecConsts.DeviceCode.findBinary(deviceCode))
        {
            case    D,
                    M,
                    L,
                    F,
                    V,
                    TS,
                    TC,
                    SS,
                    SC,
                    CS,
                    CC,
                    S,
                    Z,
                    R
                    -> {
                return String.format("%6s", Integer.toHexString(firstDeviceCode)).replace(" ","0");
            }
            default -> {
                return String.format("%6s", firstDeviceCode).replace(" ","0");

            }
        }
    }

    public String firstDeviceCodeConvert(String deviceCode, String firstDeviceCode)
    {
        switch (MelsecConsts.DeviceCode.findBinary(deviceCode))
        {
            case    D,
                    M,
                    L,
                    F,
                    V,
                    TS,
                    TC,
                    SS,
                    SC,
                    CS,
                    CC,
                    S,
                    Z,
                    R
                    -> {
                return String.format("%6s", Integer.parseInt(firstDeviceCode, 16)).replace(" ","0");
            }
            default -> {
                return String.format("%6s", firstDeviceCode).replace(" ","0");

            }
        }
    }


    public String deviceCountConvert(int count)
    {
        return String.format("%4s", Integer.toHexString(count)).replace(" ","0");
    }

    private String reverse(String value){
        String reverseValue = "";
        for (int i = value.length() / 2; 0 < i; i--) {
            reverseValue += value.substring((i - 1) * 2, i * 2);
        }
        return reverseValue;
    }
    public static String rightPad(Object value, int totalLength, char paddingChar) {
        if (paddingChar == 0) {
            paddingChar = '0';
        }

        StringBuffer sb = new StringBuffer();
        sb.append(value);

        while (sb.length() < totalLength) {
            sb.insert(sb.length(), paddingChar);
        }
        return sb.toString();
    }

    @Override
    public String ReadBit(String deviceCode, int firstDeviceCode, int deviceCount) {
        String command = "";
        switch(type) {
            case ASCII -> {
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_ASCII
                        + LOCAL_CODE
                        + Q_DATA_DEFAULT_LENGTH_ASCII
                        + CPU_DEFAULT_TIMER_ASCII
                        + COMMAND_READ_ASCII
                        + SUB_COMMAND_BIT_ASCII
                        + deviceCode
                        + firstDeviceCodeConvert(deviceCode, firstDeviceCode)
                        + deviceCountConvert(deviceCount);
            }
            case BINARY -> {
                command = ""
                    + SUB_HEADER_COMMAND
                    + NETWORK_NUMBER
                    + PLC_NUMBER
                    + IO_CODE_BINARY
                    + LOCAL_CODE
                    + Q_DATA_DEFAULT_LENGTH_BINARY
                    + CPU_DEFAULT_TIMER_BINARY
                    + COMMAND_READ_BINARY
                    + SUB_COMMAND_BIT_BINARY
                    + reverse(firstDeviceCodeConvert(deviceCode, firstDeviceCode))
                    + deviceCode
                    + reverse(deviceCountConvert(deviceCount));
            }
        }
        return command;
    }
    @Override
    public String ReadWord(String deviceCode, int firstDeviceCode, int deviceCount) {
        String command = "";
        switch(type) {
            case ASCII -> {
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_ASCII
                        + LOCAL_CODE
                        + Q_DATA_DEFAULT_LENGTH_ASCII
                        + CPU_DEFAULT_TIMER_ASCII
                        + COMMAND_READ_ASCII
                        + SUB_COMMAND_WORD_ASCII
                        + deviceCode
                        + firstDeviceCodeConvert(deviceCode, firstDeviceCode)
                        + deviceCountConvert(deviceCount);
            }
            case BINARY -> {
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_BINARY
                        + LOCAL_CODE
                        + Q_DATA_DEFAULT_LENGTH_BINARY
                        + CPU_DEFAULT_TIMER_BINARY
                        + COMMAND_READ_BINARY
                        + SUB_COMMAND_WORD_BINARY
                        + reverse(firstDeviceCodeConvert(deviceCode, firstDeviceCode))
                        + deviceCode
                        + reverse(deviceCountConvert(deviceCount));
            }
        }
        return command;

    }


    @Override
    public String ReadWord(String deviceCode, String firstDeviceCode, int deviceCount) {
        String command = "";
        switch(type) {
            case ASCII -> {
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_ASCII
                        + LOCAL_CODE
                        + Q_DATA_DEFAULT_LENGTH_ASCII
                        + CPU_DEFAULT_TIMER_ASCII
                        + COMMAND_READ_ASCII
                        + SUB_COMMAND_WORD_ASCII
                        + deviceCode
                        + firstDeviceCodeConvert(deviceCode, firstDeviceCode)
                        + deviceCountConvert(deviceCount);
            }
            case BINARY -> {
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_BINARY
                        + LOCAL_CODE
                        + Q_DATA_DEFAULT_LENGTH_BINARY
                        + CPU_DEFAULT_TIMER_BINARY
                        + COMMAND_READ_BINARY
                        + SUB_COMMAND_WORD_BINARY
                        + reverse(firstDeviceCodeConvert(deviceCode, firstDeviceCode))
                        + deviceCode
                        + reverse(deviceCountConvert(deviceCount));
            }
        }
        return command;

    }




    @Override
    public String WriteBit(String deviceCode, int firstDeviceCode, int[] value) {
        String command = "";
        switch(type) {
            case ASCII -> {
                StringBuilder deviceValue = new StringBuilder();
                for (int v : value) {
                    deviceValue.append(Integer.toHexString(v));
                }

                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_ASCII
                        + LOCAL_CODE
                        + Q_DATA_LENGTH_ADD(value.length, Q_DATA_DEFAULT_LENGTH_INTEGER_ASCII)
                        + CPU_DEFAULT_TIMER_ASCII
                        + COMMAND_WRITE_ASCII
                        + SUB_COMMAND_BIT_ASCII
                        + deviceCode
                        + firstDeviceCodeConvert(deviceCode, firstDeviceCode)
                        + deviceCountConvert(value.length)
                        + deviceValue;
            }
            case BINARY -> {
                StringBuilder deviceValue = new StringBuilder();
                //전송할 데이터 Binary 자릿수를 맞추기 위해 길이 변환
                for (int j : value) {
                    deviceValue.append(String.valueOf(j));
                }
                //전송할 전체 길이를 데이터를 포함하여 계산
                String writeData = rightPad(deviceValue.toString(), (value.length+1)/2, '0');
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_BINARY
                        + LOCAL_CODE
                        + reverse(Q_DATA_LENGTH_ADD(((value.length+1)/2), Q_DATA_DEFAULT_LENGTH_INTEGER_BINARY))
                        + CPU_DEFAULT_TIMER_BINARY
                        + COMMAND_WRITE_BINARY
                        + SUB_COMMAND_BIT_BINARY
                        + reverse(firstDeviceCodeConvert(deviceCode, firstDeviceCode))
                        + deviceCode
                        + String.format("%02X%02X", value.length & 0xFF, (value.length >> 8) & 0xFF)
                        + writeData;
            }
        }
        return command;
    }
    @Override
    public String WriteBit(String deviceCode, String firstDeviceCode, int[] value) {
        String command = "";
        switch(type) {
            case ASCII -> {
                StringBuilder deviceValue = new StringBuilder();
                for (int v : value) {
                    deviceValue.append(Integer.toHexString(v));
                }

                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_ASCII
                        + LOCAL_CODE
                        + Q_DATA_LENGTH_ADD(value.length, Q_DATA_DEFAULT_LENGTH_INTEGER_ASCII)
                        + CPU_DEFAULT_TIMER_ASCII
                        + COMMAND_WRITE_ASCII
                        + SUB_COMMAND_BIT_ASCII
                        + deviceCode
                        + firstDeviceCodeConvert(deviceCode, firstDeviceCode)
                        + deviceCountConvert(value.length)
                        + deviceValue;
            }
            case BINARY -> {
                StringBuilder deviceValue = new StringBuilder();
                //전송할 데이터 Binary 자릿수를 맞추기 위해 길이 변환
                for (int j : value) {
                    deviceValue.append(String.valueOf(j));
                }
                //전송할 전체 길이를 데이터를 포함하여 계산
                String writeData = rightPad(deviceValue.toString(), (value.length+1)/2, '0');
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_BINARY
                        + LOCAL_CODE
                        + reverse(Q_DATA_LENGTH_ADD(((value.length+1)/2), Q_DATA_DEFAULT_LENGTH_INTEGER_BINARY))
                        + CPU_DEFAULT_TIMER_BINARY
                        + COMMAND_WRITE_BINARY
                        + SUB_COMMAND_BIT_BINARY
                        + reverse(firstDeviceCodeConvert(deviceCode, firstDeviceCode))
                        + deviceCode
                        + String.format("%02X%02X", value.length & 0xFF, (value.length >> 8) & 0xFF)
                        + writeData;
            }
        }
        return command;
    }
    @Override
    public String WriteWord(String deviceCode, int firstDeviceCode, int[] value) {
        String command = "";
        switch(type) {
            case ASCII -> {
                String deviceValue = "";
                for (int i = 0; i < value.length; i++)
                {
                    deviceValue += String.format("%4s" ,Integer.toHexString(value[i])).replace(" ","0");
                }
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_ASCII
                        + LOCAL_CODE
                        + Q_DATA_LENGTH_ADD(4 * value.length, Q_DATA_DEFAULT_LENGTH_INTEGER_ASCII)
                        + CPU_DEFAULT_TIMER_ASCII
                        + COMMAND_WRITE_ASCII
                        + SUB_COMMAND_WORD_ASCII
                        + deviceCode
                        + firstDeviceCodeConvert(deviceCode, firstDeviceCode)
                        + deviceCountConvert(value.length)
                        + deviceValue;
            }
            case BINARY -> {
                String deviceReverseValue = "";
                for (int i = 0; i < value.length; i++)
                {
                    deviceReverseValue += reverse(String.format("%4s" ,Integer.toHexString(value[i])).replace(" ","0"));
                }
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_BINARY
                        + LOCAL_CODE
                        + reverse(Q_DATA_LENGTH_ADD(2 * value.length , Q_DATA_DEFAULT_LENGTH_INTEGER_BINARY))
                        + CPU_DEFAULT_TIMER_BINARY
                        + COMMAND_WRITE_BINARY
                        + SUB_COMMAND_WORD_BINARY
                        + reverse(firstDeviceCodeConvert(deviceCode, firstDeviceCode))
                        + deviceCode
                        + reverse(deviceCountConvert(value.length))
                        + deviceReverseValue;
            }
        }
        return command;
    }
    @Override
    public String WriteWord(String deviceCode, String firstDeviceCode, int[] value) {
        String command = "";
        switch(type) {
            case ASCII -> {
                String deviceValue = "";
                for (int i = 0; i < value.length; i++)
                {
                    deviceValue += String.format("%4s" ,Integer.toHexString(value[i])).replace(" ","0");
                }
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_ASCII
                        + LOCAL_CODE
                        + Q_DATA_LENGTH_ADD(4 * value.length, Q_DATA_DEFAULT_LENGTH_INTEGER_ASCII)
                        + CPU_DEFAULT_TIMER_ASCII
                        + COMMAND_WRITE_ASCII
                        + SUB_COMMAND_WORD_ASCII
                        + deviceCode
                        + firstDeviceCodeConvert(deviceCode, firstDeviceCode)
                        + deviceCountConvert(value.length)
                        + deviceValue;
            }
            case BINARY -> {
                String deviceReverseValue = "";
                for (int i = 0; i < value.length; i++)
                {
                    deviceReverseValue += reverse(String.format("%4s" ,Integer.toHexString(value[i])).replace(" ","0"));
                }
                command = ""
                        + SUB_HEADER_COMMAND
                        + NETWORK_NUMBER
                        + PLC_NUMBER
                        + IO_CODE_BINARY
                        + LOCAL_CODE
                        + reverse(Q_DATA_LENGTH_ADD(2 * value.length , Q_DATA_DEFAULT_LENGTH_INTEGER_BINARY))
                        + CPU_DEFAULT_TIMER_BINARY
                        + COMMAND_WRITE_BINARY
                        + SUB_COMMAND_WORD_BINARY
                        + reverse(firstDeviceCodeConvert(deviceCode, firstDeviceCode))
                        + deviceCode
                        + reverse(deviceCountConvert(value.length))
                        + deviceReverseValue;
            }
        }
        return command;
    }
}
