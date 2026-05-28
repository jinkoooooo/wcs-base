package operato.logis.samsung.cognex.core;

/**
 * 소켓으로부터 완전한 메시지가 수신되었을 때 호출되는 콜백 인터페이스.
 */
public interface DataReceiver {
    void handleReceivedData(Object deviceName, String rawData);
}