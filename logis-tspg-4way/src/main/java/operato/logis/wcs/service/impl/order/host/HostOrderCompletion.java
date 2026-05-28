package operato.logis.wcs.service.impl.order.host;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.QcTestStatus;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Set;

/**
 * 호스트 주문 완료/전이 통합 처리.
 *
 * onShuttleCompleted — shuttle 완료 시 호스트 전이:
 *   - INBOUND  : test_status=FAILED → TEST_FAILED / 미종결 → INBOUND_TEST_WAIT / 그 외 → COMPLETED
 *   - OUTBOUND : INBOUND_TEST_WAIT 면 보류 / OUTBOUND + 재입고 모두 완료 → COMPLETED
 *
 * tryCompleteHostOrder — 외부 트리거 (재시도/스케줄러):
 *   진행중 shuttle 0건 + (입고+testRequired 시 testStatus 확정) → COMPLETED.
 */
@Service
@RequiredArgsConstructor
public class HostOrderCompletion {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderCompletion.class);

    // 완료/취소로 간주되는 shuttle 상태
    private static final Set<Integer> SHUTTLE_DONE_STATES = Set.of(
            ShuttleOrderStatus.COMPLETED.codeAsIntOrNull(),
            ShuttleOrderStatus.CANCELLED.codeAsIntOrNull(),
            ShuttleOrderStatus.ERROR_GENERAL.codeAsIntOrNull()
    );

    private final HostOrderRepository hostOrderRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final HostOrderStateWriter stateWriter;
    private final HostOrderAuditLogger auditLogger;

    /**
     * shuttle 완료 시 host_order 상태 전이.
     * 반환값 = host=COMPLETED 전이 여부 (HOST 통보 발행 대상).
     */
    public boolean onShuttleCompleted(TbWcsShuttleOrder shuttleOrder) {
        if (ValueUtil.isEmpty(shuttleOrder)) return false;
        String hostOrderKey = shuttleOrder.getHostOrderKey();
        if (ValueUtil.isEmpty(hostOrderKey)) return false;

        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(host)) return false;

        try {
            // 유형별 전이 분기
            if (OrderType.INBOUND.matches(host.getOrderType())) {
                return transitionInbound(shuttleOrder, host);
            }
            if (OrderType.OUTBOUND.matches(host.getOrderType())) {
                return transitionOutbound(host);
            }
            return false;

        } catch (Exception e) {
            logger.error("[ Order ][ Host ] onShuttleCompleted failed - hostOrderKey={}", hostOrderKey, e);
            return false;
        }
    }

    /**
     * 외부 트리거 — 진행중 shuttle 0건 + 시험 결과 확정 시 COMPLETED 전이.
     */
    public void tryCompleteHostOrder(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) return;

        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(host)) return;
        if (HostOrderStatus.COMPLETED.code() == host.getOrderStatus()) return;

        // 모든 shuttle 종료 여부
        if (!allShuttlesDone(hostOrderKey)) return;

        // INBOUND + testRequired 인 경우 시험 결과 확정 여부 추가 검증
        if (OrderType.INBOUND.matches(host.getOrderType()) && Boolean.TRUE.equals(host.getTestRequired())
                && !isTestFinalized(host.getTestStatus())) {
            return;
        }

        applyCompleted(host, "all shuttles done");
    }

    /**
     * INBOUND 전이 — 재입고는 sibling 전체 완료 검증 추가.
     */
    private boolean transitionInbound(TbWcsShuttleOrder shuttleOrder, TbWcsHostOrder host) {
        boolean isReinbound = ValueUtil.isNotEmpty(shuttleOrder.getParentOrderKey());

        // 재입고는 sibling 전체 완료된 경우만 진행
        if (isReinbound && !allShuttlesDone(host.getHostOrderKey())) return false;

        // testRequired 인 경우 결과별 분기
        if (Boolean.TRUE.equals(host.getTestRequired())) {
            String testStatus = host.getTestStatus();

            // 시험 부적합 → TEST_FAILED
            if (QcTestStatus.FAILED.codeAsString().equalsIgnoreCase(testStatus)) {
                applyTestFailed(host, reinboundMsg(isReinbound,
                        "reinbound completed but test failed",
                        "inbound completed but test already failed"));
                return false;
            }

            // 시험 미종결 → INBOUND_TEST_WAIT
            if (!QcTestStatus.PASSED.codeAsString().equalsIgnoreCase(testStatus)) {
                applyInboundTestWait(host, reinboundMsg(isReinbound,
                        "reinbound completed - awaiting test result",
                        "inbound test wait"));
                return false;
            }
        }

        // 시험 불필요 또는 PASSED → COMPLETED
        applyCompleted(host, reinboundMsg(isReinbound, "reinbound completed - all done", null));
        return true;
    }

    /**
     * OUTBOUND 전이 — 시험 대기 중이면 보류, OUTBOUND + 재입고 모두 완료 시 COMPLETED.
     */
    private boolean transitionOutbound(TbWcsHostOrder host) {

        // 시험 대기 상태면 보류
        Integer testWaitCode = HostOrderStatus.INBOUND_TEST_WAIT.code();
        if (ValueUtil.isNotEmpty(testWaitCode) && testWaitCode.equals(host.getOrderStatus())) {
            return false;
        }

        // sibling shuttle 전체 조회
        List<TbWcsShuttleOrder> siblings = shuttleOrderRepository.findByHostOrderKey(host.getHostOrderKey());
        if (ValueUtil.isEmpty(siblings)) return false;

        // OUTBOUND/INBOUND 각각 진행중 카운트
        if (hasPendingByType(siblings, OrderType.OUTBOUND)) return false;
        if (hasPendingByType(siblings, OrderType.INBOUND)) return false;

        applyCompleted(host, "all outbound + reinbound completed");
        return true;
    }

    /**
     * 모든 shuttle 이 종료(완료/취소/에러) 상태인지.
     */
    private boolean allShuttlesDone(String hostOrderKey) {
        List<TbWcsShuttleOrder> siblings = shuttleOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(siblings)) return false;
        return siblings.stream().allMatch(s -> SHUTTLE_DONE_STATES.contains(s.getOrderStatus()));
    }

    /**
     * 특정 유형의 진행중 shuttle 존재 여부 (COMPLETED/CANCELLED 외).
     */
    private boolean hasPendingByType(List<TbWcsShuttleOrder> siblings, OrderType type) {
        String typeCode = type.codeAsString();
        Integer completed = ShuttleOrderStatus.COMPLETED.codeAsIntOrNull();
        Integer cancelled = ShuttleOrderStatus.CANCELLED.codeAsIntOrNull();

        return siblings.stream()
                .filter(s -> typeCode.equalsIgnoreCase(s.getOrderType()))
                .anyMatch(s -> !completed.equals(s.getOrderStatus())
                        && !cancelled.equals(s.getOrderStatus()));
    }

    /**
     * COMPLETED 전이 + 감사 로그.
     */
    private void applyCompleted(TbWcsHostOrder host, String message) {
        int prev = stateWriter.markCompleted(host);
        auditLogger.log(host, prev, HostOrderEvents.COMPLETED, null, message);
        logTransitionIfMessage(host, message);
    }

    /**
     * TEST_FAILED 전이 + 감사 로그.
     */
    private void applyTestFailed(TbWcsHostOrder host, String message) {
        int prev = host.getOrderStatus();
        stateWriter.markTestFailed(host);
        auditLogger.log(host, prev, HostOrderEvents.COMPLETED, null, message);
        logTransitionIfMessage(host, message);
    }

    /**
     * INBOUND_TEST_WAIT 전이 + 감사 로그.
     */
    private void applyInboundTestWait(TbWcsHostOrder host, String message) {
        int prev = host.getOrderStatus();
        stateWriter.markInboundTestWait(host);
        auditLogger.log(host, prev, HostOrderEvents.COMPLETED, null, message);
        logTransitionIfMessage(host, message);
    }

    /**
     * 전이 로그 (메시지 있을 때만).
     */
    private void logTransitionIfMessage(TbWcsHostOrder host, String message) {
        if (ValueUtil.isNotEmpty(message)) {
            logger.info("[ Order ][ Host ] transition - hostOrderKey={}, reason={}",
                    host.getHostOrderKey(), message);
        }
    }

    /**
     * 재입고/초기입고 메시지 선택.
     */
    private String reinboundMsg(boolean isReinbound, String reinboundMsg, String normalMsg) {
        return isReinbound ? reinboundMsg : normalMsg;
    }

    /**
     * 시험 결과 확정 여부 (PASSED/FAILED).
     */
    private boolean isTestFinalized(String testStatus) {
        return QcTestStatus.PASSED.code().equals(testStatus)
                || QcTestStatus.FAILED.code().equals(testStatus);
    }
}
