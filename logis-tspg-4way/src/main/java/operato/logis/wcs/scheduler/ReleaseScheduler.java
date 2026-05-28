package operato.logis.wcs.scheduler;

import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.ecs.EcsCommandSender;
import operato.logis.wcs.service.impl.allocation.port.PortLockPolicy;
import operato.logis.wcs.service.impl.allocation.port.PortService;
import operato.logis.wcs.service.impl.allocation.port.PortTrafficService;
import operato.logis.wcs.service.repository.ShuttleOrderReleaseRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import operato.logis.wcs.service.impl.system.SystemModeService;
import operato.logis.wcs.service.impl.scheduling.GaPlanCache;
import operato.logis.wcs.service.impl.scheduling.ScoringEngine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * WCS 오더 릴리즈 스케줄러 — 출고 번들 + 독립 MOVE 를 ECS 로 송신하는 핵심 진입점.
 *
 * 릴리즈 전략:
 *   - INBOUND  : BCR 스캔 시점에 EcsCommandSender.processInboundBcrScanByBarcode 만 송신
 *   - OUTBOUND : 부모 + 자식 MOVE 번들을 원자적으로 묶어 쿼터 통제 후 전송
 *   - MOVE     : 부모 없는 독립 이동 오더는 쿼터 통제 후 단독 전송
 *
 * WCS ↔ ECS 상태 분담 (ecs_if_status):
 *   - WCS 영역: READY(0) → SENDING(10) → SENT(20)
 *   - ECS 영역: SENT(20)  → ACK(30) → ...
 * 핵심: ACK(30) 은 ECS 가 직접 쓰는 값이므로 WCS 는 절대 30 을 건드리지 않는다.
 *
 * 트랜잭션 경계: 사이클 전체를 단일 트랜잭션으로 묶지 않는다 — 락 범위 폭주 방지.
 * 원자성은 단일 UPDATE CAS 쿼리와 processBundleAtomically 가 보장.
 *
 * 본 클래스는 기존 OrderReleaseScheduler / OrderReleasePipeline / PortLockManager /
 * ReleaseQuotaCalculator / OrderAgingPolicy / BundleGroup / PortAbility 를 모두 흡수했다.
 */
@Component
@RequiredArgsConstructor
public class ReleaseScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseScheduler.class);

    private static final String OT_OUTBOUND = OrderType.OUTBOUND.code();
    private static final String OT_MOVE     = OrderType.MOVE.code();
    private static final List<String> OUTBOUND_MOVE_TYPES = List.of(OT_OUTBOUND, OT_MOVE);
    private static final List<String> MOVE_ONLY_TYPES     = List.of(OT_MOVE);
    private static final int DEFAULT_PRIORITY = 5;

    @Value("${wcs.orchestrator.aging-urgency-cap-cycles:36}")
    private int agingUrgencyCapCycles;

    @Value("${wcs.orchestrator.force-release-aging-threshold:120}")
    private int forceReleaseAgingThreshold;

    @Value("${wcs.orchestrator.capacity-weight-per-shuttle:5}")
    private int capacityWeightPerShuttle;

    @Value("${wcs.orchestrator.max-release-per-level:15}")
    private int maxReleasePerLevel;

    private final ShuttleOrderReleaseRepository releaseRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final EcsCommandSender ecsCommandSender;
    private final PortService portService;
    private final PortTrafficService portTrafficService;
    private final PortLockPolicy portLockPolicy;
    private final GaPlanCache gaPlanCache;
    private final SystemModeService systemModeService;

    /**
     * 1 사이클 — 대기 그룹별 processGroup 실행. 그룹 단위 try-catch 격리.
     */
    public void runOrchestrationCycle() {
        List<String> pendingGroups = releaseRepository.fetchGroupsWithPendingOrders();
        if (ValueUtil.isEmpty(pendingGroups)) return;

        for (String eqGroupId : pendingGroups) {
            try {
                processGroup(eqGroupId);
            } catch (Exception e) {
                logger.error("[ Scheduler ][ Release ] group failed - eqGroupId={}", eqGroupId, e);
            }
        }
    }

    /**
     * 그룹 단위 처리 — 게이팅 → 포트 가용성 → 쿼터 → 번들/MOVE/대기-부모-자식 송신.
     */
    private void processGroup(String eqGroupId) {
        logger.debug("[ Scheduler ][ Release ] start - eqGroupId={}", eqGroupId);

        // 운영 모드 그룹 게이팅
        SystemModeService.GatingResult gate = systemModeService.checkGroup(eqGroupId);
        if (!gate.allowed()) {
            logger.debug("[ Scheduler ][ Release ] gated - eqGroupId={}, reason={}", eqGroupId, gate.reason());
            return;
        }

        // 포트 가용성 확인
        boolean[] abilities = releaseRepository.checkPortAbilities(eqGroupId);
        PortAbility ability = new PortAbility(abilities[0], abilities[1]);

        logger.debug("[ Scheduler ][ Release ] inbound skip - BCR scan trigger only. eqGroupId={}", eqGroupId);

        List<String> controlledTypes = ability.canOutbound() ? OUTBOUND_MOVE_TYPES : MOVE_ONLY_TYPES;

        // 레벨별 수요/공급 → 쿼터
        Map<Integer, Integer> demandByLevel = releaseRepository.countPendingOrdersByLevel(eqGroupId, controlledTypes);
        if (ValueUtil.isEmpty(demandByLevel)) return;

        Map<Integer, Integer> idleByHome = releaseRepository.countIdleShuttlesByOriginLevel(eqGroupId);
        if (ValueUtil.isEmpty(idleByHome)) {
            logger.debug("[ Scheduler ][ Release ] no idle shuttles - eqGroupId={}", eqGroupId);
            return;
        }

        Map<Integer, Integer> quotas = calculateQuotas(idleByHome, demandByLevel);

        // OUTBOUND 번들 송신 (가능 시)
        if (ability.canOutbound()) {
            releaseOutboundBundles(eqGroupId, quotas);
        }

        // 독립 MOVE 송신
        releaseStandaloneMoves(eqGroupId, quotas);

        // WAITING parent 의 obstacle children 송신 — prereq 해소용
        releaseObstacleChildrenForWaitingParents(eqGroupId, quotas);
    }

    /**
     * OUTBOUND 번들 송신 — 우선순위 점수 정렬 → 쿼터 검사 → atomic 처리.
     */
    private void releaseOutboundBundles(String eqGroupId, Map<Integer, Integer> quotaByLevel) {
        List<BundleGroup> bundles = fetchOutboundBundles(eqGroupId);
        if (ValueUtil.isEmpty(bundles)) {
            logger.debug("[ Scheduler ][ Release ] no outbound bundles - eqGroupId={}", eqGroupId);
            return;
        }

        // 점수 내림차순 정렬
        bundles.sort(Comparator.comparingDouble(BundleGroup::priorityScore).reversed()
                .thenComparing(BundleGroup::parentOrderKey));

        for (BundleGroup bundle : bundles) {
            int available = quotaByLevel.getOrDefault(bundle.level(), 0);
            int required  = bundle.requiredQuota();

            boolean hasQuota   = available >= required;
            boolean forceReady = isForceReleaseEligible(bundle);

            // 쿼터 부족 + force 미해당 → aging 증가 후 skip
            if (!hasQuota && !forceReady) {
                releaseRepository.incrementAgingCount(bundle.parentOrderKey(), eqGroupId);
                continue;
            }
            if (forceReady && !hasQuota) {
                logger.warn("[ Scheduler ][ Release ] force release - parentKey={}, required={}, available={}",
                        bundle.parentOrderKey(), required, available);
            }

            // atomic 처리
            boolean ok;
            try {
                ok = processBundleAtomically(eqGroupId, bundle);
            } catch (RuntimeException e) {
                logger.error("[ Scheduler ][ Release ] bundle failed - parentKey={}",
                        bundle.parentOrderKey(), e);
                ok = false;
            }

            if (ok) {
                quotaByLevel.put(bundle.level(), available - required);
            }
        }
    }

    /**
     * 단일 번들의 락 획득 + 전송을 한 트랜잭션으로 처리.
     *
     * 포트 락 정책은 PortLockPolicy 가 결정 — paired 재입고, partial outbound 등.
     * 단독 출고 / 시험 출고 / 수동 출고는 락 없이 통과. 입고 BCR 스캔 시점에 일괄 해제.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean processBundleAtomically(String eqGroupId, BundleGroup bundle) {
        // 포트 락 시도
        if (!tryAcquirePortLock(eqGroupId, bundle.parentOrderKey())) {
            logger.debug("[ Scheduler ][ Release ] port lock failed - parentKey={}", bundle.parentOrderKey());
            releaseRepository.incrementAgingCount(bundle.parentOrderKey(), eqGroupId);
            return false;
        }

        // CAS 락 (타 프로세스 선점 방어)
        if (!releaseRepository.lockBundleAtomically(bundle.parentOrderKey(), eqGroupId)) {
            logger.debug("[ Scheduler ][ Release ] bundle preempted - parentKey={}", bundle.parentOrderKey());
            throw new RuntimeException("Bundle CAS 락 실패 — 트랜잭션 롤백으로 포트 락 자동 해제");
        }

        // 전송
        if (!transmitBundle(bundle.parentOrderKey(), eqGroupId)) {
            throw new RuntimeException("transmitBundle 실패 — 트랜잭션 롤백");
        }

        // aging 리셋
        releaseRepository.resetAgingCount(bundle.parentOrderKey(), eqGroupId);
        logger.info("[ Scheduler ][ Release ] bundle sent - parentKey={}, childMoves={}, level={}",
                bundle.parentOrderKey(), bundle.childMoveCount(), bundle.level());
        return true;
    }

    /**
     * 번들 전송 — 자식(MOVE) 먼저 → 부모(OUTBOUND) 나중에.
     * 중간 실패 시 이미 전송된 오더들의 포트 카운터는 원복.
     * ecs_if_status 원복은 호출부 트랜잭션 롤백이 일괄 처리.
     */
    private boolean transmitBundle(String parentOrderKey, String eqGroupId) {
        List<TbWcsShuttleOrder> moves    = releaseRepository.fetchBundleOrdersByType(parentOrderKey, eqGroupId, OT_MOVE);
        List<TbWcsShuttleOrder> outbound = releaseRepository.fetchBundleOrdersByType(parentOrderKey, eqGroupId, OT_OUTBOUND);

        List<TbWcsShuttleOrder> sent = new ArrayList<>();

        // 자식 MOVE 먼저
        for (TbWcsShuttleOrder o : moves) {
            if (!sendToEcs(o)) {
                rollbackPortCounters(sent);
                return false;
            }
            sent.add(o);
        }
        // 부모 OUTBOUND
        for (TbWcsShuttleOrder o : outbound) {
            if (!sendToEcs(o)) {
                rollbackPortCounters(sent);
                return false;
            }
            sent.add(o);
        }
        return true;
    }

    /**
     * 독립 MOVE 송신 — 레벨별 쿼터 한도 내에서 CAS 락 + 전송.
     */
    @SuppressWarnings("rawtypes")
    private void releaseStandaloneMoves(String eqGroupId, Map<Integer, Integer> quotaByLevel) {
        for (Map.Entry<Integer, Integer> entry : quotaByLevel.entrySet()) {
            int level = entry.getKey();
            int limit = entry.getValue();
            if (limit <= 0) continue;

            // 후보 수집 + GA 점수 정렬 (캐시 있을 때만)
            List<Map> rows = releaseRepository.fetchStandaloneMoveCandidates(eqGroupId, level, limit);
            if (gaPlanCache.hasPlan(eqGroupId)) {
                rows = sortMovesByGaScore(eqGroupId, rows);
            }

            for (Map row : rows) {
                String orderKey = row.get("order_key").toString();

                // 타 프로세스 선점 방어
                if (!releaseRepository.lockOrderAtomically(orderKey, eqGroupId)) {
                    logger.debug("[ Scheduler ][ Release ] move preempted - orderKey={}", orderKey);
                    continue;
                }

                TbWcsShuttleOrder order = releaseRepository.fetchLockedOrder(orderKey, eqGroupId);
                if (ValueUtil.isEmpty(order)) {
                    releaseRepository.rollbackOrder(orderKey, eqGroupId);
                    continue;
                }

                if (sendToEcs(order)) {
                    quotaByLevel.put(level, quotaByLevel.get(level) - 1);
                } else {
                    releaseRepository.rollbackOrder(orderKey, eqGroupId);
                    logger.warn("[ Scheduler ][ Release ] move send failed - orderKey={}, level={}", orderKey, level);
                }
            }
        }
    }

    /**
     * WAITING OUTBOUND parent 의 obstacle child MOVE 송신.
     * parent 가 prereq 차단으로 bundle 후보에서 제외된 동안 자식들이 stuck 되지 않도록 별도 처리.
     */
    @SuppressWarnings("rawtypes")
    private void releaseObstacleChildrenForWaitingParents(String eqGroupId, Map<Integer, Integer> quotaByLevel) {
        for (Map.Entry<Integer, Integer> entry : quotaByLevel.entrySet()) {
            int level = entry.getKey();
            int limit = entry.getValue();
            if (limit <= 0) continue;

            List<Map> rows = releaseRepository.fetchObstacleChildrenForWaitingParents(eqGroupId, level, limit);
            if (ValueUtil.isEmpty(rows)) continue;

            for (Map row : rows) {
                String orderKey = row.get("order_key").toString();

                if (!releaseRepository.lockOrderAtomically(orderKey, eqGroupId)) {
                    logger.debug("[ Scheduler ][ Release ] obstacle child preempted - orderKey={}", orderKey);
                    continue;
                }

                TbWcsShuttleOrder order = releaseRepository.fetchLockedOrder(orderKey, eqGroupId);
                if (ValueUtil.isEmpty(order)) {
                    releaseRepository.rollbackOrder(orderKey, eqGroupId);
                    continue;
                }

                if (sendToEcs(order)) {
                    quotaByLevel.put(level, quotaByLevel.get(level) - 1);
                } else {
                    releaseRepository.rollbackOrder(orderKey, eqGroupId);
                    logger.warn("[ Scheduler ][ Release ] obstacle child send failed - orderKey={}, level={}", orderKey, level);
                }
            }
        }
    }

    /**
     * MOVE 후보를 ScoringEngine.computeMoveScore 기반으로 내림차순 정렬.
     */
    @SuppressWarnings("rawtypes")
    private List<Map> sortMovesByGaScore(String eqGroupId, List<Map> rows) {
        return rows.stream().sorted((a, b) -> {
            int pa = NumberUtils.toInt(a.get("priority"), DEFAULT_PRIORITY);
            int pb = NumberUtils.toInt(b.get("priority"), DEFAULT_PRIORITY);
            Double gaA = gaScoreOrNull(eqGroupId, a.get("order_key").toString());
            Double gaB = gaScoreOrNull(eqGroupId, b.get("order_key").toString());
            double sa = ScoringEngine.computeMoveScore(pa, gaA).getTotal();
            double sb = ScoringEngine.computeMoveScore(pb, gaB).getTotal();
            return Double.compare(sb, sa);
        }).collect(Collectors.toList());
    }

    /**
     * 단일 오더 ECS 송신 — 포트 카운터 +1, 실패 시 -1 으로 원복.
     */
    private boolean sendToEcs(TbWcsShuttleOrder order) {
        portTrafficService.incrementByOrder(order);
        boolean ok = ecsCommandSender.sendCommand(order);
        if (!ok) portTrafficService.decrementByOrder(order);
        return ok;
    }

    /**
     * 이미 송신된 오더들의 포트 카운터 일괄 원복.
     */
    private void rollbackPortCounters(List<TbWcsShuttleOrder> sent) {
        for (TbWcsShuttleOrder o : sent) portTrafficService.decrementByOrder(o);
    }

    /**
     * 포트 락 획득 — PortLockPolicy 가 판정. paired 재입고/partial outbound 등은 락 필요.
     * Lock key 는 항상 host_order_key. 입고 BCR 스캔 시점에 해제.
     */
    private boolean tryAcquirePortLock(String eqGroupId, String parentOrderKey) {
        TbWcsShuttleOrder parent = shuttleOrderRepository.findByOrderKey(parentOrderKey);
        if (!portLockPolicy.requiresPortLock(parent)) return true;
        return portService.tryLockForDispatch(eqGroupId, parent.getToLocCode(), parent.getHostOrderKey());
    }

    /**
     * 강제 방출 자격 — priority <= 0 (치트키) 또는 aging 임계 초과.
     */
    private boolean isForceReleaseEligible(BundleGroup bundle) {
        if (bundle.priority() <= 0) return true;
        return bundle.agingCount() > forceReleaseAgingThreshold;
    }

    /**
     * 유휴 셔틀 → 레벨별 쿼터 산출.
     *   1) 수요 없는 레벨의 유휴 셔틀을 spare pool 로 회수
     *   2) spare 셔틀을 수요 대비 가장 부족한 레벨에 1대씩 재분배
     *   3) 레벨당 maxReleasePerLevel 상한 적용
     */
    private Map<Integer, Integer> calculateQuotas(
            Map<Integer, Integer> idleByHome, Map<Integer, Integer> demandByLevel) {

        Map<Integer, Integer> shuttles = new HashMap<>(idleByHome);
        int spare = 0;

        // 수요 없는 레벨의 셔틀을 spare 로
        for (Map.Entry<Integer, Integer> e : idleByHome.entrySet()) {
            if (demandByLevel.getOrDefault(e.getKey(), 0) == 0) {
                spare += e.getValue();
                shuttles.put(e.getKey(), 0);
            }
        }

        // 결손 큰 레벨부터 1대씩 재분배
        while (spare > 0) {
            Integer pickLevel = null;
            int maxDeficit = 0;

            for (Map.Entry<Integer, Integer> e : demandByLevel.entrySet()) {
                int capacity = shuttles.getOrDefault(e.getKey(), 0) * capacityWeightPerShuttle;
                int deficit = e.getValue() - capacity;
                if (deficit > maxDeficit) {
                    maxDeficit = deficit;
                    pickLevel = e.getKey();
                }
            }
            if (ValueUtil.isEmpty(pickLevel)) break;

            shuttles.merge(pickLevel, 1, Integer::sum);
            spare--;
        }

        // 레벨별 상한 적용
        Map<Integer, Integer> quotas = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : shuttles.entrySet()) {
            if (e.getValue() > 0) {
                quotas.put(e.getKey(),
                        Math.min(e.getValue() * capacityWeightPerShuttle, maxReleasePerLevel));
            }
        }
        return quotas;
    }

    /**
     * 출고 번들 후보 → BundleGroup 매핑 + GA 점수 첨부.
     */
    @SuppressWarnings("rawtypes")
    private List<BundleGroup> fetchOutboundBundles(String eqGroupId) {
        List<Map> rows = releaseRepository.fetchOutboundBundleCandidates(eqGroupId);
        return rows.stream().map(row -> {
            String parentKey = (String) row.get("parent_order_key");
            return new BundleGroup(
                    parentKey,
                    NumberUtils.toInt(row.get("priority"), DEFAULT_PRIORITY),
                    NumberUtils.toInt(row.get("level"), 0),
                    NumberUtils.toInt(row.get("aging_count"), 0),
                    NumberUtils.toInt(row.get("child_move_count"), 0),
                    agingUrgencyCapCycles,
                    gaScoreOrNull(eqGroupId, parentKey));
        }).collect(Collectors.toList());
    }

    /**
     * 캐시에서 GA 점수 옵셔널 추출.
     */
    private Double gaScoreOrNull(String eqGroupId, String orderKey) {
        OptionalDouble opt = gaPlanCache.getScore(eqGroupId, orderKey);
        return opt.isPresent() ? opt.getAsDouble() : null;
    }

    /**
     * 그룹 내 포트의 입고/출고 가능 여부.
     */
    public record PortAbility(boolean canInbound, boolean canOutbound) {}

    /**
     * 출고 번들 그룹 — 부모 OUTBOUND + 자식 MOVE 묶음.
     * 점수 계산에 필요한 글로벌 설정값(agingUrgencyCapCycles) 을 생성 시점 스냅샷으로 주입.
     */
    public record BundleGroup(
            String parentOrderKey,
            int priority,
            int level,
            int agingCount,
            int childMoveCount,
            int agingUrgencyCapCycles,
            Double gaScore
    ) {
        public int requiredQuota() { return 1 + childMoveCount; }

        public double priorityScore() {
            return ScoringEngine.computeBundleScore(
                    priority, agingCount, agingUrgencyCapCycles, childMoveCount, gaScore
            ).getTotal();
        }
    }

    /** 숫자 변환 헬퍼. */
    static final class NumberUtils {
        static int toInt(Object v, int def) {
            return (v instanceof Number n) ? n.intValue() : def;
        }
        private NumberUtils() {}
    }
}
