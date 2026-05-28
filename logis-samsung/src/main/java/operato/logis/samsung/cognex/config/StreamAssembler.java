package operato.logis.samsung.cognex.config;


import operato.logis.samsung.cognex.core.DataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 버퍼를 관리하고, STX/ETX를 기준으로 메시지를 조립하여
 * 완전한 메시지가 완성되면 DataReceiver로 전달하는 클래스입니다.
 * SocketClient의 이름(deviceName) 대신 SocketClient 객체 자체를 Context로 사용합니다.
 */
public class StreamAssembler {

    private static final Logger log = LoggerFactory.getLogger(StreamAssembler.class);
    private static final byte STX = 0x02; // Start of Text
    private static final byte ETX = 0x03; // End of Text

    // 클라이언트의 Context를 저장 (SocketClient 인스턴스가 될 것입니다)
    private final Object clientContext;
    private final DataReceiver dataReceiver;

    // 메시지 조립용 버퍼 (최대 4KB)
    private final ByteBuffer buffer = ByteBuffer.allocate(4096);

    /**
     * 생성자. 클라이언트 컨텍스트와 데이터 수신 핸들러를 주입받습니다.
     * @param clientContext 데이터를 보내는 SocketClient 인스턴스 (또는 그 이름 등)
     * @param dataReceiver 메시지를 처리할 콜백 객체
     */
    public StreamAssembler(Object clientContext, DataReceiver dataReceiver) {
        this.clientContext = clientContext;
        this.dataReceiver = dataReceiver;
    }

    /**
     * SocketClient가 수신한 Raw 데이터를 버퍼에 추가합니다.
     */
    public void append(byte[] data, int offset, int length) {
        // 버퍼가 가득 차면 에러 처리 (여기서는 단순하게 로그만 남기고 무시)
        if (buffer.remaining() < length) {
            log.error("버퍼 공간 부족! 수신된 {} 바이트를 처리할 수 없습니다.", length);
            return;
        }
        buffer.put(data, offset, length);
        processBuffer();
    }

    /**
     * 버퍼를 검사하여 STX/ETX로 캡슐화된 메시지를 추출하고 DataReceiver로 전달합니다.
     */
    private void processBuffer() {
        // 쓰기 모드에서 읽기 모드로 전환 (position=0, limit=데이터 끝)
        buffer.flip();

        int processedBytesStart = 0; // 버퍼에서 이미 처리된 바이트의 시작점

        try {
            while (true) {
                // STX 검색: 현재까지 처리된 지점(processedBytesStart)부터 찾습니다.
                int stxIndex = -1;
                for (int i = processedBytesStart; i < buffer.limit(); i++) {
                    if (buffer.get(i) == STX) {
                        stxIndex = i;
                        break;
                    }
                }

                if (stxIndex == -1) {
                    // STX가 발견되지 않은 경우, 잔여 데이터는 모두 미완성 데이터로 간주
                    break;
                }

                // STX 이전에 불필요한 데이터가 있었다면 건너킵니다.
                if (stxIndex > processedBytesStart) {
                    log.warn("STX 이전에 불필요한 데이터 발견 ({} 바이트). 데이터를 건너킵니다.", stxIndex - processedBytesStart);
                    processedBytesStart = stxIndex;
                }

                // ETX 검색: STX 다음 위치부터 찾습니다.
                int etxIndex = -1;
                for (int i = stxIndex + 1; i < buffer.limit(); i++) {
                    if (buffer.get(i) == ETX) {
                        etxIndex = i;
                        break;
                    }
                }

                // 완전한 메시지가 아직 조립되지 않은 경우 (ETX가 없음)
                if (etxIndex == -1) {
                    // 잔여 데이터를 STX부터 보존하도록 processedBytesStart를 STX 위치로 설정
                    processedBytesStart = stxIndex;
                    break;
                }

                // ----------------- 완전한 메시지 추출 -----------------
                int messageLength = etxIndex - stxIndex + 1;
                byte[] messageBytes = new byte[messageLength];

                // 추출할 메시지의 시작점(STX)으로 position을 임시 이동
                buffer.position(stxIndex);
                buffer.get(messageBytes, 0, messageLength); // 메시지 추출

                // 처리된 메시지의 끝(ETX+1)으로 processedBytesStart를 이동
                processedBytesStart = etxIndex + 1;

                // 문자열로 변환하여 DataReceiver에 전달
                String completeMessage = new String(messageBytes, StandardCharsets.US_ASCII);

                // **** 핵심 수정 부분: clientContext를 함께 전달합니다. ****
                dataReceiver.handleReceivedData(clientContext, completeMessage);
                // *************************************************

                // 다음 메시지 검색은 새로운 processedBytesStart부터 시작합니다.
            }
        } finally {
            // 5. 처리되지 않은 잔여 데이터 이동 (COMPACT)

            // 잔여 데이터의 시작점(processedBytesStart)으로 position을 설정
            buffer.position(processedBytesStart);

            // compact()를 호출하여 position부터 limit까지의 데이터를 버퍼 시작으로 복사하고
            // position을 복사된 데이터 끝으로, limit을 capacity로 설정하여 쓰기 모드로 전환
            buffer.compact();
        }
    }
}