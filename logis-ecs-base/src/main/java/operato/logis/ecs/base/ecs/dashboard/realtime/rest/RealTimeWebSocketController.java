package operato.logis.ecs.base.ecs.dashboard.realtime.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import operato.logis.ecs.base.ecs.dashboard.realtime.scheduler.RealTimeBroadcastScheduler;
import operato.logis.ecs.base.ecs.dashboard.realtime.service.ActiveSubscriptionManager;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

/**
 * ====================================================================
 * 실시간 데이터 WebSocket 컨트롤러
 * ====================================================================
 */
@Controller
public class RealTimeWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeWebSocketController.class);

    @Autowired
    private ActiveSubscriptionManager subscriptionManager;

    @Autowired
    private RealTimeBroadcastScheduler broadcastScheduler;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/realtime/subscribe/{eqGroupId}/{lcId}/{pageId}")
    public void handleSubscribe(
            @DestinationVariable String eqGroupId,
            @DestinationVariable String lcId,
            @DestinationVariable String pageId,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        logger.info("Subscribe request: eqGroupId={}, lcId={}, pageId={}, sessionId={}",
                eqGroupId, lcId, pageId, sessionId);

        subscriptionManager.subscribe(eqGroupId, lcId, pageId, sessionId);
        sendSubscriptionConfirmation(lcId, eqGroupId, true);
    }

    @MessageMapping("/realtime/unsubscribe/{eqGroupId}")
    public void handleUnsubscribe(
            @DestinationVariable String eqGroupId,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        logger.info("Unsubscribe request: eqGroupId={}, sessionId={}", eqGroupId, sessionId);
        subscriptionManager.unsubscribe(sessionId);
    }

    @MessageMapping("/realtime/heartbeat/{eqGroupId}")
    public void handleHeartbeat(
            @DestinationVariable String eqGroupId,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        logger.trace("Heartbeat: eqGroupId={}, sessionId={}", eqGroupId, sessionId);

        // 해당 세션 자체가 살아있는지 검증
        if (!subscriptionManager.isSessionActive(sessionId)) {
            sendResubscribeRequired(headerAccessor, eqGroupId);
        }
    }

    @MessageMapping("/realtime/status")
    public void handleStatusRequest(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> status = ValueUtil.newMap(
                "activeBroadcasts,totalSubscriptions,runningTasks,timestamp",
                broadcastScheduler.getActiveBroadcasts(),
                subscriptionManager.getTotalSubscriptionCount(),
                broadcastScheduler.getRunningTaskCount(),
                System.currentTimeMillis()
        );

        String sessionId = headerAccessor.getSessionId();
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/realtime/status", status);
    }

    private void sendSubscriptionConfirmation(String lcId, String eqGroupId, boolean success) {
        Map<String, Object> confirmation = ValueUtil.newMap(
                "eqGroupId,subscribed,subscriberCount,timestamp",
                eqGroupId, success, subscriptionManager.getSubscriberCountByGroup(eqGroupId), System.currentTimeMillis()
        );

        messagingTemplate.convertAndSend(
                "/topic/realtime/subscription/" + lcId,
                confirmation
        );
    }

    private void sendResubscribeRequired(SimpMessageHeaderAccessor headerAccessor, String eqGroupId) {
        Map<String, Object> message = ValueUtil.newMap(
                "type,eqGroupId,message,timestamp",
                "RESUBSCRIBE_REQUIRED", eqGroupId, "Subscription expired. Please resubscribe.", System.currentTimeMillis()
        );

        String sessionId = headerAccessor.getSessionId();
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/realtime/notification", message);
    }
}