package operato.logis.wcs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * WCS 다중 스레드 스케줄러 설정.
 *
 * @EnableScheduling 기본 단일 스레드 스케줄러는 1초 주기 EcsCommandSender 콜백이 쌓이면
 * 다른 @Scheduled 작업(재고 배치, 포트 재조정 등)을 모두 밀어버린다.
 * poolSize=20 의 ThreadPoolTaskScheduler 로 교체하고 빈 이름 "taskScheduler" 로 노출해
 * EcsCommandSender 의 @Autowired TaskScheduler 가 코드 변경 없이 멀티스레드로 주입받게 한다.
 * configureTasks() 에서 동일 풀을 @Scheduled 실행기로 등록한다.
 */
@Configuration
@EnableScheduling
public class WcsSchedulingConfig implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WcsSchedulingConfig.class);

    /**
     * WCS 전용 스케줄링 스레드 풀.
     *
     * 빈 이름 "taskScheduler" 는 Spring 의 스케줄링 인프라와 EcsCommandSender 모두가
     * 타입+이름 기반으로 자동 주입받는 약속된 이름이다.
     */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(20);
        scheduler.setThreadNamePrefix("wcs-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        // 스케줄 실행 중 예외가 발생해도 다음 사이클이 취소되지 않도록 처리
        scheduler.setErrorHandler(t ->
                logger.error("[ Config ][ Scheduler ] scheduled task failed, next cycle continues", t));

        scheduler.initialize();

        logger.info("[ Config ][ Scheduler ] scheduler initialized - poolSize={}", 20);
        return scheduler;
    }

    /**
     * @Scheduled 어노테이션이 위 ThreadPoolTaskScheduler 풀을 사용하도록 등록한다.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}
