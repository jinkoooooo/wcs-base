package app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayReadConsts;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import operato.logis.ecs.tspg4way.service.TspgShuttlePlcReadService;
import xyz.anyware.wcs.AnywareWcsTspg4wayTestApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringBootTest(
        classes = AnywareWcsTspg4wayTestApplication.class
)
// logis-tspg-4way
public class Shuttle4wayServiceTest {


    private TspgShuttlePlcReadService service = new TspgShuttlePlcReadService();

    @Test
    void 컨텍스트_로드(){
        service.doSomething();
    }
    @Test
    void 셔틀_리드_바이너리_체크(){
        // String readStr = "D00000FFFF03002E010000010001000400F203000000000000000001000200000000000200010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008000D1AC00000000000000000000000000000000D1AC010000000000000000000000000000000000D1AC01000000000000000000000000000000000024000000F106000000000000000000000000000000000000000000000000000000000000000000000000";
        // String readStr = "D000 00FF FF03 002E 01 0000 0100 0200 0000 F203 0000 0000 0000 0000 0100 0200 0000 0000 0100 3E3D00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008000D1AC00000000000000000000000000000000D1AC010000000000000000000000000000000000D1AC01000000000000000000000000000000000024000000F106000000000000000000000000000000000000000000000000000000000000000000000000";
        String readStr = "D00000FFFF03002E010000060002000800F4030000000000000000010002000200000001008C2700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008000D1AC00000000000000000000000000000000D1AC010000000000000000000000000000000000D1AC01000000000000000000000000000000000024000000F106000000000000000000000000000000000000000000000000000000000000000000000000";

        if (MelsecParser.isSuccessResponse(readStr)) {
            List<Integer> wordValues = MelsecParser.parseWordValues(MelsecConsts.InterfaceType.BINARY.getValue(), readStr);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                System.out.println(objectMapper.writeValueAsString(wordValues));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            service.logInfo(801, wordValues);

        } else {
            Assertions.fail();
        }
    }
    public static String getBarcodeFromTwoWords(int word1, int word2) {
        return decodeWordToString(word1) + decodeWordToString(word2);
    }
    public static String decodeWordToString(int wordValue) {
        // WORD = 2바이트
        char high = (char) ((wordValue >> 8) & 0xFF);  // 상위 바이트
        char low  = (char) (wordValue & 0xFF);         // 하위 바이트

        StringBuilder sb = new StringBuilder();

        if (high != 0) sb.append(high);
        if (low  != 0) sb.append(low);

        return sb.toString();
    }
    public static int combineBarcode(int lowWord, int highWord) {
        return ((highWord & 0xFFFF) << 16) | (lowWord & 0xFFFF);
    }
    @Test
    void 셔틀_리드_테스트(){
        int firstDeviceCode = 800;
        List<Integer> wordValues = new ArrayList<>(Collections.nCopies(200, 0));

        // MODE (1번 주소) → 1: 자동
        wordValues.set(Shuttle4WayReadConsts.ShuttleReadAddress.MODE.getAddress() - firstDeviceCode, 1);
        // WORKING_STATUS (2번 주소) → bit0 = 1 (작업 중)
        wordValues.set(Shuttle4WayReadConsts.ShuttleReadAddress.WORKING_STATUS.getAddress() - firstDeviceCode, 1);
        // STATUS (3번 주소) → 0: RUN
        wordValues.set(Shuttle4WayReadConsts.ShuttleReadAddress.RUN_STATUS.getAddress() - firstDeviceCode, 0);
        // WORK_ID (5번 주소) → 123
        wordValues.set(Shuttle4WayReadConsts.ShuttleReadAddress.WORK_ID.getAddress() - firstDeviceCode, 123);
        // INTER_LOCK (6번 주소) → bit0 = 1 (인터록 발생)
        wordValues.set(Shuttle4WayReadConsts.ShuttleReadAddress.INTER_LOCK.getAddress() - firstDeviceCode, 1);
        // CHARGE_STATUS (7번 주소) → bit0 = 1 (충전중)
        wordValues.set(Shuttle4WayReadConsts.ShuttleReadAddress.CHARGE_STATUS.getAddress() - firstDeviceCode, 1);
        // ... 필요하면 다른 값도 set

        // CENTER_STATUS (100번 주소) → 비트 0, 2, 8 세트
        int centerStatusWord = 0b0000_0000_0000_0101; // CHARGER_FAULT + EMERGENCY_STOP
        wordValues.set(Shuttle4WayReadConsts.ShuttleReadAddress.CENSER_STATUS.getAddress() - firstDeviceCode, centerStatusWord);

        service.logInfo(firstDeviceCode, wordValues);
    }
}
