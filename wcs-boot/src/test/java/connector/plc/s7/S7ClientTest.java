package connector.plc.s7;

import operato.logis.connector.plc.s7.S7Client;
import operato.logis.connector.plc.s7.S7Config;
import operato.logis.connector.plc.s7.exception.S7Exception;
import operato.logis.connector.plc.s7.type.AreaType;
import operato.logis.connector.plc.s7.type.ConnectionType;
import operato.logis.connector.plc.s7.type.DataType;
import operato.logis.connector.plc.s7.util.S7;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class S7ClientTest {

    private static final Logger log = LoggerFactory.getLogger(S7ClientTest.class);
    private S7Client client;
    private S7Config config;

    private String ip = "127.0.0.1";
    private int rack = 0;
    private int slot = 1;

    void init(){
        client = new S7Client();
        config = new S7Config();
        config.setType(ConnectionType.OP);
        config.setHost(ip);
        config.setRack(rack);
        config.setSlot(slot);
        client.setConfig(config);
    }

    @Test
    void 클라이언트_연결(){
        init();
        boolean isConnected = false;
        try {
            isConnected = client.connect();
        }
        catch (Exception e){
            System.out.println(e);
        }
        System.out.println("hhh");
        Assert.assertTrue(isConnected);
        System.out.println("Connected to   : " + ip + " (Rack=" + rack + ", Slot=" + slot + ")");
        System.out.println("PDU negotiated : " + client.getPduLength() + " bytes");

        client.disconnect();
    }

    @Test
    void 데이터_읽기(){
        byte[] buffer = new byte[50];
        try {
            int result = client.readArea(AreaType.DB, 1, 0, 50, DataType.BYTE,  buffer);
            Assert.assertEquals(0, result); // result != 0 에러
            System.out.println(result);

            S7.getDIntAt(buffer, 0);        // DataType = int, offset = 0
            S7.getS7StringAt(buffer, 6);    // DataType = String, offset = 6

        } catch (Exception e) {
            System.out.println(e);
        }
    }
    @Test
    void 데이터_쓰기(){
        byte[] buffer = new byte[50];

        S7.setBitAt(buffer, 0, 0, true);
        S7.setDIntAt(buffer, 2, 1500);
        S7.setStringAt(buffer, 6, "Hello World");

        try {
            boolean result = client.writeArea(AreaType.DB, 1, 0, 50, DataType.BYTE, buffer);
            Assert.assertTrue(result);
        } catch (S7Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // 테스트 끝난 뒤 소켓 안전하게 닫기
        if (client != null) client.disconnect();
    }

}