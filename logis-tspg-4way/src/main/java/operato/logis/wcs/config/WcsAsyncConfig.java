package operato.logis.wcs.config;

import operato.logis.wcs.common.service.audit.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * WCS 비동기 스레드 풀 설정 (스레드 풀 격리).
 *
 * WMS/ERP 등 외부 I/O 작업(HostCallbackPublisher)을 WCS 핵심 트랜잭션 스레드에서 격리해
 * "외부 시스템 네트워크 지연 -> WCS DB 커넥션 풀 고갈" 장애를 차단한다.
 *
 * 풀 파라미터: core=10(상시), max=50(스파이크 상한), queue=200(버퍼), keepAlive=60s.
 * 거부 정책 CallerRuns: 큐+MaxPool 초과 시 호출 스레드가 직접 실행 — 요청 유실 없이 back-pressure 전파.
 */
@Configuration
@EnableAsync
public class WcsAsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(WcsAsyncConfig.class);

    /**
     * WMS/외부 I/O 전용 비동기 스레드 풀.
     * 사용처: HostCallbackPublisher.notify*() — WMS HTTP/MQ 콜백.
     */
    @Bean(name = "wcsCallbackExecutor")
    public ThreadPoolTaskExecutor wcsCallbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("wcs-callback-");

        // 호출 스레드의 감사 행위자를 작업 스레드로 전파 (작업 종료 후 이전 값 복원)
        executor.setTaskDecorator(runnable -> {
            ActorContext.Actor captured = ActorContext.get();
            return () -> {
                ActorContext.Actor previous = ActorContext.get();
                if (captured != null) ActorContext.set(captured);
                try {
                    runnable.run();
                } finally {
                    if (previous != null) ActorContext.set(previous);
                    else ActorContext.clear();
                }
            };
        });

        // 큐 + MaxPool 초과 시: 호출 스레드가 직접 실행 (요청 유실 방지 + Back-pressure)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 애플리케이션 종료 시 진행 중인 I/O 콜백 완료 후 셧다운
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        logger.info("[ Config ][ Async ] executor initialized - core={}, max={}, queue={}", 10, 50, 200);
        return executor;
    }
}
