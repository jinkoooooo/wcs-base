package xyz.elidom.dev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Value("${scheduler.pool.size:10}")
    private int poolSize;

    @Value("${scheduler.shutdown.wait:true}")
    private boolean waitForTasksToComplete;

    @Value("${scheduler.shutdown.await-seconds:60}")
    private int awaitTerminationSeconds;

    @Bean
    @Primary
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("dynamic-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(waitForTasksToComplete);
        scheduler.setAwaitTerminationSeconds(awaitTerminationSeconds);
        scheduler.initialize();
        return scheduler;
    }
}