package operato.logis.wcs.service.impl.order.host;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderHistory;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.service.impl.event.RealtimeEventPublisher;
import operato.logis.wcs.service.repository.HostOrderHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 호스트 주문 변경 이력(audit) 기록 + 실시간 이벤트 발행.
 *
 * REQUIRES_NEW 로 본 트랜잭션 롤백과 분리해 이력은 항상 보존된다.
 */
@Service
@RequiredArgsConstructor
public class HostOrderAuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderAuditLogger.class);

    private final HostOrderHistoryRepository historyRepository;

    @Autowired(required = false)
    private RealtimeEventPublisher eventPublisher;

    /**
     * 주문 단위 이력 기록 + 실시간 이벤트 발행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void log(TbWcsHostOrder order, Integer fromStatus, HostOrderEvents event, String operator, String reason) {
        if (ValueUtil.isEmpty(order) || ValueUtil.isEmpty(event)) return;

        try {
            // 이력 row 생성 및 저장
            TbWcsHostOrderHistory history = buildHistory(order, fromStatus, event, operator, reason, null);
            historyRepository.insert(history);

            // 실시간 이벤트 발행 (옵셔널)
            publishRealtimeEvent(order, fromStatus);

        } catch (Exception e) {
            logger.error("[ Order ][ Host ] audit log failed - hostOrderKey={}, event={}",
                    order.getHostOrderKey(), event, e);
        }
    }

    /**
     * 아이템 단위 이력 기록 (detail JSON 포함).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logItem(TbWcsHostOrder order, TbWcsHostOrderItem item, Integer fromStatus,
                        HostOrderEvents event, String operator, String reason) {
        if (ValueUtil.isEmpty(order) || ValueUtil.isEmpty(item) || ValueUtil.isEmpty(event)) return;

        try {
            // 아이템 detail JSON 동봉해 이력 저장
            TbWcsHostOrderHistory history = buildHistory(order, fromStatus, event, operator, reason,
                    buildItemDetailJson(item));
            historyRepository.insert(history);

        } catch (Exception e) {
            logger.error("[ Order ][ Host ] audit item log failed - hostOrderKey={}, testNo={}, event={}",
                    order.getHostOrderKey(), item.getTestNo(), event, e);
        }
    }

    /**
     * 이력 row 빌드 공통 처리.
     */
    private TbWcsHostOrderHistory buildHistory(TbWcsHostOrder order, Integer fromStatus,
                                               HostOrderEvents event, String operator,
                                               String reason, String detailJson) {
        TbWcsHostOrderHistory h = new TbWcsHostOrderHistory();
        h.setHostSystemCode(order.getHostSystemCode());
        h.setHostOrderKey(order.getHostOrderKey());
        h.setEqGroupId(order.getEqGroupId());
        h.setFromStatus(fromStatus);
        h.setToStatus(order.getOrderStatus());
        h.setEventType(event.code());
        h.setOperator(operator);
        h.setReason(reason);
        h.setDetailJson(detailJson);
        return h;
    }

    /**
     * 실시간 이벤트 발행 — publisher 주입돼 있을 때만.
     */
    private void publishRealtimeEvent(TbWcsHostOrder order, Integer fromStatus) {
        if (ValueUtil.isEmpty(eventPublisher)) return;
        eventPublisher.publishHostOrderStatus(
                order.getEqGroupId(),
                order.getHostOrderKey(),
                fromStatus,
                order.getOrderStatus(),
                order.getOrderType());
    }

    /**
     * 아이템 detail JSON 생성 — LinkedHashMap 으로 빈 값 제거 후 직렬화.
     */
    private String buildItemDetailJson(TbWcsHostOrderItem item) {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "testNo", item.getTestNo());
        putIfPresent(fields, "testRequestNo", item.getTestRequestNo());
        putIfPresent(fields, "itemCode", item.getItemCode());
        putIfPresent(fields, "lotNo", item.getLotNo());
        putIfPresent(fields, "testStatus", item.getTestStatus());
        return toJson(fields);
    }

    /**
     * 값 있을 때만 Map 에 추가.
     */
    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (ValueUtil.isNotEmpty(value)) map.put(key, value);
    }

    /**
     * Map → JSON 문자열 (간단 직렬화, 따옴표 이스케이프).
     */
    private String toJson(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"")
                    .append(e.getValue().replace("\"", "\\\""))
                    .append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }
}
