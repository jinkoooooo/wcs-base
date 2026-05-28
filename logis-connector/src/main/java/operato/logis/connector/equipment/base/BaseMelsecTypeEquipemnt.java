package operato.logis.connector.equipment.base;

import lombok.Getter;
import operato.logis.connector.plc.melsec.Melsec;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecQ3E;
import operato.logis.connector.socket.sync.SocketClient;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Getter
public abstract class BaseMelsecTypeEquipemnt {
    private String id;
    private SocketClient clientRead;
    private SocketClient clientSend;
    private Melsec plc;
    private String plcType;

    public BaseMelsecTypeEquipemnt(String id, String ip, int readPort, int sendPort, MelsecConsts.InterfaceType plcType) throws Exception {
        try {
            this.id = id;
            clientRead = new SocketClient(ip, readPort);
            clientSend = new SocketClient(ip, sendPort);
            plc = new Melsec(new MelsecQ3E(plcType));
            this.plcType = plcType.getValue();
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][init] Error", e);
        }
    }
    public void start() throws Exception {
        try {
            clientRead.connect();
            clientSend.connect();
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][start] Error", e);
        }
    }
    public void reStart() throws Exception {
        try {
            clientRead.reconnect();
            clientSend.reconnect();
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][start] Error", e);
        }
    }



    public void stop() throws Exception {
        try {
            clientRead.disconnet();
            clientSend.disconnet();
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][stop] Error", e);
        }
    }
    public String readWord(MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int length) throws Exception {
        try {
            validateConnection();

            byte[] requestBytes = buildReadRequest(deviceCode, firstDeviceCode, length);
            sendRequest(clientRead, requestBytes);

            String response = readResponse(clientRead);

            return response;
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][readWord] Error", e);
        }
    }
    public byte[] readWordGetByte(MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int length) throws Exception {
        try {
            validateConnection();

            byte[] requestBytes = buildReadRequest(deviceCode, firstDeviceCode, length);
            sendRequest(clientRead, requestBytes);

            byte[] response = readResponseRaw(clientRead);

            return response;
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][readWord] Error", e);
        }
    }
    public String writeWord(MelsecConsts.DeviceCode deviceCode, String firstDeviceCode, int[] value) throws Exception {
        try {
            validateConnection();

            byte[] requestBytes = WriteWord(deviceCode, firstDeviceCode, value);
            sendRequest(clientSend, requestBytes);

            String response = readResponse(clientSend);

            return response;
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][readWord] Error", e);
        }
    }
    public String writeWord(MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int[] value) throws Exception {
        try {
            validateConnection();

            byte[] requestBytes = WriteWord(deviceCode, firstDeviceCode, value);
            sendRequest(clientSend, requestBytes);

            String response = readResponse(clientSend);

            return response;
        }catch (Exception e){
            throw new Exception("[BaseMelsecTypeEquipemnt][readWord] Error", e);
        }
    }

    public boolean isReady() {
        return plc != null && clientSend != null && clientSend.isAvailable()
                && clientRead != null  && clientRead.isAvailable();
    }
    private void validateConnection() {
        if (!isReady()) {
            throw new IllegalStateException("BaseMelsecTypeEquipemnt is not ready: Not initialized or not connected.");
        }
    }

    private byte[] buildReadRequest(MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int length) {
        switch (MelsecConsts.InterfaceType.find((plcType)))
        {
            case ASCII -> {
                String hexString = plc.ReadWord(deviceCode.getAsciiValue(), firstDeviceCode, length);
                return hexString.getBytes(StandardCharsets.UTF_8);
            }
            case BINARY -> {
                String binary = plc.ReadWord(deviceCode.getBinaryValue(), firstDeviceCode, length);
                return HexFormat.of().parseHex(binary);
            }
        }
        throw new IllegalStateException("BaseMelsecTypeEquipemnt plcType error.");
    }

    private byte[] WriteWord(MelsecConsts.DeviceCode deviceCode, String firstDeviceCode, int[] value){
        switch (MelsecConsts.InterfaceType.find((plcType)))
        {
            case ASCII -> {
                String hexString = plc.WriteWord(deviceCode.getAsciiValue(), firstDeviceCode, value);
                return hexString.getBytes(StandardCharsets.UTF_8);
            }
            case BINARY -> {
                String binary = plc.WriteWord(deviceCode.getBinaryValue(), firstDeviceCode, value);
                return HexFormat.of().parseHex(binary);
            }
        }
        throw new IllegalStateException("BaseMelsecTypeEquipemnt plcType error.");
    }

    private byte[] WriteWord(MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int[] value){
        switch (MelsecConsts.InterfaceType.find((plcType)))
        {
            case ASCII -> {
                String hexString = plc.WriteWord(deviceCode.getAsciiValue(), firstDeviceCode, value);
                return hexString.getBytes(StandardCharsets.UTF_8);
            }
            case BINARY -> {
                String binary = plc.WriteWord(deviceCode.getBinaryValue(), firstDeviceCode, value);
                return HexFormat.of().parseHex(binary);
            }
        }
        throw new IllegalStateException("BaseMelsecTypeEquipemnt plcType error.");
    }



    private void sendRequest(SocketClient client, byte[] request) throws Exception {
        client.send(request);
    }

    private String readResponse(SocketClient client) throws Exception {
        String readResponse = "";
        switch (MelsecConsts.InterfaceType.find((plcType)))
        {
            case ASCII -> readResponse =  client.readByte2Utf8();
            case BINARY -> readResponse =  client.readByte2HexString();
        }
        return readResponse;
    }

    private byte[] readResponseRaw(SocketClient client) throws Exception {
        return client.readRawBytes();
    }

}
