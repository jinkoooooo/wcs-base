package operato.logis.wcs.service.impl.qctest;

import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.stringOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 시험 미종결 host_order 목록 + 자식 shuttle 사이클 조회.
 *
 * triggerReinbound: 사전 발행된 INBOUND shuttle 을 SENDING 으로 전환한다.
 */
@Service
@RequiredArgsConstructor
public class QcTestTargetQueryService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(QcTestTargetQueryService.class);

    private final ShuttleOrderRepository shuttleOrderRepository;
    private final ShuttleOrderStateWriter shuttleOrderStateWriter;

    /**
     * INBOUND_TEST_WAIT 상태 host_order 목록 + 자식 셔틀 사이클을 함께 반환.
     */
    public List<Map<String, Object>> listInspectionTargets(String eqGroupId) {
        StringBuilder sql = new StringBuilder("""
            SELECT ho.host_order_key, ho.eq_group_id, ho.barcode, ho.test_required, ho.test_status,
                   ho.order_status, ho.received_at, ho.test_requested_at, ho.test_resulted_at,
                   ho.test_reason
              FROM tb_wcs_host_order ho
             WHERE ho.order_status = :status
        """);
        Map<String, Object> params = new HashMap<>();
        params.put("status", HostOrderStatus.INBOUND_TEST_WAIT.code());

        // eqGroupId 옵셔널 필터
        if (ValueUtil.isNotEmpty(eqGroupId)) {
            sql.append(" AND ho.eq_group_id = :eqGroupId");
            params.put("eqGroupId", eqGroupId);
        }
        sql.append(" ORDER BY ho.received_at DESC");

        // 자식 셔틀 사이클 첨부
        @SuppressWarnings("unchecked")
        List<Map> rows = this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 0);
        List<Map<String, Object>> result = new ArrayList<>();
        if (ValueUtil.isNotEmpty(rows)) {
            for (Map r : rows) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) r;
                m.put("shuttles", listShuttlesForHost(stringOf(m.get("host_order_key"))));
                result.add(m);
            }
        }
        return result;
    }

    /**
     * 호스트 오더의 자식 셔틀 사이클 요약을 Map 리스트로.
     */
    private List<Map<String, Object>> listShuttlesForHost(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) return Collections.emptyList();
        List<TbWcsShuttleOrder> shuttles = shuttleOrderRepository.findByHostOrderKey(hostOrderKey);
        List<Map<String, Object>> out = new ArrayList<>();
        if (ValueUtil.isEmpty(shuttles)) return out;
        for (TbWcsShuttleOrder s : shuttles) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderKey", s.getOrderKey());
            m.put("orderType", s.getOrderType());
            m.put("orderStatus", s.getOrderStatus());
            m.put("ecsIfStatus", s.getEcsIfStatus());
            m.put("parentOrderKey", s.getParentOrderKey());
            m.put("fromLocCode", s.getFromLocCode());
            m.put("toLocCode", s.getToLocCode());
            m.put("barcode", s.getBarcode());
            m.put("createdAt", s.getCreatedAt());
            out.add(m);
        }
        return out;
    }

    /**
     * 가장 최근 재입고 INBOUND shuttle (parent_order_key NOT NULL, CREATED) 을 SENDING 으로 전환.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> triggerReinbound(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) {
            return Map.of("success", false, "message", "hostOrderKey 누락");
        }

        List<TbWcsShuttleOrder> shuttles = shuttleOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(shuttles)) {
            return Map.of("success", false, "message", "shuttle 없음");
        }

        Integer createdCode = ShuttleOrderStatus.CREATED.codeAsIntOrNull();
        String inboundType = OrderType.INBOUND.codeAsString();

        // 가장 최근 INBOUND CREATED + parent_order_key 가 있는 셔틀
        TbWcsShuttleOrder target = null;
        for (TbWcsShuttleOrder s : shuttles) {
            if (!inboundType.equalsIgnoreCase(s.getOrderType())) continue;
            if (ValueUtil.isEmpty(s.getParentOrderKey())) continue;
            if (!createdCode.equals(s.getOrderStatus())) continue;
            if (ValueUtil.isEmpty(target)) target = s;
            else if (ValueUtil.isNotEmpty(s.getCreatedAt()) && ValueUtil.isNotEmpty(target.getCreatedAt())
                    && s.getCreatedAt().after(target.getCreatedAt())) {
                target = s;
            }
        }

        if (ValueUtil.isEmpty(target)) {
            return Map.of("success", false, "message", "사전 발행된 재입고 shuttle 없음");
        }

        // SENDING 전이
        shuttleOrderStateWriter.markEcsIfStatusSending(target);

        logger.info("[ Qctest ][ Target ] reinbound triggered - host={}, shuttle={}",
                hostOrderKey, target.getOrderKey());

        return Map.of("success", true, "orderKey", target.getOrderKey());
    }

    /**
     * Object → 빈 문자열 fallback 변환.
     */}
