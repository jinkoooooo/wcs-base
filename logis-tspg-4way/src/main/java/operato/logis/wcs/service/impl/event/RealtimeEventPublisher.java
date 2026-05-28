package operato.logis.wcs.service.impl.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * STOMP 토픽으로 WCS 운영 이벤트를 실시간 발행한다.
 *
 * 토픽 규약: /topic/wcs/{eqGroupId|GLOBAL}/{type}.
 * 브로커가 없는 환경에서도 빈 생성이 가능하도록 messagingTemplate 은 optional 주입.
 */
@Service
public class RealtimeEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    private static final String TOPIC_PREFIX = "/topic/wcs/";
    private static final String GLOBAL_GROUP = "GLOBAL";

    private static final String TYPE_MODE_CHANGED        = "mode-changed";
    private static final String TYPE_FLAG_CHANGED        = "flag-changed";
    private static final String TYPE_HOST_ORDER_STATUS   = "host-order-status";
    private static final String TYPE_PORT_LOCK_ACQUIRED  = "port-lock-acquired";
    private static final String TYPE_PORT_LOCK_RELEASED  = "port-lock-released";
    private static final String TYPE_PORT_MODE_CHANGED   = "port-mode-changed";
    private static final String TYPE_TEST_FAILED         = "test-failed";
    private static final String TYPE_TEST_ITEM_FAILED    = "test-item-failed";
    private static final String TYPE_REINBOUND_ALARM     = "reinbound-alarm";

    // STOMP 브로커 미연결 환경(테스트/임베디드) 대비 — 양쪽 모두 optional
    @Autowired(required = false)
    @Qualifier("brokerMessagingTemplate")
    private SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private SimpMessagingTemplate fallbackTemplate;

    /**
     * 운영 모드 전환 이벤트.
     */
    public void publishModeChanged(String eqGroupId, String previousMode, String newMode,
                                   String operator, String reason) {
        Map<String, Object> payload = base(TYPE_MODE_CHANGED);
        payload.put("eqGroupId", eqGroupId);
        payload.put("previousMode", previousMode);
        payload.put("newMode", newMode);
        payload.put("operator", operator);
        payload.put("reason", reason);
        publish(eqGroupId, TYPE_MODE_CHANGED, payload);
    }

    /**
     * 운영 플래그 토글 이벤트.
     */
    public void publishFlagChanged(String eqGroupId, String flagName, Boolean value,
                                   String operator, String reason) {
        Map<String, Object> payload = base(TYPE_FLAG_CHANGED);
        payload.put("eqGroupId", eqGroupId);
        payload.put("flagName", flagName);
        payload.put("value", value);
        payload.put("operator", operator);
        payload.put("reason", reason);
        publish(eqGroupId, TYPE_FLAG_CHANGED, payload);
    }

    /**
     * host_order 상태 전이 이벤트.
     */
    public void publishHostOrderStatus(String eqGroupId, String hostOrderKey,
                                       Integer fromStatus, Integer toStatus, String orderType) {
        Map<String, Object> payload = base(TYPE_HOST_ORDER_STATUS);
        payload.put("eqGroupId", eqGroupId);
        payload.put("hostOrderKey", hostOrderKey);
        payload.put("from", fromStatus);
        payload.put("to", toStatus);
        payload.put("orderType", orderType);
        publish(eqGroupId, TYPE_HOST_ORDER_STATUS, payload);
    }

    /**
     * 포트 락 획득 이벤트.
     */
    public void publishPortLockAcquired(String eqGroupId, String portCode, String orderKey) {
        Map<String, Object> payload = base(TYPE_PORT_LOCK_ACQUIRED);
        payload.put("eqGroupId", eqGroupId);
        payload.put("portCode", portCode);
        payload.put("orderKey", orderKey);
        publish(eqGroupId, TYPE_PORT_LOCK_ACQUIRED, payload);
    }

    /**
     * 포트 락 해제 이벤트. forced=true 면 강제 해제.
     */
    public void publishPortLockReleased(String eqGroupId, String portCode, String orderKey, boolean forced) {
        Map<String, Object> payload = base(TYPE_PORT_LOCK_RELEASED);
        payload.put("eqGroupId", eqGroupId);
        payload.put("portCode", portCode);
        payload.put("orderKey", orderKey);
        payload.put("forced", forced);
        publish(eqGroupId, TYPE_PORT_LOCK_RELEASED, payload);
    }

    /**
     * 포트 모드 변경 이벤트.
     */
    public void publishPortModeChanged(String eqGroupId, String portCode,
                                       String previousMode, String newMode, String operator) {
        Map<String, Object> payload = base(TYPE_PORT_MODE_CHANGED);
        payload.put("eqGroupId", eqGroupId);
        payload.put("portCode", portCode);
        payload.put("previousMode", previousMode);
        payload.put("newMode", newMode);
        payload.put("operator", operator);
        publish(eqGroupId, TYPE_PORT_MODE_CHANGED, payload);
    }

    /**
     * 검사 실패 이벤트 (주문 단위).
     */
    public void publishTestFailed(String eqGroupId, String hostOrderKey, String reason) {
        Map<String, Object> payload = base(TYPE_TEST_FAILED);
        payload.put("eqGroupId", eqGroupId);
        payload.put("hostOrderKey", hostOrderKey);
        payload.put("reason", reason);
        publish(eqGroupId, TYPE_TEST_FAILED, payload);
    }

    /**
     * QC 테스트 아이템 단위 실패 이벤트.
     */
    public void publishQcTestItemFailed(String eqGroupId, String hostOrderKey, String testNo, String reason) {
        Map<String, Object> payload = base(TYPE_TEST_ITEM_FAILED);
        payload.put("eqGroupId", eqGroupId);
        payload.put("hostOrderKey", hostOrderKey);
        payload.put("testNo", testNo);
        payload.put("reason", reason);
        publish(eqGroupId, TYPE_TEST_ITEM_FAILED, payload);
    }

    /**
     * 재입고 대기 알람 — 현재 대기 파렛트 전체 + 알람 간격. GLOBAL 토픽 발행.
     * follow_up_since set/clear 시점에만 호출 — 목록 변동 시 push.
     */
    public void publishReinboundAlarm(int intervalMin, List<Map> pallets) {
        Map<String, Object> payload = base(TYPE_REINBOUND_ALARM);
        payload.put("intervalMin", intervalMin);
        payload.put("pallets", pallets);
        publish(null, TYPE_REINBOUND_ALARM, payload);
    }

    /**
     * 모든 페이로드 공통 헤더(type + timestamp) 생성.
     */
    private static Map<String, Object> base(String type) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("timestamp", OffsetDateTime.now().toString());
        return payload;
    }

    /**
     * STOMP 토픽으로 발행. 브로커 없으면 DEBUG 로그만, 발행 실패는 ERROR + 스택트레이스.
     */
    private void publish(String eqGroupId, String suffix, Map<String, Object> payload) {
        SimpMessagingTemplate template = ValueUtil.isNotEmpty(messagingTemplate) ? messagingTemplate : fallbackTemplate;
        String topic = topicOf(eqGroupId, suffix);

        // 브로커 미연결 — 운영에는 영향 없음
        if (ValueUtil.isEmpty(template)) {
            logger.debug("[ Realtime ] no messaging template - topic={}", topic);
            return;
        }

        // 발행
        try {
            template.convertAndSend(topic, payload);
        } catch (Exception e) {
            logger.error("[ Realtime ] publish failed - topic={}", topic, e);
        }
    }

    /**
     * eqGroupId 가 비면 GLOBAL 그룹으로 묶는다.
     */
    private static String topicOf(String eqGroupId, String suffix) {
        String group = ValueUtil.isEmpty(eqGroupId) ? GLOBAL_GROUP : eqGroupId;
        return TOPIC_PREFIX + group + "/" + suffix;
    }
}
