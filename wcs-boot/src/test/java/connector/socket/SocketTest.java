package connector.socket;

import operato.logis.connector.plc.melsec.Melsec;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecQ3E;
import operato.logis.connector.socket.sync.SocketClient;
import operato.logis.connector.socket.sync.SocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class SocketTest {
    SocketClient client;
    SocketServer server;

    @Test
    void 동기소켓_멜섹_ASCII_통신_테스트() throws Exception {
        SocketClient client = new SocketClient("192.168.3.40",5000);
        Melsec melsecAscii = new Melsec(new MelsecQ3E(MelsecConsts.InterfaceType.ASCII));
        int firstDeviceCode = 0;
        while(true)
        {
            Thread.sleep(1000);
            try {
                 client.connect();

                 // String hexString = melsecAscii.ReadWord(MelsecConsts.DeviceCode.R.getValue(), 1, 1);
                 // String hexString = melsecAscii.WriteWord(MelsecConsts.DeviceCode.R.getValue(), 1, new int [] {9});
                 String hexString = melsecAscii.ReadBit(MelsecConsts.DeviceCode.M.getAsciiValue(), firstDeviceCode, 8);
                 // String hexString = melsecAscii.WriteBit(MelsecConsts.DeviceCode.M.getValue(), 20, new int [] {1,0,1,0,1,1,1,1});

                System.out.println(hexString.trim());
                 byte[] byteArrayAscii = hexString.getBytes(StandardCharsets.UTF_8);
                 client.send(byteArrayAscii);

                 var readStr = client.readByte2Utf8();
                 System.out.println(readStr);
                 // 18번쨰자리부터 4자리는 응답코드임 "0000" - 정상
                 if(readStr.substring(18,22).equals("0000")){
                     System.out.println("success");
                     String readmessage = readStr.substring(22);

                     System.out.println("read bit  : " + readmessage);

                     List<Integer> w1 = new ArrayList<>();
                     for (int i=0; i<readmessage.length()/4 ; i++){
                         String readWord = readmessage.substring(i*4, (i*4)+4);
                         w1.add(Integer.parseInt(readWord, 16));

                         System.out.println("read word "+(firstDeviceCode+i)+"번지 : " + w1.get(i));
                     }
                 }else{
                     System.out.println("fail");
                 }
                break;

            }catch (Exception e){
                System.out.println(e);
                break;
            }
        }
        client.disconnet();
    }

    @Test
    void 동기소켓_멜섹_BINARY_통신_테스트() throws Exception {
        SocketClient client = new SocketClient("192.168.14.102",3000);
        Melsec melsecBinary = new Melsec(new MelsecQ3E(MelsecConsts.InterfaceType.BINARY));
        Melsec melsecAscii = new Melsec(new MelsecQ3E(MelsecConsts.InterfaceType.ASCII));

        int firstDeviceCode = 0;
        while(true)
        {
            Thread.sleep(1000);
            try {
                client.connect();
                // String binary = melsecBinary.ReadWord(MelsecConsts.DeviceCode.W.getBinaryValue(), 200 , 200);
                String binary = melsecBinary.WriteWord(MelsecConsts.DeviceCode.W.getBinaryValue(),
                        "20C", new int [] {buildWordFromBits(1,3,5,7,9,11,13,15)});
                // String binary = melsecBinary.ReadWord(MelsecConsts.DeviceCode.W.getValue(), "20B", 1);
                // String binary = melsecBinary.ReadWord(MelsecConsts.DeviceCode.W.getValue(), firstDeviceCode , 50);
                // String binary = melsecBinary.WriteBit(MelsecConsts.DeviceCode.W.getValue(), 200, new int [] {1,0,0,0,0,0,0,0});


                System.out.println(binary);
                byte[] byteArray = HexFormat.of().parseHex(binary);
                client.send(byteArray);

                var readStr = client.readByte2HexString();
                System.out.println(readStr);

                // 18번쨰자리부터 4자리는 응답코드 "0000" - 정상
                if(readStr.substring(18,22).equals("0000")){
                    System.out.println("success");
                    String readmessage = readStr.substring(22);

                    System.out.println("read bit  : " + readmessage);
                    List<Integer> w1 = new ArrayList<>();
                     for (int i=0; i<readmessage.length()/4 ; i++){
                         String readWord = readmessage.substring(i*4, (i*4)+4);
                         w1.add(Integer.parseInt(reverse(readWord), 16));
                         System.out.println((firstDeviceCode+i)+"번지 : " + w1.get(i));

//                         for (int bit = 0; bit <= 15; bit++) {
//                             boolean isOn = isSet(w1.get(i), bit);
//                             System.out.println("Bit " + bit + ": " + isOn);
//                         }


                     }
                    var decode = decodePlcWordsToAscii(w1);
                     System.out.println(decode);
                     System.out.println(decode.replaceAll("[\\x00-\\x1F]", ""));

                }else{
                    System.out.println("fail");
                }

                break;

            }catch (Exception e){
                System.out.println(e);
                break;
            }
        }
        client.disconnet();
    }

    public static String decodePlcWordsToAscii(List<Integer> wordData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int word : wordData) {
            // Little-endian: 하위 바이트 먼저, 상위 바이트 나중
            baos.write(word & 0xFF);        // 하위 바이트
            baos.write((word >> 8) & 0xFF); // 상위 바이트
        }

        byte[] byteArray = baos.toByteArray();
        return new String(byteArray, StandardCharsets.US_ASCII);
        // new String(byteArray, StandardCharsets.US_ASCII).replaceAll("[\\x00-\\x1F]", "");
    }
    public static int buildWordFromBits(int... bitsToSet) {
        int word = 0;
        for (int bit : bitsToSet) {
            word |= (1 << (bit - 1));
        }
        return word;
    }
    private boolean isSet(int wordValue, int getBitIndex) {
        return ((wordValue >> getBitIndex) & 1) == 1;
    }
    private static String reverse(String value){
        String reverseValue = "";
        for (int i = value.length() / 2; 0 < i; i--) {
            reverseValue += value.substring((i - 1) * 2, i * 2);
        }
        return reverseValue;
    }
    @AfterEach
    void tearDown() throws Exception {
        // 테스트 끝난 뒤 소켓 안전하게 닫기
        if (client != null) client.disconnet();
    }
}
