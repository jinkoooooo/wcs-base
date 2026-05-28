package operato.logis.samsung.cognex.core;

import operato.logis.samsung.cognex.config.StreamAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;


/**
 * ECS와의 TCP/IP 연결을 담당하는 클라이언트 클래스입니다.
 * 연결, 데이터 송수신, 안정적인 해제 로직을 포함하며, 수신 데이터 처리는 DataReceiver에 위임합니다.
 */
public class SocketClient {

    private static final Logger log = LoggerFactory.getLogger(SocketClient.class);

    private final String name;
    private final String ip;
    private final int port;
    private final DataReceiver dataReceiver; // DataParserService가 이 인터페이스를 구현합니다.

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private Thread receiverThread;

    /**
     * 클라이언트 초기화.
     */
    public SocketClient(String name, String ip, int port, DataReceiver dataReceiver) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.dataReceiver = dataReceiver;
    }

    // public getter 추가 (HeartbeatScheduler에서 사용)
    public String getName() {
        return name;
    }

    // public getter 추가 (HeartbeatScheduler에서 사용)
    public String getIp() {
        return ip;
    }

    // public getter 추가 (HeartbeatScheduler에서 사용)
    public int getPort() {
        return port;
    }

    /**
     * 서버에 연결을 시도합니다. 연결 성공 시 수신 스레드를 시작합니다.
     */
    public void connect() {
        if (isConnected()) {
            return;
        }
        // 연결 재시도 전에 기존 자원 안전하게 정리
        disconnect();

        try {
            log.info("[{}] 연결 시도: {}:{}", name, ip, port);
            // new Socket(ip, port)는 내부적으로 연결 타임아웃을 사용합니다.
            this.socket = new Socket(ip, port);
            this.out = socket.getOutputStream();
            this.in = socket.getInputStream();
            log.info("[{}] 성공적으로 연결되었습니다.", name);

            // 연결 성공 후 데이터 수신 스레드 시작
            startReceiver();

        } catch (IOException e) {
            log.error("[{}] 연결 실패. IP:{}, PORT:{}. Heartbeat Scheduler에 의해 재시도될 예정. 메시지: {}", name, ip, port, e.getMessage());
            // 연결 실패 시에도 자원 정리가 필요하므로 disconnect() 호출
            disconnect();
            // 연결 실패 예외는 Heartbeat Scheduler가 처리하므로 여기서 다시 던지지 않습니다.
        }
    }

    // 데이터 수신 스레드 관리
    private void startReceiver() {
        if (receiverThread != null && receiverThread.isAlive()) {
            return;
        }

        receiverThread = new Thread(this::receiveLoop, name + "-Receiver-Thread");
        receiverThread.start();
        // ⭐️ 디버깅: 수신 스레드 시작 요청 완료 로그 추가
        log.info("[{}] 데이터 수신 스레드 ({}) 시작 요청 완료.", name, receiverThread.getName());
    }

    // 데이터 수신 루프 (Blocking Read)
    private void receiveLoop() {
        // ⭐️ 디버깅: 스레드 내부 진입 확인 로그 추가
        log.info("[{}] 데이터 수신 스레드가 실행 중입니다.", name);

        byte[] buffer = new byte[4096];
        int bytesRead;

        // DataReceiver와 이 클라이언트의 이름을 함께 전달하여 메시지 조립 및 처리를 위임
        // StreamAssembler가 DataReceiver.handleReceivedData(deviceName, rawData)를 호출한다고 가정
        StreamAssembler assembler = (dataReceiver != null) ? new StreamAssembler(name, dataReceiver) : null;

        // ⭐️ 디버깅: DataReceiver가 null일 경우 에러 로그 출력 및 종료 로직
        if (assembler == null) {
            log.error("[{}] StreamAssembler 초기화 실패: DataReceiver가 null입니다. 데이터 수신 처리가 불가능합니다.", name);
            return;
        }

        try {
            // isConnected()와 Thread.isInterrupted()를 사용하여 소켓 연결 상태 및 스레드 상태 확인
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                // ⭐️ 디버깅: read() 호출 직전 로그 추가 (데이터 수신 대기 상태 확인)
                log.trace("[{}] Blocking read() 호출 대기 중...", name);

                bytesRead = in.read(buffer);

                // ⭐️ 디버깅: read() 호출 후 결과 로그 추가 (실제 바이트 수신 확인)
                log.debug("[{}] read() 결과: {} bytes (0이면 루프 계속, -1이면 서버 종료).", name, bytesRead);

                if (bytesRead == -1) {
                    log.warn("[{}] 서버가 연결을 정상적으로 종료했습니다(EOF). 수신 루프 종료.", name);
                    break;
                }

                if (bytesRead > 0) {
                    // assembler는 위에서 null 체크를 했으므로 바로 사용
                    assembler.append(buffer, 0, bytesRead);
                }
            }
        } catch (SocketException e) {
            // Connection reset은 서버 측 강제 종료이므로 오류 기록 후 재연결 필요
            if ("Connection reset".equalsIgnoreCase(e.getMessage())) {
                log.error("[{}] 서버 연결 강제 종료 (Connection reset). Heartbeat Scheduler가 재연결 시도.", name);
            } else {
                log.error("[{}] 소켓 예외 발생. 연결 해제.", name, e);
            }
        } catch (IOException e) {
            if (isConnected()) {
                log.error("[{}] 데이터 수신 중 IO 오류 발생. 연결을 해제합니다.", name, e);
            }
        } finally {
            // ⭐️ 디버깅: 스레드 종료 로그 추가
            log.info("[{}] 데이터 수신 스레드가 종료됩니다.", name);
            // 수신 루프가 종료되거나 예외 발생 시 연결 해제 루틴 호출
            disconnect();
        }
    }

    /**
     * Heartbeat 메시지나 요청 메시지를 서버로 전송합니다.
     * @param message 전송할 메시지 문자열
     * @throws IOException 연결이 끊어져 전송에 실패했을 때
     */
    public void sendData(String message) throws IOException {
        if (!isConnected()) {
            throw new IOException("소켓이 연결되어 있지 않거나 닫혔습니다. 전송 실패.");
        }

        try {
            // US-ASCII 인코딩을 사용하여 전송
            out.write(message.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            // ⭐️ 디버깅: 전송 완료 로그 추가
            log.debug("[{}] 데이터 전송 완료: {}", name, message);
        } catch (IOException e) {
            log.error("[{}] 데이터 전송 중 오류 발생. 연결 해제.", name, e);
            // 전송 실패는 연결 문제로 간주하고 Heartbeat Scheduler가 재연결하도록 예외를 던집니다.
            disconnect();
            throw e;
        }
    }

    /**
     * 모든 소켓 및 스레드 자원을 안전하게 해제합니다.
     */
    public synchronized void disconnect() {
        try {
            if (receiverThread != null) {
                receiverThread.interrupt();
                // 수신 스레드가 완전히 종료될 때까지 대기 (최대 1초)
                try {
                    receiverThread.join(1000);
                } catch (InterruptedException ie) {
                    // 현재 스레드의 인터럽트 상태를 다시 설정
                    Thread.currentThread().interrupt();
                }
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                log.info("[{}] 소켓 연결이 해제되었습니다.", name);
            }
        } catch (IOException e) {
            log.error("[{}] 소켓을 닫는 중 오류 발생.", name, e);
        } finally {
            // 자원 null 처리
            this.socket = null;
            this.in = null;
            this.out = null;
            this.receiverThread = null;
        }
    }

    /**
     * 현재 클라이언트가 서버에 연결되어 통신 가능한 상태인지 확인합니다.
     */
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected() && !this.socket.isClosed();
    }
}