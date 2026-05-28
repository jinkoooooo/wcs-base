package operato.logis.ecs.base.ecs.dashboard.realtime.service;

import operato.logis.ecs.base.ecs.dashboard.realtime.scheduler.RealTimeBroadcastScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ====================================================================
 * WebSocket 구독 관리자
 * ====================================================================
 */
@Component
public class ActiveSubscriptionManager {

    private static final Logger logger = LoggerFactory.getLogger(ActiveSubscriptionManager.class);

    @Autowired
    private RealTimeBroadcastScheduler broadcastScheduler;

    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, SubscriptionInfo> sessionInfos = new ConcurrentHashMap<>();

    // 중앙 집중형 조합 키 생성
    public static String buildSubKey(String eqGroupId, String pageId) {
        return eqGroupId + ":" + pageId;
    }

    public void subscribe(String eqGroupId, String lcId, String pageId, String sessionId) {
        String subKey = buildSubKey(eqGroupId, pageId);

        sessionInfos.put(sessionId, new SubscriptionInfo(eqGroupId, lcId, pageId));

        Set<String> sessions = subscriptions.computeIfAbsent(subKey, k -> ConcurrentHashMap.newKeySet());
        boolean isFirstSubscriber = sessions.isEmpty();
        sessions.add(sessionId);

        logger.info("Subscribed: sessionId={}, subKey={}, isFirst={}", sessionId, subKey, isFirstSubscriber);

        if (isFirstSubscriber) {
            broadcastScheduler.startBroadcast(eqGroupId, lcId, pageId);
        }
    }

    public void unsubscribe(String sessionId) {
        SubscriptionInfo info = sessionInfos.remove(sessionId);
        if (info == null) {
            return;
        }

        String subKey = buildSubKey(info.eqGroupId, info.pageId);
        Set<String> sessions = subscriptions.get(subKey);

        if (sessions != null) {
            sessions.remove(sessionId);
            logger.info("Unsubscribed: sessionId={}, subKey={}, remaining={}", sessionId, subKey, sessions.size());

            if (sessions.isEmpty()) {
                subscriptions.remove(subKey);
                broadcastScheduler.stopBroadcast(info.eqGroupId, info.pageId);
            }
        }
    }

    // 하트비트 세션 검증용
    public boolean isSessionActive(String sessionId) {
        return sessionInfos.containsKey(sessionId);
    }

    // 프론트엔드 응답 데이터 호환용
    public int getSubscriberCountByGroup(String eqGroupId) {
        return (int) sessionInfos.values().stream()
                .filter(info -> info.eqGroupId.equals(eqGroupId))
                .count();
    }

    public int getTotalSubscriptionCount() {
        return sessionInfos.size();
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            logger.debug("Session disconnected: {}", sessionId);
            unsubscribe(sessionId);
        }
    }

    private static class SubscriptionInfo {
        final String eqGroupId;
        final String lcId;
        final String pageId;

        SubscriptionInfo(String eqGroupId, String lcId, String pageId) {
            this.eqGroupId = eqGroupId;
            this.lcId = lcId;
            this.pageId = pageId;
        }
    }
}