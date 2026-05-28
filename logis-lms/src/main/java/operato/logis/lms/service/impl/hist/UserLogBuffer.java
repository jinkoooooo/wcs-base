package operato.logis.lms.service.impl.hist;

import operato.logis.lms.dto.hist.RetrySysLogDto;
import operato.logis.lms.entity.hist.AccessSysLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserLogBuffer extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(UserLogBuffer.class);

    private final Queue<AccessSysLog> logQueue = new ConcurrentLinkedQueue<>();  // 로그 저장 대기 큐
    private final Queue<RetrySysLogDto> retryLogQueue = new ConcurrentLinkedQueue<>();     // 로그 저장 재시도 큐
    private final AtomicInteger queueSize = new AtomicInteger(0); // 로그 저장 대기 큐 크기
    private final AtomicInteger retryQueueSize = new AtomicInteger(0); // 로그 저장 재시도 큐 크기

    private static final int BATCH_SIZE = 200;              // 일괄 저장할 로그 수
    private static final int MAX_QUEUE_SIZE = 2000;         // 로그 저장 대기 큐 최대 사이즈
    private static final int MAX_RETRY_QUEUE_SIZE = 10_000; // 로그 저장 재시도 대기 큐 최대 사이즈
    private static final int MAX_RETRY_COUNT = 3; // 로그 저장 실패 시 재시도 최대 회수

    // 상수
    public int getBatchSize() { return BATCH_SIZE; }

    public int getMaxQueueSize() { return MAX_QUEUE_SIZE; }

    public int getMaxRetryQueueSize() { return MAX_RETRY_QUEUE_SIZE; }

    public int getMaxRetryCount() { return MAX_RETRY_COUNT; }

    // 로그 큐
    public int getQueueSize(){
        return queueSize.get();
    }
    public boolean isQueueEmpty() {
        return logQueue.isEmpty();
    }

    public boolean isQueueFull() {
        return queueSize.get() >= MAX_QUEUE_SIZE;
    }

    public boolean isQueueReachedBatchSize() {
        return queueSize.get() >= BATCH_SIZE;
    }

    public void addLogQueue(AccessSysLog log) {
        if (log == null) {
            logger.info("log is null. Add log queue rejected.");
            return;
        }
        logQueue.offer(log);
        queueSize.incrementAndGet();
    }

    public AccessSysLog removeLogQueue() {
        AccessSysLog log = logQueue.poll();
        if (log != null) {
            queueSize.decrementAndGet();
        }
        return log;
    }

    public boolean needInsertQueue(){
        return queueSize.get() > MAX_QUEUE_SIZE * 0.8;
    }

    // 재시도 로그 큐
    public boolean isRetryQueueEmpty() {
        return retryLogQueue.isEmpty();
    }

    public int getRetryQueueSize(){
        return retryQueueSize.get();
    }

    public boolean isRetryQueueFull() {
        return retryQueueSize.get() >= MAX_QUEUE_SIZE;
    }

    public void addRetryLogQueue(RetrySysLogDto retryLog) {
        if (retryLog == null) {
            logger.info("retryLog is null. Add retry log queue rejected.");
            return;
        }
        retryLogQueue.offer(retryLog);
        retryQueueSize.incrementAndGet();
    }

    public void addRetryLogQueue(AccessSysLog log, int retryCnt, LocalDateTime nextRetryTime) {
        if (log == null) {
            logger.info("log is null. Add log queue rejected.");
            return;
        }

        if (nextRetryTime == null) {
            logger.info("nextRetryTime is null. Add log queue rejected.");
            return;
        }

        if (retryCnt > MAX_RETRY_QUEUE_SIZE) {
            logger.info("Exceeded maxRetryQueueSize = {}. Drop log", MAX_RETRY_QUEUE_SIZE);
            return;
        }

        if (retryCnt < 0){
            logger.info("Invalid retryCnt = {}. Reset to 0", retryCnt);
            retryCnt = 0;
        }

        retryLogQueue.offer(new RetrySysLogDto(log, retryCnt, nextRetryTime));
        retryQueueSize.incrementAndGet();
    }

    public RetrySysLogDto removeRetryLogQueue() {
        RetrySysLogDto log = retryLogQueue.poll();
        if (log != null) {
            retryQueueSize.decrementAndGet();
        }
        return log;
    }
}