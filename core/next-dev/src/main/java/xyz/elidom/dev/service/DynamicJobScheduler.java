package xyz.elidom.dev.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import xyz.elidom.dev.consts.ScheduleType;
import xyz.elidom.dev.entity.JobDefinition;
import xyz.elidom.dev.repository.JobRepository;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicJobScheduler {

    private final JobRepository jobRepository;
    private final TaskScheduler taskScheduler; // Spring Boot의 ThreadPoolTaskScheduler 사용
    private final ApplicationContext applicationContext; // Bean 찾기 용도

    // 실행 중인 Job을 관리하는 Map (Key: Job ID, Value: ScheduledFuture)
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 해당 서버에서 Scheduler를 실행할지에 대한 설정값
    @Value("${scheduler.enabled:true}")
    private boolean runScheduler;

    /**
     * 서버 시작 후 5초 뒤에 초기화
     * 실제로는 ApplicationReadyEvent 등을 써도 되지만,
     * scheduler 자체 기능을 이용해 5초 뒤 실행을 예약합니다.
     */
    @PostConstruct
    public void init() {
        if (!runScheduler) return;

        taskScheduler.schedule(this::refreshJobs, Instant.now().plusSeconds(5));
        log.info("Scheduler init: Job loading scheduled in 5 seconds.");
    }

    /**
     * 외부에서 호출 가능한 Job 리프레시 메서드
     */
    public synchronized void refreshJobs() {
        if (!runScheduler) return;

        log.info("Refreshing jobs from database...");
        List<JobDefinition> dbJobs = jobRepository.findAll();

        // 1. 현재 DB에 존재하는 Job ID 목록 추출
        Set<String> dbJobIds = dbJobs.stream()
                .map(JobDefinition::getId)
                .collect(Collectors.toSet());

        // 2. [좀비 Job 청소] 메모리에는 있는데 DB 목록에는 없는 Job 중지
        for (String runningJobId : new HashSet<>(scheduledTasks.keySet())) {
            if (!dbJobIds.contains(runningJobId)) {
                log.warn("Found Zombie Job (Deleted from DB): {}", runningJobId);
                stopJob(runningJobId);
            }
        }

        // 3. DB 정보 기반으로 Job 시작/중지
        for (JobDefinition job : dbJobs) {
            boolean isRunning = scheduledTasks.containsKey(job.getId());
            boolean isActive = Boolean.TRUE.equals(job.getIsActive());

            if (isActive && !isRunning) {
                // Case 1: Active -> Inactive
                startJob(job);
            } else if (!isActive && isRunning) {
                // Case 2: Inactive -> Active
                stopJob(job.getId());
            }
            // Case 3: Active -> Active (무시)
            // Case 4: Inactive -> Inactive (무시)
        }
    }

    private void startJob(JobDefinition job) {
        try {
            Runnable task = createRunnableTask(job);
            ScheduledFuture<?> future;

            ScheduleType type = ScheduleType.valueOf(job.getScheduleType());
            switch (type) {
                case CRON:
                    future = taskScheduler.schedule(task, new CronTrigger(job.getSchedValue()));
                    break;
                case FIXED_RATE:
                    long rate = Long.parseLong(job.getSchedValue());
                    future = taskScheduler.scheduleAtFixedRate(task, Duration.ofSeconds(rate));
                    break;
                case FIXED_DELAY:
                    long delay = Long.parseLong(job.getSchedValue());
                    future = taskScheduler.scheduleWithFixedDelay(task, Duration.ofSeconds(delay));
                    break;
                case INITIAL_DELAY:
                    long initialDelay = Long.parseLong(job.getSchedValue());
                    future = taskScheduler.schedule(task, Instant.now().plusSeconds(initialDelay));
                    break;
                default:
                    log.warn("Unknown schedule type for job: {}", job.getId());
                    return;
            }

            if (future != null) {
                scheduledTasks.put(job.getId(), future);
                log.info("Started job: {} ({})", job.getId(), job.getDescription());
            }

        } catch (Exception e) {
            log.error("Failed to start job: {}", job.getId(), e);
        }
    }

    private void stopJob(String jobId) {
        ScheduledFuture<?> future = scheduledTasks.get(jobId);
        if (future != null) {
            future.cancel(false); // 실행 중인 작업은 완료 후 종료 (true면 interrupt 발생)
            scheduledTasks.remove(jobId);
            log.info("Stopped job: {}", jobId);
        }
    }

    /**
     * 실제 실행 로직 (Reflection + 결과 로깅)
     */
    private Runnable createRunnableTask(JobDefinition jobInfo) {
        return () -> {
            String jobId = jobInfo.getId();
            String resultMsg = "";
            boolean isSuccess = false;

            try {
                // 1. Bean 찾기 (service 컬럼은 Bean Name이어야 함)
                Object bean = applicationContext.getBean(jobInfo.getService());

                // 2. 메서드 찾기 (파라미터가 있는 경우와 없는 경우 구분 필요)
                // String 파라미터 1개 혹은 파라미터 없음만 가능
                Method method;
                Object result;

                if (jobInfo.getMethodParam() == null || jobInfo.getMethodParam().isEmpty()) {
                    method = bean.getClass().getMethod(jobInfo.getMethod());
                    result = method.invoke(bean);
                } else {
                    method = bean.getClass().getMethod(jobInfo.getMethod(), String.class);
                    result = method.invoke(bean, jobInfo.getMethodParam());
                }

                // 3. 결과 처리
                isSuccess = true;
                if (method.getReturnType().equals(Void.TYPE)) {
                    resultMsg = "Success (void)";
                } else {
                    resultMsg = String.valueOf(result);
                }

            } catch (Exception e) {
                isSuccess = false;

                // InvocationTargetException 등 래핑된 예외의 원인을 찾습니다.
                Throwable cause = e.getCause();

                // 원인이 있으면 원인의 메시지, 없으면 예외 자체의 메시지를 사용합니다.
                String causeMsg = (cause != null) ? cause.getMessage() : e.getMessage();

                // 메시지가 null인 경우(NPE 등)를 대비해 안전하게 처리
                if (causeMsg == null) {
                    causeMsg = (cause != null) ? cause.toString() : e.toString();
                }

                resultMsg = "Error: " + causeMsg;
                log.error("Job execution error [{}]: {}", jobId, resultMsg, e); // StackTrace 전체 로깅
            } finally {
                // 4. 결과 DB 업데이트 (별도 트랜잭션으로 처리해야 롤백 안됨)
                jobRepository.updateJobResult(jobId, isSuccess, resultMsg);
            }
        };
    }
}