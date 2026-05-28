package operato.logis.wcs.service.impl.qctest;

import operato.logis.wcs.consts.*;
import operato.logis.wcs.dto.QcTestResultRequest;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsQcTestRequest;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.event.RealtimeEventPublisher;
import operato.logis.wcs.service.impl.external.ExternalOrderNotifier;
import operato.logis.wcs.service.impl.external.ExternalQcTestRequester;
import operato.logis.wcs.service.impl.order.host.HostOrderAuditLogger;
import operato.logis.wcs.service.impl.order.host.HostOrderEvents;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import operato.logis.wcs.service.impl.inventory.reservation.InboundReservationService;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.QcTestRequestRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 입고 시험(QC Test) 파이프라인 — ITEM 단위.
 *
 * 식별자 정책:
 *   - test_request_no: 사용자가 UI 등록 시 입력 (시험 대상 item 필수)
 *   - test_no: 외부 LIMS 가 결과 콜백 시 발급/통보
 *
 * order_status 와 test_status 는 독립 차원이며 입고 완료 후(INBOUND_TEST_WAIT) 결과 확정 시점에만
 * order_status 가 COMPLETED / TEST_FAILED 로 전이된다.
 * 진실의 원천은 host_order_item.test_status, 헤더는 집계 캐시다.
 */
@Service
@RequiredArgsConstructor
public class QcResultService {

    private static final Logger logger = LoggerFactory.getLogger(QcResultService.class);

    // DB update 필드 셋 — 의미별 분리
    private static final String[] TEST_REQUEST_FIELDS = {
            "testStatus", "testRequestedAt", "testResultedAt", "testReason"
    };
    private static final String[] TEST_RESULT_FIELDS = {
            "testStatus", "testResultedAt", "testReason"
    };
    private static final String[] TEST_RESULT_WITH_NO_FIELDS = {
            "testStatus", "testResultedAt", "testReason", "testNo"
    };

    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final ExternalOrderNotifier externalNotifier;
    private final ExternalQcTestRequester externalQcTestRequester;
    private final HostOrderAuditLogger historyLogger;
    @Autowired(required = false) private RealtimeEventPublisher eventPublisher;
    private final InboundReservationService reservationService;
    private final ShuttleOrderStateWriter shuttleOrderStateWriter;
    private final HostOrderStateWriter hostOrderStateWriter;
    private final OrderLookupUtils orderLookup;
    private final QcTestRequestRepository qcTestRequestRepository;

    /**
     * 시험 요청 — host_order 와 item 의 test_* 필드를 초기화하고 외부 LIMS 에 의뢰.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsHostOrder requestTest(TbWcsHostOrder host) {
        if (ValueUtil.isEmpty(host)) return null;

        // 시험 대상 item 의 test_request_no 누락 검증
        validateTestRequestNoForTargets(host.getHostOrderKey());

        Date now = new Date();
        String prev = String.valueOf(host.getOrderStatus());

        // item 별 초기 상태 설정 + 시험 대상 추출
        List<TbWcsHostOrderItem> qcTargets = initializeItemTestStatuses(host.getHostOrderKey(), now);
        boolean hasQcTargets = ValueUtil.isNotEmpty(qcTargets);

        // 헤더 집계 상태 — 시험 대상 없으면 즉시 PASSED
        QcTestStatus headerStatus = hasQcTargets ? QcTestStatus.REQUESTED : QcTestStatus.PASSED;
        hostOrderStateWriter.markReceived(host);
        host.setTestStatus(headerStatus.code());
        host.setTestRequestedAt(now);
        host.setTestResultedAt(hasQcTargets ? null : now);
        host.setTestReason(null);
        hostOrderRepository.update(host, TEST_REQUEST_FIELDS);
        externalNotifier.notifyOrderStatusChanged(host, prev);

        // 시험 대상 item 별 외부 LIMS 의뢰 (실패해도 트랜잭션 진행)
        for (TbWcsHostOrderItem item : qcTargets) {
            requestExternalQcTestSafely(host, item);
        }

        logger.info("[ Qctest ][ Request ] requested - hostOrderKey={}, targets={}, testStatus={}",
                host.getHostOrderKey(), qcTargets.size(), headerStatus.code());
        return host;
    }

    /**
     * 시험 대상 item 의 test_request_no 누락 검증.
     */
    private void validateTestRequestNoForTargets(String hostOrderKey) {
        List<String> missing = new ArrayList<>();
        for (TbWcsHostOrderItem item : hostOrderItemRepository.findByHostOrderKey(hostOrderKey)) {
            if (!Boolean.TRUE.equals(item.getTestRequired())) continue;
            if (ValueUtil.isEmpty(item.getTestRequestNo())) {
                missing.add("sku=" + item.getItemCode() + ",lot_no=" + item.getLotNo());
            }
        }
        if (ValueUtil.isNotEmpty(missing)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "시험 대상 item 의 test_request_no 가 누락되었습니다. hostOrderKey=%s, missing=%s"
                            .formatted(hostOrderKey, missing));
        }
    }

    /**
     * host_order 의 모든 item 에 초기 test 상태 설정 — 시험 대상은 REQUESTED, 비대상은 PASSED.
     */
    private List<TbWcsHostOrderItem> initializeItemTestStatuses(String hostOrderKey, Date now) {
        List<TbWcsHostOrderItem> qcTargets = new ArrayList<>();
        for (TbWcsHostOrderItem item : hostOrderItemRepository.findByHostOrderKey(hostOrderKey)) {
            boolean isQcTarget = Boolean.TRUE.equals(item.getTestRequired());
            QcTestStatus status = isQcTarget ? QcTestStatus.REQUESTED : QcTestStatus.PASSED;
            fillTestFields(item, status, now, isQcTarget ? null : now, null);
            hostOrderItemRepository.update(item, TEST_REQUEST_FIELDS);
            if (isQcTarget) qcTargets.add(item);
        }
        return qcTargets;
    }

    /**
     * 외부 LIMS 시험 의뢰 — 실패해도 예외 전파 안 함 (전체 트랜잭션 보호).
     */
    private void requestExternalQcTestSafely(TbWcsHostOrder host, TbWcsHostOrderItem item) {
        try {
            externalQcTestRequester.requestQcTest(host, item);
        } catch (Exception e) {
            logger.error("[ Qctest ][ Request ] external qc test failed - hostOrderKey={}, testRequestNo={}",
                    host.getHostOrderKey(), item.getTestRequestNo(), e);
        }
    }

    /**
     * 시험 결과 반영 — LIMS 콜백 시점 호출. 동일 testReqNo 의 모든 item 에 결과를 적용한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsHostOrder applyResult(String testReqNo, QcTestResultRequest req) {
        QcTestStatus result = validateAndParse(testReqNo, req);

        // ★ 마스터(qc_test_request) 먼저 반영 — 취소(CANCEL)는 종결 처리하지 않음(운영자 재의뢰 여지)
        if (result != QcTestStatus.CANCEL) {
            completeQcTestRequestMaster(testReqNo, req.getTestNo());
        }

        List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByTestRequestNo(testReqNo);
        if (ValueUtil.isEmpty(items)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "host_order_item 없음: testRequestNo=" + testReqNo);
        }

        Date now = new Date();
        Map<String, List<TbWcsHostOrderItem>> updatedByHost = applyItemResults(items, result, req, now);

        // 모두 멱등 skip — 현재 host_order 상태만 반환
        if (ValueUtil.isEmpty(updatedByHost)) {
            return hostOrderRepository.findByHostOrderKey(items.get(0).getHostOrderKey());
        }

        // 변경된 host_order 별 재계산 후 마지막 호스트 반환
        TbWcsHostOrder lastHost = null;
        for (Map.Entry<String, List<TbWcsHostOrderItem>> entry : updatedByHost.entrySet()) {
            lastHost = recomputeHost(entry.getKey(), entry.getValue(), result, req, testReqNo, now);
        }
        return lastHost;
    }

    /**
     * 입력값 검증 — testReqNo, result 필수. PASSED/FAILED 만 허용.
     */
    private QcTestStatus validateAndParse(String testReqNo, QcTestResultRequest req) {
        if (ValueUtil.isEmpty(testReqNo)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "testRequestNo is required");
        }
        if (ValueUtil.isEmpty(req) || ValueUtil.isEmpty(req.getResult())) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "result is required");
        }
        QcTestStatus result = QcTestStatus.from(req.getResult());
        if (result != QcTestStatus.PASSED && result != QcTestStatus.FAILED && result != QcTestStatus.CANCEL) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "result는 PASSED, FAILED, CANCEL 중 하나: " + req.getResult());
        }
        return result;
    }

    /**
     * 마스터(tb_wcs_qc_test_request) 결과 선반영 — testReqNo 의 모든 의뢰 행을 COMPLETED 로.
     * 멱등: 이미 COMPLETED + 동일 testNo 면 skip. testNo 없으면 기존값 유지(FAIL 등 미동반 허용).
     */
    private void completeQcTestRequestMaster(String testReqNo, String limsTestNo) {
        List<TbWcsQcTestRequest> masters = qcTestRequestRepository.findAllByTestRequestNo(testReqNo);
        if (ValueUtil.isEmpty(masters)) {
            // 마스터가 없는 경우 정책 선택: 경고만 남기고 진행 vs 예외.
            // 입고 흐름상 마스터가 반드시 선행되므로 경고 후 진행으로 둠.
            logger.warn("[ Qctest ][ Result ] master not found - testReqNo={}", testReqNo);
            return;
        }

        Date now = new Date();
        for (TbWcsQcTestRequest m : masters) {
            boolean alreadyDone = QcTestRequestStatus.COMPLETED.code().equals(m.getStatus());
            boolean sameTestNo = ValueUtil.isEmpty(limsTestNo) || limsTestNo.equals(m.getTestNo());
            if (alreadyDone && sameTestNo) {
                logger.info("[ Qctest ][ Result ] master idempotent skip - testReqNo={}, testNo={}",
                        testReqNo, m.getTestNo());
                continue;
            }

            if (ValueUtil.isNotEmpty(limsTestNo)) m.setTestNo(limsTestNo);
            m.setStatus(QcTestRequestStatus.COMPLETED.code());
            m.setCompletedAt(now);
            qcTestRequestRepository.update(m, "testNo", "status", "completedAt");
            logger.info("[ Qctest ][ Result ] master completed - testReqNo={}, id={}, testNo={}",
                    testReqNo, m.getId(), m.getTestNo());
        }
    }

    /**
     * item 단위 결과 반영 + host_order_key 별로 그룹핑하여 반환.
     */
    private Map<String, List<TbWcsHostOrderItem>> applyItemResults(
            List<TbWcsHostOrderItem> items, QcTestStatus result, QcTestResultRequest req, Date now) {

        String reason = req.getReason();
        String limsTestNo = req.getTestNo();

        Map<String, List<TbWcsHostOrderItem>> updatedByHost = new LinkedHashMap<>();
        for (TbWcsHostOrderItem item : items) {
            // 멱등 처리 — 같은 결과 + 같은 testNo 면 skip
            if (result == QcTestStatus.from(item.getTestStatus())
                    && (ValueUtil.isEmpty(limsTestNo) || limsTestNo.equals(item.getTestNo()))) {
                logger.info("[ Qctest ][ Result ] idempotent skip - status={}, testRequestNo={}, testNo={}",
                        result.code(), item.getTestRequestNo(), item.getTestNo());
                continue;
            }

            // 결과 필드 반영
            fillTestFields(item, result, null, now, reason);
            if (ValueUtil.isNotEmpty(limsTestNo)) {
                item.setTestNo(limsTestNo);
                hostOrderItemRepository.update(item, TEST_RESULT_WITH_NO_FIELDS);
            } else {
                hostOrderItemRepository.update(item, TEST_RESULT_FIELDS);
            }

            updatedByHost.computeIfAbsent(item.getHostOrderKey(), k -> new ArrayList<>()).add(item);
        }
        return updatedByHost;
    }

    /**
     * host_order 단위로 결과 재계산 — 헤더 갱신, 상태 전이, 알림, 후속 처리, 감사 로그 일괄 수행.
     */
    private TbWcsHostOrder recomputeHost(String hostOrderKey, List<TbWcsHostOrderItem> updatedItems,
                                         QcTestStatus result, QcTestResultRequest req,
                                         String testReqNo, Date now) {
        TbWcsHostOrder host = orderLookup.getHostOrderOrThrow(hostOrderKey);
        Aggregate aggregate = aggregate(hostOrderItemRepository.findTestTargetItems(hostOrderKey));
        String prev = String.valueOf(host.getOrderStatus());
        Integer prevInt = parseStatus(prev);

        // 헤더 집계 필드 갱신 + DB 반영
        saveAggregateToHeader(host, aggregate, now);

        // order_status 전이 (시험 대기 상태에서만)
        transitionOrderStatusByResult(host, aggregate);

        // 외부 알림
        externalNotifier.notifyOrderStatusChanged(host, prev);

        // 후속 처리 (재입고 셔틀 취소, stock 전파)
        handleResultSideEffects(host, aggregate, req.getReason());

        // 감사 로그
        logItemHistory(host, updatedItems, result, prevInt, req);
        logHeaderHistory(host, aggregate, prevInt, prev, req);

        logger.info("[ Qctest ][ Result ] applied - testReqNo={}, hostOrderKey={}, updated={}, result={}, testNo={}, aggregate={}, prev={}, orderStatus={}, testStatus={}",
                testReqNo, hostOrderKey, updatedItems.size(), result.code(), req.getTestNo(),
                aggregate.summary(), prev, host.getOrderStatus(), host.getTestStatus());
        return host;
    }

    /**
     * 헤더 집계 필드 in-memory 갱신 + DB persist.
     */
    private void saveAggregateToHeader(TbWcsHostOrder host, Aggregate aggregate, Date now) {
        fillHeaderTestFields(host, aggregate, now);
        hostOrderRepository.update(host, TEST_RESULT_FIELDS);
    }

    /**
     * INBOUND_TEST_WAIT 일 때만 시험 결과에 따라 order_status 전이.
     */
    private void transitionOrderStatusByResult(TbWcsHostOrder host, Aggregate aggregate) {
        if (host.getOrderStatus() != HostOrderStatus.INBOUND_TEST_WAIT.code()) return;

        if (aggregate.allPassed()) {
            hostOrderStateWriter.markCompleted(host);
        } else if (aggregate.anyFailed()) {
            hostOrderStateWriter.markTestFailed(host);
        }
    }

    /**
     * 후속 처리 — 부적합 시 재입고 셔틀 취소, 결과 확정 시 stock 전파.
     */
    private void handleResultSideEffects(TbWcsHostOrder host, Aggregate aggregate, String reason) {
        if (aggregate.anyFailed()) {
            cancelOpenReinboundShuttles(host.getHostOrderKey(), reason);
        }
        if (aggregate.allPassed() || aggregate.anyFailed()) {
            reservationService.propagateQcResultToStocks(host.getHostOrderKey(), aggregate.allPassed(), aggregate.anyFailed());
        }
    }

    /**
     * item 단위 감사 로그 + 부적합 이벤트 발행.
     */
    private void logItemHistory(TbWcsHostOrder host, List<TbWcsHostOrderItem> updatedItems,
                                QcTestStatus result, Integer prevInt, QcTestResultRequest req) {
        HostOrderEvents event = switch (result) {
            case PASSED -> HostOrderEvents.TEST_ITEM_PASSED;
            case CANCEL -> HostOrderEvents.TEST_ITEM_CANCELLED;
            default     -> HostOrderEvents.TEST_ITEM_FAILED;
        };

        for (TbWcsHostOrderItem item : updatedItems) {
            historyLogger.logItem(host, item, prevInt, event, req.getTesterId(), req.getReason());
            if (result == QcTestStatus.FAILED && ValueUtil.isNotEmpty(eventPublisher)) {
                eventPublisher.publishQcTestItemFailed(
                        host.getEqGroupId(), host.getHostOrderKey(), item.getTestNo(), req.getReason());
            }
        }
    }

    /**
     * 헤더 단위 감사 로그 + 부적합 이벤트 발행. 이미 TEST_FAILED 였으면 중복 기록 방지.
     */
    private void logHeaderHistory(TbWcsHostOrder host, Aggregate aggregate,
                                  Integer prevInt, String prev, QcTestResultRequest req) {
        if (aggregate.allPassed()) {
            historyLogger.log(host, prevInt, HostOrderEvents.TEST_PASSED,
                    req.getTesterId(), req.getReason());
            return;
        }
        if (aggregate.anyFailed() && !wasAlreadyTestFailed(prev)) {
            historyLogger.log(host, prevInt, HostOrderEvents.TEST_FAILED,
                    req.getTesterId(), req.getReason());
            if (ValueUtil.isNotEmpty(eventPublisher)) {
                eventPublisher.publishTestFailed(host.getEqGroupId(), host.getHostOrderKey(), req.getReason());
            }
            return;
        }
        // 취소만 있고 부적합 없음 — 헤더 취소 이벤트 (운영자 보류)
        if (aggregate.anyCancel()) {
            historyLogger.log(host, prevInt, HostOrderEvents.TEST_CANCELLED,
                    req.getTesterId(), req.getReason());
        }
    }

    /**
     * 헤더 test_* 필드 in-memory 갱신 (order_status 는 별도 marker 호출).
     */
    private void fillHeaderTestFields(TbWcsHostOrder host, Aggregate aggregate, Date now) {
        QcTestStatus next = aggregate.toTestStatus();
        Date resultedAt = resolveResultedAt(next, aggregate, now);

        host.setTestStatus(next.code());
        host.setTestResultedAt(resultedAt);
        host.setTestReason(next == QcTestStatus.FAILED ? aggregate.firstFailReason() : null);
    }

    /**
     * testResultedAt 결정 — REQUESTED 면 null, 그 외엔 item 중 최신값 또는 now.
     */
    private static Date resolveResultedAt(QcTestStatus next, Aggregate aggregate, Date now) {
        if (next == QcTestStatus.REQUESTED) return null;
        return ValueUtil.isNotEmpty(aggregate.latestResultedAt()) ? aggregate.latestResultedAt() : now;
    }

    /**
     * item 리스트 → Aggregate 집계 (passed/failed/requested 카운트 + 최신 결과시각 + 첫 실패사유).
     */
    private static Aggregate aggregate(List<TbWcsHostOrderItem> items) {
        if (ValueUtil.isEmpty(items)) return Aggregate.empty();

        int passed = 0;
        int failed = 0;
        int requested = 0;
        int cancelled = 0;
        Date latest = null;
        String firstFailReason = null;

        for (TbWcsHostOrderItem item : items) {
            QcTestStatus status = QcTestStatus.from(item.getTestStatus());
            switch (status) {
                case PASSED -> passed++;
                case FAILED -> {
                    failed++;
                    if (firstFailReason == null) firstFailReason = item.getTestReason();
                }
                case REQUESTED -> requested++;
                case CANCEL -> cancelled++;
                default -> { /* no-op */ }
            }
            latest = laterOf(latest, item.getTestResultedAt());
        }
        return new Aggregate(items.size(), passed, failed, requested, cancelled, latest, firstFailReason);
    }

    /**
     * 시험 부적합 확정 시 사전 발행된 재입고 INBOUND shuttle (CREATED/SENT) 을 CANCELLED 로 전환.
     */
    private void cancelOpenReinboundShuttles(String hostOrderKey, String reason) {
        if (ValueUtil.isEmpty(hostOrderKey)) return;
        List<TbWcsShuttleOrder> shuttles = shuttleOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(shuttles)) return;

        Integer created = ShuttleOrderStatus.CREATED.codeAsIntOrNull();
        Integer sent = ShuttleOrderStatus.SENT.codeAsIntOrNull();
        String inboundType = OrderType.INBOUND.codeAsString();

        for (TbWcsShuttleOrder shuttle : shuttles) {
            if (!inboundType.equalsIgnoreCase(shuttle.getOrderType())) continue;
            // parent_order_key 가 있어야 재입고 (초기 입고는 제외)
            if (ValueUtil.isEmpty(shuttle.getParentOrderKey())) continue;
            Integer status = shuttle.getOrderStatus();
            if (!(created.equals(status) || sent.equals(status))) continue;
            shuttleOrderStateWriter.markCancelled(shuttle);
            logger.info("[ Qctest ][ Reinbound ] auto cancelled - host={}, shuttle={}, parent={}, reason={}",
                    hostOrderKey, shuttle.getOrderKey(), shuttle.getParentOrderKey(), reason);
        }
    }

    /**
     * item 의 test_* 필드 in-memory 갱신. requestedAt 이 null 이면 기존값 유지.
     */
    private void fillTestFields(TbWcsHostOrderItem item, QcTestStatus status,
                                Date requestedAt, Date resultedAt, String reason) {
        item.setTestStatus(status.code());
        if (ValueUtil.isNotEmpty(requestedAt)) item.setTestRequestedAt(requestedAt);
        item.setTestResultedAt(resultedAt);
        item.setTestReason(reason);
    }

    /**
     * 이전 상태가 TEST_FAILED 였는지 확인.
     */
    private boolean wasAlreadyTestFailed(String prev) {
        Integer pi = parseStatus(prev);
        return ValueUtil.isNotEmpty(pi) && pi == HostOrderStatus.TEST_FAILED.code();
    }

    /**
     * 상태 문자열 → Integer. 실패 시 null.
     */
    private Integer parseStatus(String status) {
        try { return Integer.valueOf(status); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * 두 Date 중 더 최근 값. 한쪽이 비면 다른 쪽 반환.
     */
    private static Date laterOf(Date a, Date b) {
        if (ValueUtil.isEmpty(a)) return b;
        if (ValueUtil.isEmpty(b)) return a;
        return b.after(a) ? b : a;
    }

    /**
     * 시험 item 들의 상태 분포 집계 — 헤더 test_status 결정에 사용.
     */
    private record Aggregate(
            int total, int passedCount, int failedCount, int requestedCount, int cancelledCount,
            Date latestResultedAt, String firstFailReason
    ) {
        static Aggregate empty() {
            return new Aggregate(0, 0, 0, 0, 0, null, null);
        }

        boolean allPassed() { return total > 0 && passedCount == total; }
        boolean anyFailed() { return failedCount > 0; }
        boolean anyCancel() { return cancelledCount > 0; }

        /**
         * 헤더 test_status 결정 — 우선순위: FAILED > CANCEL > REQUESTED > PASSED.
         * CANCEL/REQUESTED 가 남아있으면 allPassed 가 아니므로 주문은 비종결로 유지된다.
         */
        QcTestStatus toTestStatus() {
            if (total == 0)   return QcTestStatus.PASSED;
            if (anyFailed())  return QcTestStatus.FAILED;
            if (anyCancel())  return QcTestStatus.CANCEL;
            if (allPassed())  return QcTestStatus.PASSED;
            return QcTestStatus.REQUESTED;
        }

        String summary() {
            return "total=%d,passed=%d,failed=%d,requested=%d,cancelled=%d"
                    .formatted(total, passedCount, failedCount, requestedCount, cancelledCount);
        }
    }
}
