package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.model.BroadcastKey;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.registry.RealTimeProviderRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.service.LayoutCoordinateService;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.service.ActiveSubscriptionManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * ====================================================================
 * 실시간 브로드캐스트 스케줄러 (GC 최적화 + 다중 페이지 완벽 지원)
 * ====================================================================
 */
@Component
public class RealTimeBroadcastScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeBroadcastScheduler.class);

    @Autowired
    private RealTimeProviderRegistry registry;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LayoutCoordinateService coordinateService;

    private ScheduledExecutorService executor;

    // 실행 중인 태스크: "providerType:eqGroupId:pageId" -> ScheduledFuture
    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    // 활성 브로드캐스트: "eqGroupId:pageId" -> lcId
    private final Map<String, String> activeBroadcasts = new ConcurrentHashMap<>();

    // 구독자 수: "eqGroupId:pageId" -> count
    private final Map<String, Integer> subscriberCounts = new ConcurrentHashMap<>();

    // ── 성능 최적화: 250ms 고주기 브로드캐스트용 캐시 ──────────────────────────
    private final Map<String, String> topicCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastPayloadHash = new ConcurrentHashMap<>();
    private final Map<String, RealTimeFetchContext> contextCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        int poolSize = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
        executor = Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "RealTimeBroadcast");
            t.setDaemon(true);
            return t;
        });
        logger.info("RealTimeBroadcastScheduler initialized with pool size: {}", poolSize);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down RealTimeBroadcastScheduler...");
        stopAllBroadcasts();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void startBroadcast(String eqGroupId, String lcId, String pageId) {
        String broadcastKey = ActiveSubscriptionManager.buildSubKey(eqGroupId, pageId);

        if (activeBroadcasts.containsKey(broadcastKey)) {
            subscriberCounts.merge(broadcastKey, 1, Integer::sum);
            logger.info("Added subscriber to existing broadcast: broadcastKey={}, subscribers={}",
                    broadcastKey, subscriberCounts.get(broadcastKey));
            return;
        }

        coordinateService.loadLayoutCoordinates(lcId, pageId);

        for (RealTimeDataProvider<?> provider : registry.getEnabledProviders()) {
            String taskKey = buildTaskKey(provider.getProviderType(), broadcastKey);

            if (!runningTasks.containsKey(taskKey)) {
                ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                        () -> broadcastProviderData(provider, eqGroupId, lcId, pageId),
                        0,
                        provider.getIntervalMs(),
                        TimeUnit.MILLISECONDS
                );
                runningTasks.put(taskKey, future);
                logger.debug("Started broadcast task: {}", taskKey);
            }
        }

        activeBroadcasts.put(broadcastKey, lcId);
        subscriberCounts.put(broadcastKey, 1);

        logger.info("Started broadcast for page: {}, providers={}", broadcastKey, registry.getEnabledProviders().size());
    }

    public synchronized void stopBroadcast(String eqGroupId, String pageId) {
        String broadcastKey = ActiveSubscriptionManager.buildSubKey(eqGroupId, pageId);
        Integer count = subscriberCounts.get(broadcastKey);

        if (count == null || count <= 1) {
            doStopBroadcast(broadcastKey);
            subscriberCounts.remove(broadcastKey);
        } else {
            subscriberCounts.put(broadcastKey, count - 1);
            logger.info("Removed subscriber from page: {}, remaining={}", broadcastKey, count - 1);
        }
    }

    private void doStopBroadcast(String broadcastKey) {
        String suffix = ":" + broadcastKey;

        runningTasks.entrySet().removeIf(entry -> {
            if (entry.getKey().endsWith(suffix)) {
                entry.getValue().cancel(false);
                String taskKey = entry.getKey();
                topicCache.remove(taskKey);
                lastPayloadHash.remove(taskKey);
                contextCache.remove(taskKey);
                logger.debug("Stopped broadcast task: {}", taskKey);
                return true;
            }
            return false;
        });

        activeBroadcasts.remove(broadcastKey);
        logger.info("Stopped broadcast for page: {}", broadcastKey);
    }

    public synchronized void stopAllBroadcasts() {
        for (ScheduledFuture<?> future : runningTasks.values()) {
            future.cancel(false);
        }
        runningTasks.clear();
        activeBroadcasts.clear();
        subscriberCounts.clear();
        topicCache.clear();
        lastPayloadHash.clear();
        contextCache.clear();
        logger.info("Stopped all broadcasts");
    }

    private void broadcastProviderData(RealTimeDataProvider<?> provider, String eqGroupId, String lcId, String pageId) {
        try {
            String broadcastKey = ActiveSubscriptionManager.buildSubKey(eqGroupId, pageId);
            String taskKey = buildTaskKey(provider.getProviderType(), broadcastKey);

            // 1) FetchContext 재사용 (GC 압력 완화)
            RealTimeFetchContext context = contextCache.computeIfAbsent(
                    taskKey, k -> new RealTimeFetchContext(new BroadcastKey(eqGroupId, lcId, pageId))
            );

            List<?> data = provider.fetchData(context);

            if (data == null || data.isEmpty()) {
                lastPayloadHash.remove(taskKey);
                return;
            }

            // 2) 변경 감지 (이전 데이터와 해시값이 같으면 전송 생략)
            int currentHash = data.hashCode();
            Integer prevHash = lastPayloadHash.get(taskKey);
            if (prevHash != null && prevHash == currentHash) {
                return;
            }
            lastPayloadHash.put(taskKey, currentHash);

            // 3) 토픽 문자열 캐싱
            String topic = topicCache.computeIfAbsent(
                    taskKey, k -> provider.getTopicPattern()
                            .replace("{lcId}", lcId)
                            .replace("{eqGroupId}", eqGroupId)
                            .replace("{pageId}", pageId)
            );

            messagingTemplate.convertAndSend(topic, data);

            if (logger.isDebugEnabled()) {
                logger.debug("[RealTimeBroadcast] Sent {} {} items → {}", data.size(), provider.getProviderType(), topic);
            }

        } catch (Exception e) {
            if (provider.retryOnError()) {
                logger.warn("Broadcast error for {} (will retry): {}", provider.getProviderType(), e.getMessage());
            } else {
                logger.error("Broadcast error for {} (no retry): {}", provider.getProviderType(), e.getMessage(), e);
            }
        }
    }

    public Set<String> getActiveBroadcasts() {
        return activeBroadcasts.keySet();
    }

    public int getSubscriberCount(String eqGroupId, String pageId) {
        return subscriberCounts.getOrDefault(ActiveSubscriptionManager.buildSubKey(eqGroupId, pageId), 0);
    }

    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    private String buildTaskKey(String providerType, String broadcastKey) {
        return providerType + ":" + broadcastKey;
    }
}