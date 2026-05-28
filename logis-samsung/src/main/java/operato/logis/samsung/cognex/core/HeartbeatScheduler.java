package operato.logis.samsung.cognex.core;

import jakarta.annotation.PostConstruct;
import operato.logis.samsung.WcsUtils;
import operato.logis.samsung.cognex.service.DataParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 3개 이상의 Cognex 장치 (BCR-01, BCR-02, VISION-01)의 연결 상태를 관리하고 Heartbeat를 전송합니다.
 * 다중 클라이언트 관리를 위해 SocketClient 리스트를 주입받도록 구조를 변경했습니다.
 */
@Component
public class HeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatScheduler.class);
    private static final int MAX_RETRY_COUNT = 3; // 최대 재연결 시도 횟수
    private static final long RECONNECT_INTERVAL_MS = 10000;
    private static final String ACTIVE = "0";
    private static final String INACTIVE = "99";

    // Heartbeat 주기 (1초)
    private final long HEARTBEAT_INTERVAL_MS = 1000;
    // Heartbeat 메시지는 단일 STX/ETX를 사용
    private static final String HEARTBEAT_MSG = new String(new byte[] {0x02}) + "Heartbeat" + new String(new byte[] {0x03});

    // 주입받는 모든 SocketClient 리스트
    private final List<SocketClient> allSocketClients;
    private final DataParserService dataParserService; // DB 서비스 주입

    private Thread heartbeatThread;
    private volatile boolean isRunning = false;

    // 재연결 시도 횟수와 현재 DB 상태 및 마지막 재시도 시간을 관리하는 맵
    private final ConcurrentHashMap<String, AtomicInteger> retryCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> currentDbStatus = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastRetryTime = new ConcurrentHashMap<>(); // 마지막 재시도 시간을 기록

    /**
     * Spring Configuration에서 정의된 모든 SocketClient Bean들을 주입받습니다.
     */
    public HeartbeatScheduler(
            List<SocketClient> allSocketClients,
            DataParserService dataParserService
    )
    {
        this.allSocketClients = allSocketClients;
        this.dataParserService = dataParserService;

        // 초기 상태 설정
        for (SocketClient client : allSocketClients) {
            initializeClientStatus(client.getName());
        }

        log.info("Heartbeat Scheduler 초기화 완료. 총 관리 클라이언트 수: {}", allSocketClients.size());
    }

    private void initializeClientStatus(String clientName) {
        retryCounts.put(clientName, new AtomicInteger(0));
        currentDbStatus.put(clientName, "UNKNOWN"); // 초기에는 INACTIVE로 시작
        lastRetryTime.put(clientName, 0L);
        log.info("[{}] 초기 상태 설정 완료. IP: {} PORT: {}", clientName,
                allSocketClients.stream().filter(c -> c.getName().equals(clientName)).findFirst().map(SocketClient::getIp).orElse("N/A"),
                allSocketClients.stream().filter(c -> c.getName().equals(clientName)).findFirst().map(SocketClient::getPort).orElse(0));
    }

    // PostConstruct 메서드: Bean 생성이 완료된 후 실행
    @PostConstruct
    public void initDbStatus() {
        log.info("Heartbeat Scheduler 초기 DB 상태 설정을 시작합니다.");
        try {
            for (SocketClient client : allSocketClients) {
                //dataParserService.updateHeartbeatStatus(client.getName(), INACTIVE);
                updateDbStatusIfNeeded(client.getName(), INACTIVE);
                log.info("Heartbeat 초기 DB 상태 설정 완료: {} -> 99 (INACTIVE)", client.getName());
            }
        } catch (Exception e) {
            log.error("초기 Heartbeat DB 상태 설정 중 오류 발생. 쿼리 경로 문제 확인 필요.", e);
        }
    }

    // Heartbeat 시작 (Controller에서 호출)
    @EventListener(ApplicationReadyEvent.class)
    public void startHeartbeat() {
        if (isRunning) {
            log.info("Heartbeat는 이미 실행 중입니다.");
            return;
        }

        // Heartbeat 시작 전, 모든 소켓 연결 시도
        log.info("Heartbeat 시작 전, 모든 클라이언트의 초기 연결을 시도합니다.");
        for (SocketClient client : allSocketClients) {
            client.connect();
        }

        isRunning = true;
        heartbeatThread = new Thread(() -> {
            while (isRunning) {
                sendHeartbeat();
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Heartbeat 스레드가 중단되었습니다.");
                    isRunning = false;
                }
            }
        }, "Heartbeat-Thread");

        heartbeatThread.start();
        log.info("Heartbeat 전송이 시작되었습니다. 주기: {}ms", HEARTBEAT_INTERVAL_MS);
    }

    // Heartbeat 전송 로직: 모든 클라이언트에 반복 전송
    private void sendHeartbeat() {
        for (SocketClient client : allSocketClients) {
            sendHeartbeatToClient(client, client.getName());
        }
    }

    private void sendHeartbeatToClient(SocketClient client, String clientName) {

        if (client.isConnected()) {
            // 1. 연결이 활성화된 경우: Heartbeat 전송 시도
            try {
                client.sendData(HEARTBEAT_MSG);
                log.debug("[{} Heartbeat] 전송 성공.", clientName);

                // 2. Heartbeat 성공 시: 재시도 카운터 초기화 및 DB 상태 '0'(ACTIVE)으로 업데이트
                retryCounts.get(clientName).set(0); // 카운터 초기화
                lastRetryTime.put(clientName, System.currentTimeMillis()); // 성공 시간 기록

                updateDbStatusIfNeeded(clientName, ACTIVE);

            } catch (IOException e) {
                // Heartbeat 전송 실패 = 연결이 끊어졌거나 통신 문제 발생
                handleConnectionFailure(client, clientName, e);
            }
        } else {
            // 3. 연결 자체가 끊어져 있는 경우: 재연결 시도
            handleConnectionFailure(client, clientName, null);
        }
    }

    // 연결 실패 핸들러 (영구적 복구 로직 포함)
    private void handleConnectionFailure(SocketClient client, String clientName, IOException e) {
        if (e != null) {
            log.error("[{} Heartbeat] 전송 실패. (원인: {}). 재연결 시도 로직 실행.", clientName, e.getMessage());
        }

        // 맵에서 해당 클라이언트의 상태를 가져옵니다.
        AtomicInteger currentRetryCount = retryCounts.get(clientName);
        if (currentRetryCount == null) {
            log.error("[{}] Heartbeat 카운터 맵에 존재하지 않는 클라이언트입니다.", clientName);
            return;
        }

        int currentCount = currentRetryCount.get();
        long now = System.currentTimeMillis();
        long lastAttempt = lastRetryTime.getOrDefault(clientName, 0L);

        if (currentCount < MAX_RETRY_COUNT) {
            // 1. 최대 재연결 횟수 미만: 즉시 재연결 시도 (1초 Heartbeat 주기 이용)
            currentRetryCount.incrementAndGet();
            log.warn("[{}] 연결 상태 아님. 재연결 시도 {}/{}...", clientName, currentRetryCount.get(), MAX_RETRY_COUNT);
            client.connect();

        } else if (now - lastAttempt >= RECONNECT_INTERVAL_MS) {
            // 2. 최대 재연결 횟수 초과 & 주기적 재시도 시간 도래 (10초)
            log.warn("[{}] 최대 실패 횟수 ({}) 초과. 영구 복구를 위해 재시도 카운트를 리셋하고 연결을 재시도합니다.", clientName, MAX_RETRY_COUNT);

            // 핵심 로직: 카운터 리셋 및 재연결 시도
            currentRetryCount.set(1); // 카운트를 1로 설정하고 재시도 시작
            client.connect();
            lastRetryTime.put(clientName, now); // 재연결 시도 시간 업데이트

        } else {
            // 3. 최대 재연결 횟수 초과 & 주기적 재시도 시간 미도래: 연결 시도 Skip
            log.debug("[{}] 최대 재연결 횟수 초과. 다음 주기적 재시도 시간까지 대기 중...", clientName);

            // Heartbeat가 완전히 끊긴 상태를 DB에 유지하기 위해 상태 업데이트 확인
            updateDbStatusIfNeeded(clientName, INACTIVE);
            return;
        }

        // 연결 재시도 후에도 연결이 활성화되지 않았다면 DB 상태 INACTIVE로 전환
        if (!client.isConnected()) {
            updateDbStatusIfNeeded(clientName, INACTIVE);
        }
    }

    /**
//     * 현재 메모리 상태(currentDbStatus)와 비교하여 상태가 변경될 때만
     * DataParserService를 통해 DB 업데이트를 실행합니다.
     */
    private void updateDbStatusIfNeeded(String clientName, String newStatus) {
        // 도메인 설정 (안하면 도메인 Null 에러 발생)
        WcsUtils.setupDomainContext();

        // 1. 상태 변경이 없으면 로그를 남기고 즉시 종료 (Guard Clause)
        String oldStatus = currentDbStatus.get(clientName);
        if (newStatus.equals(oldStatus)) {
            return;
        }

        log.info("[{}] 상태 변경 감지: {} -> {}", clientName, oldStatus, newStatus);

        String unitType = clientName.split("-")[0];

        // 3. DB 업데이트 및 메모리 상태 반영 로직 (트랜잭션 안전성 확보)
        try {
            // !!! 중요: 중복된 DB 호출을 제거하고, try 블록 내에 핵심 로직만 유지 !!!
            dataParserService.updateHeartbeatStatusall(clientName, newStatus);

            // DB 업데이트 성공 후, 메모리 상태를 업데이트합니다.
            currentDbStatus.put(clientName, newStatus);
            log.info("[{}] DB 및 메모리 상태 업데이트 성공: {}", clientName, newStatus);

        } catch (Exception e) {
            // DB 업데이트 실패 시, 메모리 상태를 변경하지 않고 에러 로그를 남깁니다.
            log.error("[{}] DB 상태 업데이트 중 오류 발생. 메모리 상태는 이전 값({})을 유지합니다.",
                    clientName, oldStatus, e);
        }
    }

    // Heartbeat 중지 (Controller에서 호출)
    public void stopHeartbeat() {
        if (!isRunning) {
            log.info("Heartbeat는 현재 실행 중이 아닙니다.");
            return;
        }
        isRunning = false;
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }

        // Heartbeat 중지 시 모든 소켓 연결도 안전하게 해제
        for (SocketClient client : allSocketClients) {
            client.disconnect();
        }

        log.info("Heartbeat 전송이 중지되었고, 모든 소켓 연결이 해제되었습니다.");
    }

    // 현재 Heartbeat 상태 확인
    public boolean isRunning() {
        return isRunning;
    }
}