package operato.logis.lms.service.impl.hist;

import lombok.RequiredArgsConstructor;
import operato.logis.lms.entity.hist.AccessSysLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Service
@RequiredArgsConstructor
public class UserLogAsyncService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(UserLogAsyncService.class);

    private UserLogBuffer buffer;
    private UserLogService userLogService;

    /**
     * 로그 저장 대기 큐에 신규 로그 추가 (비동기)
     */
    @Async("logExecutor")
    public void saveUserActHistoryAsync(AccessSysLog log) {
        if (buffer.isQueueFull()) {
            logger.warn("사용자 활동 로그 큐가 가득참. 로그 드롭: {}", log.getReqUri());
            return;
        }

        buffer.addLogQueue(log);

        if (buffer.isQueueReachedBatchSize()) {
            userLogService.processUserActBatch();
        }
    }
}