package operato.logis.lms.service.impl.hist;

import jakarta.annotation.PreDestroy;
import operato.logis.lms.consts.SessionStatus;
import operato.logis.lms.consts.SignOutType;
import operato.logis.lms.dto.hist.AccessSysLogDto;
import operato.logis.lms.dto.hist.RetrySysLogDto;
import operato.logis.lms.dto.hist.SysQueueStatusDto;
import operato.logis.lms.entity.hist.AccessSessionLog;
import operato.logis.lms.entity.hist.AccessSignInLog;
import operato.logis.lms.entity.hist.AccessSysLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserLogService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(UserLogService.class);

    private UserLogBuffer buffer;
    private UserLogPersistenceService userLogPersistenceService;
    private UserLogAsyncService userLogAsyncService;

    public UserLogService(UserLogBuffer buffer, UserLogPersistenceService userLogPersistenceService, UserLogAsyncService userLogAsyncService) {
        this.buffer = buffer;
        this.userLogPersistenceService = userLogPersistenceService;
        this.userLogAsyncService = userLogAsyncService;
    }

    /**
     * 큐에 쌓인 로그를 DB에 일괄 저장
     * - 매 5분마다 실행 (job: Save queued logs to LmsAccessSysLog table)
     */
    public void processUserActBatch() {
        if (buffer.isQueueEmpty()) return;

        List<AccessSysLog> batch = new ArrayList<>();
        for (int i = 0; i < buffer.getBatchSize() && !buffer.isQueueEmpty(); i++) {
            AccessSysLog log = buffer.removeLogQueue();
            if (log != null) {
                batch.add(log);
            }
        }

        if (!batch.isEmpty()) {
            saveUserActBatchToDatabase(batch);
        }
    }

    @PreDestroy // NOTE: Timed out while waiting for executor 'taskScheduler' 발생. 함수에 걸려있는 스케쥴링 영향.
    public void flushBeforeShutdown() {
        //logger.info("Start to save log batch");
        //processUserActBatch();
        //retryFailedLogs();
        //flushQueueSafely();
    }

    public void saveUserActBatchToDatabase(List<AccessSysLog> logs) {
        try {
            userLogPersistenceService.insertSysLogBatch(logs);
        } catch (Exception e) {
            logger.error("Failed to save log batch - {} 건", logs.size(), e);
            handleFailedBatch(logs, 0);
        }
    }

    /**
     * 배치 저장 실패 처리
     * - 재시도 큐에 적재 (retryCount = 0)
     */
    private void handleFailedBatch(List<AccessSysLog> failedLogs, int retryCount) {
        int addCount = 0;
        for (AccessSysLog log : failedLogs) {
            if (buffer.isRetryQueueFull()) {
                logger.error("Retry queue overflow. Dropping log. {}", log.getReqUri());
                break;
            }

            int delayMinutes = (int) Math.pow(2, retryCount);
            LocalDateTime nextRetryTime = LocalDateTime.now().plusMinutes(delayMinutes);
            buffer.addRetryLogQueue(log, retryCount, nextRetryTime);
            addCount++;
        }
        logger.warn("실패한 로그 {}/{} 건을 재시도 큐에 추가 (재시도 큐 {} 건)", addCount, failedLogs.size(), buffer.getRetryQueueSize());
    }

    /**
     * 실패 로그 재시도 (5분 주기)
     * 1. 재시도 대기 큐 순회하며 retryCount < MAX_BACKOFF_RETRY_COUNT인 로그 추출
     * 2. 로그 저장 시도
     * 2-1. maunal 명령 시, 재시도 시각이 안됐어도 업로드
     * 2-2. 스케쥴러에 의한 진행 시, 재시도 시각이 되었을 때 업로드
     * 3. 로그 저장 실패 시, retryCount+1하여 재시도 대기 큐에 추가
     */
    @Scheduled(fixedDelay = 300_000)
    public void retryFailedLogs() {
        retryFailedLogsImpl(false);
    }

    // Controller 호출
    public void retryFailedLogsManual() {
        retryFailedLogsImpl(true);
    }

    // 실패 로그 재시도 구현
    public void retryFailedLogsImpl(boolean isManual) {
        if (buffer.isRetryQueueEmpty()) return;

        List<RetrySysLogDto> batch = new ArrayList<>(buffer.getBatchSize());
        LocalDateTime now = LocalDateTime.now();
        int maxPolls = buffer.getRetryQueueSize();

        for (int i = 0; i < maxPolls && batch.size() < buffer.getBatchSize(); i++) {
            RetrySysLogDto batchItem = buffer.removeRetryLogQueue();

            if (batchItem == null) {
                break;
            }

            boolean exceededRetry = batchItem.getRetryCount() >= (1 << buffer.getMaxRetryCount());
            boolean retryTimeNotReached = !isManual && batchItem.getNextRetryTime().isAfter(now);

            if (exceededRetry) {
                logger.error("Max retry exceeded - dropping log = {}", batchItem.getLog().getReqUri());
            } else if (retryTimeNotReached) {
                buffer.addRetryLogQueue(batchItem);
            } else {
                batch.add(batchItem);
            }
        }

        if (batch.isEmpty()) {
            return;
        }

        logger.info("로그 저장 재시도 시작 - {} 건", batch.size());

        List<AccessSysLog> logs = batch.stream().map(RetrySysLogDto::getLog).toList();

        try {
            userLogPersistenceService.insertSysLogBatch(logs);
            logger.info("Retry success - {} items", logs.size());
        } catch (Exception e) {
            logger.error("Retry failed - {} items", logs.size(), e);

            for (RetrySysLogDto r : batch) {
                int nextDelayMinutes = (int) Math.pow(2, r.getRetryCount());
                LocalDateTime nextTime = LocalDateTime.now().plusMinutes(nextDelayMinutes);
                buffer.addRetryLogQueue(r.getLog(), r.getRetryCount() + 1, nextTime);
            }
        }
    }

    /**
     * 큐 상태 조회
     */
    public SysQueueStatusDto getQueueStatus() {
        SysQueueStatusDto dto = new SysQueueStatusDto();
        dto.setQueueSize(buffer.getQueueSize());
        dto.setMaxQueueSize(buffer.getMaxQueueSize());
        dto.setRetryQueueSize(buffer.getRetryQueueSize());
        dto.setMaxRetryQueueSize(buffer.getMaxRetryQueueSize());
        dto.setMaxRetryCount(buffer.getMaxRetryCount());
        dto.setBatchSize(buffer.getBatchSize());
        return dto;
    }

    /**
     * 큐 사용률 모니터링 (1분 주기)
     */
    @Scheduled(fixedDelay = 60000)
    public void monitorQueueHealth() {
        if (buffer.needInsertQueue()) {
            processUserActBatch();
            logger.warn("로그 큐 사용률 높음: {}/{}", buffer.getQueueSize(), buffer.getMaxQueueSize());
        }
    }

    /**
     * 사용자 활동 이력 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveUserActHistory(AccessSysLogDto logDto) {
        try {
            String requestUri = logDto.getRequestUri();
            String serverIp = InetAddress.getLocalHost().getHostAddress();
            if (ValueUtil.isEmpty(requestUri) || ValueUtil.isEmpty(logDto.getActType()) || ValueUtil.isEmpty(logDto.getResponseStatus()) || ValueUtil.isEmpty(serverIp) || ValueUtil.isEmpty(logDto.getServerPort())) {
                logger.info("saveUserActHistory [FAIL] - NULL값 허용 불가, reqUri = {}, actType = {}, resStatus = {}", requestUri, logDto.getActType(), logDto.getResponseStatus());
                return;
            }

            AccessSysLog history = new AccessSysLog();
            history.setLogDate(LocalDate.now());
            history.setLogContent("{}");  // NOTE: 필요시 Log DTO 추가
            history.setActDesc(logDto.getLog().description());
            history.setUserId(logDto.getUserId());
            history.setSessionId(logDto.getSessionId());
            history.setReqUri(requestUri);
            history.setActType(logDto.getActType());
            history.setReqDetail(logDto.getRequestJson());
            history.setResDetail(logDto.getResponseJson());
            history.setResStatus(logDto.getResponseStatus());
            history.setResTime(logDto.getDuration());
            history.setServerIp(serverIp);
            history.setServerPort(logDto.getServerPort());
            history.setAccessIp(logDto.getAccessIp());
            history.setUserAgent(logDto.getUserAgent());
            history.setDomainId(resolveDomainId());

            // 1. 즉시 insert
            userLogPersistenceService.insertSysLogBatch(List.of(history));
            // 2. 로그 큐 사용 - 서버 부하 감소
            //userLogAsyncService.saveUserActHistoryAsync(history);
            logger.info("saveUserActHistory [SUCCESS] - reqUri={}", requestUri);
        } catch (Exception e) {
            logger.error("saveUserActHistory [FAIL] - reqUri={}", logDto.getRequestUri(), e);
        }
    }

    /**
     * 현재 사용자의 도메인 ID 조회 (조회 실패 시 0L 반환)
     */
    private Long resolveDomainId() {
        try {
            return Domain.currentDomain().getId();
        } catch (Exception e) {
            logger.debug("도메인 ID 조회 실패: {}", e.getMessage());
        }
        return 0L;
    }

    /**
     * 로그인 시도 이력 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSignInHistory(boolean isSuccess, String userId, String sessionId,
                                  String userAgent, String ipAddress, String deviceInfo,
                                  Integer httpStatusCd, String failReason) {
        logger.info("Access Sign-in history [{}] - userId: {}, fail_reason: {}", isSuccess, userId, failReason);

        if (isSuccess) {
            // TODO: 날짜 제한 추가 (성능 개선)
            userLogPersistenceService.updateSignOutHistory(SignOutType.MULTI_LOGIN.getValue());
            userLogPersistenceService.updateSessionHistory(SignOutType.MULTI_LOGIN.getValue());
        }

        AccessSignInLog history = new AccessSignInLog();
        history.setLogDate(LocalDate.now());
        history.setUserId(userId);
        history.setSessionId(sessionId); // 로그인 실패 시 null
        history.setAccessIp(ipAddress);
        history.setHttpStatusCd(httpStatusCd);
        history.setUserAgent(userAgent);
        history.setDeviceInfo(deviceInfo);
        history.setSignInResult(isSuccess ? 1 : 0);
        if (!isSuccess) {
            history.setFailReason(failReason);
        }

        this.queryManager.insert(history);
    }

    /**
     * 로그인 성공 후 세션 이력 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSessionHistory(String userId, String sessionId, String userAgent, String ipAddress, String deviceInfo, Integer sessionStatus) {
        try {
            if (sessionStatus == null) {
                logger.info("Access session history [FAIL] - userId: {}, session_status: {}", userId, sessionStatus);
                return;
            }
            if (sessionStatus.equals(SessionStatus.ACTIVE.getValue())) {
                AccessSessionLog history = new AccessSessionLog();
                history.setLogDate(LocalDate.now());
                history.setUserId(userId);
                history.setSessionId(sessionId);
                history.setAccessIp(ipAddress);
                history.setUserAgent(userAgent);
                history.setDeviceInfo(deviceInfo);
                history.setSessionStatus(sessionStatus);
                this.queryManager.insert(history);
            }
            logger.info("Access session history [SUCCESS] - userId: {}, session_status: {}", userId, sessionStatus);
        } catch (Exception e) {
            logger.info("Access session history [FAIL] - userId: {}, session_status: {}", userId, sessionStatus);
            logger.error(e.getMessage(), e);
        }
    }
}