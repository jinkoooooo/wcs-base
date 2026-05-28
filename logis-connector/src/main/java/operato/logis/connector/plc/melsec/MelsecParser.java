package operato.logis.connector.plc.melsec;

import java.util.ArrayList;
import java.util.List;

public class MelsecParser {
    private static final int RESPONSE_CODE_START = 18;
    private static final int RESPONSE_CODE_END = 22;
    private static final String SUCCESS_CODE = "0000";

    public static boolean isSuccessResponse(String response) {
        if (response == null || response.length() < RESPONSE_CODE_END) return false;
        return response.startsWith(SUCCESS_CODE, RESPONSE_CODE_START);
    }

    public static List<Integer> parseWordValues(String plcType, String response) {
        if (!isSuccessResponse(response)) throw new IllegalArgumentException("Melsec 응답 실패");
        String data = response.substring(RESPONSE_CODE_END);
        List<Integer> words = new ArrayList<>();
        switch (MelsecConsts.InterfaceType.find(plcType)){
            case ASCII -> {
                for (int i = 0; i < data.length() / 4; i++) {
                    String hex = data.substring(i * 4, i * 4 + 4);
                    words.add(Integer.parseInt(hex, 16));
                }
            }case BINARY -> {
                for (int i = 0; i < data.length() / 4; i++) {
                    String hex = data.substring(i * 4, i * 4 + 4);
                    words.add(Integer.parseInt(reverse(hex), 16));
                }
            }
        }
        return words;
    }
    public static int buildWordFromBits(int... bitsToSet) {
        int word = 0;
        for (int bit : bitsToSet) {
            word |= (1 << (bit - 1));
        }
        return word;
    }
    private static String reverse(String value){
        String reverseValue = "";
        for (int i = value.length() / 2; 0 < i; i--) {
            reverseValue += value.substring((i - 1) * 2, i * 2);
        }
        return reverseValue;
    }

    public static int convertTwoDecWordToInt(int lowWord, int highWord) {
        // lowWord = 하위 16비트 WORD
        // highWord = 상위 16비트 WORD
        return ((highWord & 0xFFFF) << 16) | (lowWord & 0xFFFF);
    }
}
