package operato.logis.changwon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;// ThreadPoolConfig.java
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import xyz.elidom.sys.entity.Domain;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static Domain _currentDomain;

    @Bean(name = "wcsTaskExecutor")
    public Executor wcsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 기본 10개 스레드로 운영
        executor.setMaxPoolSize(20);  // 최대 20개까지 확장
        executor.setQueueCapacity(500); // 큐에 500개까지 대기 가능
        executor.setThreadNamePrefix("WCS-Task-");
        executor.initialize();
        return executor;
    }

    public static Domain getCurrentDomain() {
        return _currentDomain;
    }

    public static void setCurrentDomain(Domain currentDomain) {
        _currentDomain = currentDomain;
    }
}