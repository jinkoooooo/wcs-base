package operato.logis.wcs.scheduler;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.alarm.ReinboundAlarmBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import xyz.elidom.util.ValueUtil;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * WCS 스케줄러 통합 런처.
 *
 * 모든 주기 작업을 단일 ThreadPoolTaskScheduler 풀에서 PeriodicTrigger 로 구동한다.
 * 빈 초기화 안정화를 위해 첫 실행은 INITIAL_DELAY_MS 만큼 지연된다.
 *
 * 잡 구성:
 *   - HostOrder      (10s) — 예약일 전이 + 신규/재시도 산출 + 장애 진단
 *   - EcsRelease     ( 5s) — ECS 송신 핵심 (최단 주기 유지)
 *   - GaPlanning     (10s) — GA 점수 산출 (계산 무거움)
 *   - Maintenance    (10s) — 포트 모드 전환 + Ghost 보정 + 재고 재배치 (사이클 카운터로 내부 빈도 분기)
 *   - ReinboundAlarm ( 5s) — 재입고 대기 알람 STOMP 브로드캐스트 (있을 때만)
 */
@Component
@RequiredArgsConstructor
public class WcsJobLauncher {

    private static final Logger logger = LoggerFactory.getLogger(WcsJobLauncher.class);

    private static final long INITIAL_DELAY_MS = 30_000L;

    private final HostOrderJobs hostOrderJobs;
    private final ReleaseScheduler releaseScheduler;
    private final GaPlanningJob gaPlanningJob;
    private final MaintenanceJobs maintenanceJobs;
    private final ReinboundAlarmBroadcaster reinboundAlarmBroadcaster;

    private ThreadPoolTaskScheduler pool;
    private final Map<String, ScheduledFuture<?>> tasks = new LinkedHashMap<>();

    /**
     * Application ready 이후 자동 시작.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            start();
        } catch (Exception e) {
            logger.error("[ Scheduler ][ Launcher ] onReady failed", e);
        }
    }

    /**
     * 4개 잡을 단일 풀에 등록.
     */
    public void start() {
        if (ValueUtil.isNotEmpty(tasks)) {
            logger.warn("[ Scheduler ][ Launcher ] already started - skip duplicate call");
            return;
        }
        pool = newPool();
        logger.info("[ Scheduler ][ Launcher ] starting all jobs");

        register("HostOrder",      10_000L, hostOrderJobs::runHostOrderCycle);
        register("EcsRelease",      5_000L, releaseScheduler::runOrchestrationCycle);
        register("GaPlanning",     10_000L, gaPlanningJob::runPlanningCycle);
        register("Maintenance",    10_000L, maintenanceJobs::runMaintenanceCycle);
        register("ReinboundAlarm",  5_000L, reinboundAlarmBroadcaster::broadcastTick);

        logger.info("[ Scheduler ][ Launcher ] registered - count={}", tasks.size());
    }

    /**
     * 컨테이너 종료 시 모든 잡 취소 + 풀 셧다운.
     */
    @PreDestroy
    public void stop() {
        if (ValueUtil.isEmpty(tasks)) return;
        logger.info("[ Scheduler ][ Launcher ] stopping jobs - count={}", tasks.size());

        tasks.forEach((name, future) -> {
            if (ValueUtil.isNotEmpty(future) && !future.isCancelled()) {
                future.cancel(false);
                logger.info("[ Scheduler ][ Launcher ] stopped - {}", name);
            }
        });
        tasks.clear();

        if (ValueUtil.isNotEmpty(pool)) {
            pool.shutdown();
            pool = null;
        }
        logger.info("[ Scheduler ][ Launcher ] all jobs stopped");
    }

    /**
     * 풀 5 스레드 — 잡 5개 동시 진행 보장.
     */
    private ThreadPoolTaskScheduler newPool() {
        ThreadPoolTaskScheduler p = new ThreadPoolTaskScheduler();
        p.setPoolSize(5);
        p.setThreadNamePrefix("wcs-master-job-");
        p.setWaitForTasksToCompleteOnShutdown(true);
        p.setAwaitTerminationSeconds(10);
        p.initialize();
        return p;
    }

    /**
     * 잡 등록 — 예외 가드된 Runnable + PeriodicTrigger.
     */
    private void register(String name, long periodMs, Runnable job) {
        Runnable guarded = () -> {
            try {
                job.run();
            } catch (Exception e) {
                logger.error("[ Scheduler ][ Launcher ] job failed - name={}", name, e);
            }
        };

        PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(periodMs));
        trigger.setInitialDelay(Duration.ofMillis(INITIAL_DELAY_MS));

        tasks.put(name, pool.schedule(guarded, trigger));
        logger.info("[ Scheduler ][ Launcher ] registered - name={}, periodMs={}", name, periodMs);
    }
}
