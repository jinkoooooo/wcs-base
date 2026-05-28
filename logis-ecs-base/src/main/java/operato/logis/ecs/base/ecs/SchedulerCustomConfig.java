package operato.logis.ecs.base.ecs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerCustomConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(30);
        threadPoolTaskScheduler.setThreadNamePrefix("SysScheduler-");

        // 셧다운 시 안전 종료 옵션 추가
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);

        // 셧다운 시 30초 waiting 후 강제 종료
        threadPoolTaskScheduler.setAwaitTerminationSeconds(30);

        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }
}