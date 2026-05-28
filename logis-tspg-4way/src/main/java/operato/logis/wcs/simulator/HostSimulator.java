package operato.logis.wcs.simulator;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.PortMode;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.entity.TbWcsSimulatorState;
import operato.logis.wcs.facade.Tspg4WayShuttleWcsFacade;
import operato.logis.wcs.service.impl.ecs.EcsCommandSender;
import operato.logis.wcs.service.impl.allocation.port.PortService;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * HOST(WMS) 역할 시뮬레이터.
 *
 * 정책:
 *   - 펜딩 제한 없음 — 목표 수량만큼 한 틱에 전체 일괄 생성
 *   - 입고 BCR 스캔은 별도 틱으로 1건씩 순차 송신
 *   - 출고는 IDLE 재고만 조회
 *   - 페이즈 매칭 시 created_at 밀리초 절사로 오버슈팅 방지
 */
@Component
public class HostSimulator extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(HostSimulator.class);

    /**
     * 시뮬 페이즈 — INBOUND ↔ OUTBOUND 순환, 그 사이에 SWITCHING_TO_* 가 끼어든다.
     */
    public enum Phase {
        INBOUND,
        SWITCHING_TO_OUTBOUND,
        OUTBOUND,
        SWITCHING_TO_INBOUND
    }

    private final Tspg4WayShuttleWcsFacade wcsFacade;
    private final SimulatorMetrics metrics;
    private final SimulatorStateService stateService;
    private final PortService portService;
    private final EcsCommandSender ecsCommandSender;
    private final SimStockQuery simStockQuery;
    private final SimulatorSeeder seeder;
    private final SimOrderRequestFactory requestFactory;

    private final Set<String> reservedLocIds = ConcurrentHashMap.newKeySet();
    private final AtomicLong seq = new AtomicLong(System.currentTimeMillis());

    public HostSimulator(Tspg4WayShuttleWcsFacade wcsFacade,
                         SimulatorMetrics metrics,
                         @Lazy SimulatorStateService stateService,
                         PortService portService,
                         EcsCommandSender ecsCommandSender,
                         SimStockQuery simStockQuery,
                         SimulatorSeeder seeder,
                         SimOrderRequestFactory requestFactory) {
        this.wcsFacade = wcsFacade;
        this.metrics = metrics;
        this.stateService = stateService;
        this.portService = portService;
        this.ecsCommandSender = ecsCommandSender;
        this.simStockQuery = simStockQuery;
        this.seeder = seeder;
        this.requestFactory = requestFactory;
    }

    /**
     * HOST 주문 일괄 생성 tick.
     */
    @Scheduled(fixedDelayString = "#{T(operato.logis.wcs.simulator.SimulatorConfig).HOST_INTERVAL_MS}")
    public void tick() {
        if (!SimulatorConfig.ENABLED) return;

        List<String> activeGroups = stateService.findActiveHostGroups();
        if (ValueUtil.isEmpty(activeGroups)) return;

        // 시드 SKU 일괄 등록 (최초 1회)
        try { seeder.ensureOnce(); }
        catch (Exception e) {
            logger.error("[ Sim ][ Host ] seed failed", e);
            return;
        }

        // 그룹별 tick 격리
        for (String groupId : activeGroups) {
            try { tickGroup(groupId); }
            catch (Exception e) {
                metrics.recordHostFail("UNKNOWN", "?", "EXCEPTION", e.getMessage());
                logger.error("[ Sim ][ Host ] tick failed - eqGroupId={}", groupId, e);
            }
        }
    }

    /**
     * BCR 스캔 시뮬레이션 tick — host tick 과 별개로 동작 (순차 송신용).
     */
    @Scheduled(fixedDelayString = "#{T(operato.logis.wcs.simulator.SimulatorConfig).BCR_INTERVAL_MS}")
    public void bcrScanTick() {
        if (!SimulatorConfig.ENABLED) return;

        List<String> activeGroups = stateService.findActiveHostGroups();
        if (ValueUtil.isEmpty(activeGroups)) return;

        for (String groupId : activeGroups) {
            try { tickBcrScan(groupId); }
            catch (Exception e) {
                logger.error("[ Sim ][ Host ] bcrScan failed - eqGroupId={}", groupId, e);
            }
        }
    }

    /**
     * 그룹별 페이즈 기반 분기 — 작업 페이즈 vs 모드 전환 페이즈.
     */
    private void tickGroup(String groupId) {
        TbWcsSimulatorState state = stateService.findByEqGroupId(groupId);
        if (ValueUtil.isEmpty(state)) {
            logger.debug("[ Sim ][ Host ] no state - skip. eqGroupId={}", groupId);
            return;
        }

        Phase phase = parsePhase(state.getCurrentPhase());

        switch (phase) {
            case INBOUND, OUTBOUND       -> tickWorkPhase(groupId, state, phase);
            case SWITCHING_TO_OUTBOUND   -> tickSwitchPhase(groupId, PortMode.OUTBOUND, Phase.OUTBOUND);
            case SWITCHING_TO_INBOUND    -> tickSwitchPhase(groupId, PortMode.INBOUND,  Phase.INBOUND);
        }
    }

    /**
     * 작업 페이즈 (INBOUND/OUTBOUND) — 목표/진행 계산 후 가용 자원만큼 일괄 생성.
     * 목표 도달 또는 자원 고갈 시 다음 페이즈로 전환.
     */
    private void tickWorkPhase(String groupId, TbWcsSimulatorState state, Phase phase) {
        int target = readPhaseTarget(state, phase);

        if (target <= 0) {
            advancePhase(groupId, phase, "target=0 skip");
            return;
        }

        if (!isPortModeReady(groupId, phase)) return;

        // 현재 진행상황 + 가용 자원 계산
        PhaseProgress progress = readPhaseProgress(groupId, state.getPhaseStartedAt(), phase);
        int currentSubmitted = progress.submitted;

        int feasibleCount = (phase == Phase.INBOUND) ? simStockQuery.countEmptyRacks(groupId) : simStockQuery.countSimStocks(groupId);
        int remainingToCreate = target - currentSubmitted;
        int countToCreate = Math.min(remainingToCreate, feasibleCount);

        // 일괄 생성 (FOR 루프, 실패 시 중단)
        if (countToCreate > 0) {
            logger.info("[ Sim ][ Host ] bulk create - eqGroupId={}, planned={}, remaining={}, feasible={}",
                    groupId, countToCreate, remainingToCreate, feasibleCount);

            int newlyCreated = 0;

            if (phase == Phase.INBOUND) {
                for (int i = 0; i < countToCreate; i++) {
                    if (createOneOrder(groupId, phase, target, currentSubmitted + newlyCreated)) {
                        newlyCreated++;
                    } else {
                        break;
                    }
                }
            } else if (phase == Phase.OUTBOUND) {
                // 출고: 한 번에 필요한 수의 재고 후보를 가져와 루프 처리
                List<SimStock> candidates = simStockQuery.fetchAvailable(groupId, countToCreate, reservedLocIds);
                for (SimStock pick : candidates) {
                    if (createOutboundOrder(groupId, pick, target, currentSubmitted + newlyCreated)) {
                        newlyCreated++;
                    } else {
                        break;
                    }
                }
            }

            currentSubmitted += newlyCreated;
        }

        // 전환 조건 검사 — 목표 도달 또는 자원 고갈 + 완료 카운트 따라잡힘
        boolean isDoneTarget = (currentSubmitted >= target);
        int finalFeasibleCount = (phase == Phase.INBOUND) ? simStockQuery.countEmptyRacks(groupId) : simStockQuery.countSimStocks(groupId);
        boolean isExhausted  = (finalFeasibleCount <= 0);

        if (isDoneTarget || isExhausted) {
            PhaseProgress finalProgress = readPhaseProgress(groupId, state.getPhaseStartedAt(), phase);

            if (finalProgress.completed >= currentSubmitted) {
                String reason = isDoneTarget ? "Target Reached" : "Resource Exhausted";
                logger.info("[ Sim ][ Host ] phase done - eqGroupId={}, phase={}, completed={}/{}, reason={}",
                        groupId, phase, finalProgress.completed, target, reason);
                advancePhase(groupId, phase, reason);
            }
        }
    }

    /**
     * 입고 오더 1건 생성 + 결과 메트릭 기록.
     */
    private boolean createOneOrder(String groupId, Phase phase, int target, int currentSubmitted) {
        String hostOrderKey = SimulatorConfig.SIM_HOST_KEY_PREFIX + seq.incrementAndGet();
        HostOrderApi.Request req = requestFactory.build(groupId, phase, hostOrderKey);

        if (ValueUtil.isEmpty(req)) return false;

        HostOrderApi.Response res = wcsFacade.receiveHostOrder(req);

        if (ValueUtil.isNotEmpty(res) && res.isSuccess()) {
            metrics.recordHostSubmit(phase.name(), hostOrderKey);
            if (phase == Phase.INBOUND) {
                logger.info("[ Sim ][ Host ] inbound submitted - eqGroupId={}, progress={}/{}, key={}",
                        groupId, currentSubmitted + 1, target, hostOrderKey);
            }
            return true;
        } else {
            String code = ValueUtil.isEmpty(res) ? "NULL_RES" : res.getErrorCode();
            metrics.recordHostFail(phase.name(), hostOrderKey, code, "Order creation failed");
            return false;
        }
    }

    /**
     * 출고 오더 1건 생성. 성공 시 locId 를 reservedLocIds 에 기록 (다음 후보 중복 방지).
     */
    private boolean createOutboundOrder(String groupId, SimStock pick, int target, int currentSubmitted) {
        String hostOrderKey = SimulatorConfig.SIM_HOST_KEY_PREFIX + seq.incrementAndGet();
        HostOrderApi.Request req = requestFactory.outboundFromStock(groupId, hostOrderKey, pick);

        reservedLocIds.add(pick.locId());

        HostOrderApi.Response res = wcsFacade.receiveHostOrder(req);

        if (ValueUtil.isNotEmpty(res) && res.isSuccess()) {
            metrics.recordHostSubmit("OUTBOUND", hostOrderKey);
            logger.info("[ Sim ][ Host ] outbound submitted - eqGroupId={}, progress={}/{}, key={}",
                    groupId, currentSubmitted + 1, target, hostOrderKey);
            return true;
        } else {
            reservedLocIds.remove(pick.locId());
            return false;
        }
    }

    /**
     * BCR 스캔 — 1틱당 1건만 순차 송신.
     */
    @SuppressWarnings("rawtypes")
    private void tickBcrScan(String groupId) {
        String sql = """
            SELECT order_key, barcode, from_loc_code
              FROM tb_wcs_shuttle_order
             WHERE eq_group_id   = :eqGroupId
               AND order_type    = :inboundType
               AND order_status  = :createdStatus
               AND ecs_if_status = :readyStatus
               AND barcode IS NOT NULL
               AND from_loc_code IS NOT NULL
               AND barcode LIKE :barcodePrefix
             ORDER BY priority ASC, created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,inboundType,createdStatus,readyStatus,barcodePrefix",
                groupId,
                OrderType.INBOUND.codeAsString(),
                ShuttleOrderStatus.CREATED.codeAsIntOrNull(),
                EcsIfStatus.READY.codeAsIntOrNull(),
                SimulatorConfig.SIM_BARCODE_PREFIX + "%");

        // LIMIT 1 — 순차 송신 보장
        List<Map> rows = queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return;

        Map row = rows.get(0);
        String orderKey = (String) row.get("order_key");
        String barcode = (String) row.get("barcode");
        String portCode = (String) row.get("from_loc_code");

        try {
            boolean ok = ecsCommandSender.processInboundBcrScanByBarcode(groupId, barcode, portCode);
            if (ok) {
                logger.info("[ Sim ][ Host ] bcr sent - eqGroupId={}, orderKey={}, port={}, barcode={}",
                        groupId, orderKey, portCode, barcode);
            } else {
                logger.warn("[ Sim ][ Host ] bcr send failed (will retry) - eqGroupId={}, orderKey={}, port={}",
                        groupId, orderKey, portCode);
            }
        } catch (Exception e) {
            logger.error("[ Sim ][ Host ] bcr tick failed - eqGroupId={}, orderKey={}", groupId, orderKey, e);
        }
    }

    /**
     * SWITCHING_* 페이즈 — 포트 전부 targetMode 도달 시 nextPhase 로.
     */
    private void tickSwitchPhase(String groupId, PortMode targetMode, Phase nextPhase) {
        if (allPortsAtMode(groupId, targetMode)) {
            logger.info("[ Sim ][ Host ] mode switched - eqGroupId={}, nextPhase={}", groupId, nextPhase);
            stateService.savePhase(groupId, nextPhase, new Date());
            return;
        }
        logger.debug("[ Sim ][ Host ] awaiting mode switch - eqGroupId={}, target={}", groupId, targetMode);
    }

    /**
     * 그룹의 모든 IN_OUTBOUND_PORT 에 portMode 변경 요청.
     */
    @SuppressWarnings("rawtypes")
    private void requestModeChange(String groupId, PortMode newMode) {
        String sql = """
                SELECT loc_id FROM tb_inventory_location
                 WHERE loc_group = :eqGroupId
                   AND loc_type = 'IN_OUTBOUND_PORT'
                   AND is_enabled = TRUE
                """;
        List<Map> rows = queryManager.selectListBySql(sql,
                ValueUtil.newMap("eqGroupId", groupId), Map.class, 0, 50);

        for (Map row : rows) {
            String locId = (String) row.get("loc_id");
            try {
                PortService.ChangeResult result = portService.changePortMode(
                        groupId, locId, newMode, "HOST-SIM", "phase transition");
                if (result.success()) {
                    logger.info("[ Sim ][ Host ] portMode changed - eqGroupId={}, locId={}, {} -> {}",
                            groupId, locId, result.previousMode(), result.currentMode());
                } else {
                    logger.warn("[ Sim ][ Host ] portMode change failed - eqGroupId={}, locId={}, error={}",
                            groupId, locId, result.errorDesc());
                }
            } catch (Exception e) {
                logger.error("[ Sim ][ Host ] requestModeChange failed - eqGroupId={}, locId={}",
                        groupId, locId, e);
            }
        }
    }

    /**
     * 그룹의 모든 IN_OUTBOUND_PORT 가 targetMode 로 통일됐는지.
     */
    @SuppressWarnings("rawtypes")
    private boolean allPortsAtMode(String groupId, PortMode targetMode) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM tb_inventory_location
                 WHERE loc_group = :eqGroupId
                   AND loc_type = 'IN_OUTBOUND_PORT'
                   AND is_enabled = TRUE
                   AND (port_mode IS NULL OR port_mode <> :targetMode)
                """;
        List<Map> rows = queryManager.selectListBySql(sql,
                ValueUtil.newMap("eqGroupId,targetMode", groupId, targetMode.code()),
                Map.class, 0, 1);
        return ValueUtil.isNotEmpty(rows) && toInt(rows.get(0).get("cnt")) == 0;
    }

    /**
     * 현재 페이즈에 대한 가용 포트가 1개 이상 있는지 — 작업 진행 전제.
     */
    @SuppressWarnings("rawtypes")
    private boolean isPortModeReady(String groupId, Phase phase) {
        String requiredMode = (phase == Phase.INBOUND) ? PortMode.INBOUND.code() : PortMode.OUTBOUND.code();
        String portTypeOnly = (phase == Phase.INBOUND) ? "INBOUND_PORT" : "OUTBOUND_PORT";

        String sql = """
                SELECT COUNT(*) AS cnt FROM tb_inventory_location
                 WHERE loc_group = :eqGroupId
                   AND is_enabled = TRUE
                   AND (loc_type = :portTypeOnly
                        OR (loc_type = 'IN_OUTBOUND_PORT' AND port_mode = :requiredMode))
                """;
        Map<String, Object> p = ValueUtil.newMap(
                "eqGroupId,portTypeOnly,requiredMode",
                groupId, portTypeOnly, requiredMode);
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 1);
        return ValueUtil.isNotEmpty(rows) && toInt(rows.get(0).get("cnt")) > 0;
    }

    /**
     * 다음 페이즈 결정 + 모드 전환 요청 + DB 저장.
     */
    private void advancePhase(String groupId, Phase prev, String reason) {
        // 페이즈 전환 시 출고 reserved 메모리 초기화
        reservedLocIds.clear();

        Phase next = nextPhase(prev);

        // SWITCHING_* 진입 시 포트 모드 변경 요청
        if (next == Phase.SWITCHING_TO_OUTBOUND) {
            requestModeChange(groupId, PortMode.OUTBOUND);
        } else if (next == Phase.SWITCHING_TO_INBOUND) {
            requestModeChange(groupId, PortMode.INBOUND);
        }

        stateService.savePhase(groupId, next, new Date());
        logger.info("[ Sim ][ Host ] phase transition - eqGroupId={}, {} -> {}, reason={}",
                groupId, prev, next, reason);
    }

    /**
     * 순환 룰: INBOUND → SWITCHING_TO_OUTBOUND → OUTBOUND → SWITCHING_TO_INBOUND → INBOUND.
     */
    private Phase nextPhase(Phase p) {
        return switch (p) {
            case INBOUND               -> Phase.SWITCHING_TO_OUTBOUND;
            case SWITCHING_TO_OUTBOUND -> Phase.OUTBOUND;
            case OUTBOUND              -> Phase.SWITCHING_TO_INBOUND;
            case SWITCHING_TO_INBOUND  -> Phase.INBOUND;
        };
    }

    /**
     * 페이즈별 목표 카운트 — 그룹 설정 우선, 없으면 글로벌 RATIO.
     */
    private int readPhaseTarget(TbWcsSimulatorState state, Phase phase) {
        return switch (phase) {
            case INBOUND  -> nz(state.getTargetInbound(),  SimulatorConfig.RATIO_INBOUND);
            case OUTBOUND -> nz(state.getTargetOutbound(), SimulatorConfig.RATIO_OUTBOUND);
            default       -> 0;
        };
    }

    /**
     * 페이즈 진행상황 (submitted/completed) DB 조회.
     */
    @SuppressWarnings("rawtypes")
    private PhaseProgress readPhaseProgress(String groupId, Date phaseStartedAt, Phase phase) {
        if (ValueUtil.isEmpty(phaseStartedAt)) return new PhaseProgress(0, 0);
        String expectedType = phase.name();

        String sql = """
                SELECT
                  COUNT(*) AS submitted,
                  COUNT(*) FILTER (WHERE order_status = :completedStatus) AS completed
                  FROM tb_wcs_host_order
                 WHERE host_system_code = :hsc
                   AND eq_group_id = :eqGroupId
                   AND order_type = :orderType
                """;
        Map<String, Object> p = ValueUtil.newMap(
                "completedStatus,hsc,eqGroupId,orderType",
                HostOrderStatus.COMPLETED.code(),
                SimulatorConfig.HOST_SYSTEM_CODE,
                groupId,
                expectedType);
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return new PhaseProgress(0, 0);
        Map row = rows.get(0);
        return new PhaseProgress(
                toInt(row.get("submitted")),
                toInt(row.get("completed"))
        );
    }

    public Phase getCurrentPhase(String groupId) {
        TbWcsSimulatorState s = stateService.findByEqGroupId(groupId);
        return ValueUtil.isEmpty(s) ? Phase.INBOUND : parsePhase(s.getCurrentPhase());
    }

    public int getPhaseSubmitted(String groupId) {
        TbWcsSimulatorState s = stateService.findByEqGroupId(groupId);
        if (ValueUtil.isEmpty(s)) return 0;
        Phase phase = parsePhase(s.getCurrentPhase());
        if (isSwitchingPhase(phase)) return 0;
        return readPhaseProgress(groupId, s.getPhaseStartedAt(), phase).submitted;
    }

    public int getPhaseTarget(String groupId) {
        TbWcsSimulatorState s = stateService.findByEqGroupId(groupId);
        if (ValueUtil.isEmpty(s)) return 0;
        return readPhaseTarget(s, parsePhase(s.getCurrentPhase()));
    }

    public int getPhaseCompleted(String groupId) {
        TbWcsSimulatorState s = stateService.findByEqGroupId(groupId);
        if (ValueUtil.isEmpty(s)) return 0;
        Phase phase = parsePhase(s.getCurrentPhase());
        if (isSwitchingPhase(phase)) return 0;
        return readPhaseProgress(groupId, s.getPhaseStartedAt(), phase).completed;
    }

    private static boolean isSwitchingPhase(Phase p) {
        return p == Phase.SWITCHING_TO_INBOUND || p == Phase.SWITCHING_TO_OUTBOUND;
    }

    /**
     * 시뮬 시작 — 포트가 INBOUND 모드면 바로 INBOUND, 아니면 SWITCHING_TO_INBOUND.
     */
    public void start(String groupId) {
        Phase initialPhase;
        if (allPortsAtMode(groupId, PortMode.INBOUND)) {
            initialPhase = Phase.INBOUND;
        } else {
            requestModeChange(groupId, PortMode.INBOUND);
            initialPhase = Phase.SWITCHING_TO_INBOUND;
        }
        stateService.savePhase(groupId, initialPhase, new Date());
        stateService.saveHostRunning(groupId, true);
        logger.info("[ Sim ][ Host ] started - eqGroupId={}, initialPhase={}", groupId, initialPhase);
    }

    /**
     * 부팅 복원 — 페이즈는 유지하되 포트 모드 불일치 시 SWITCHING 으로 정렬.
     */
    public void startKeepingPhase(String groupId) {
        TbWcsSimulatorState s = stateService.findByEqGroupId(groupId);
        if (ValueUtil.isEmpty(s)) {
            logger.info("[ Sim ][ Host ] start skipped - no state. eqGroupId={}", groupId);
            return;
        }

        Phase phase = parsePhase(s.getCurrentPhase());

        // 부팅 시 페이즈 vs 포트 모드 불일치 정렬
        if (phase == Phase.INBOUND && !allPortsAtMode(groupId, PortMode.INBOUND)) {
            logger.warn("[ Sim ][ Host ] boot align - phase=INBOUND but ports not INBOUND, switching. eqGroupId={}",
                    groupId);
            requestModeChange(groupId, PortMode.INBOUND);
            stateService.savePhase(groupId, Phase.SWITCHING_TO_INBOUND, new Date());
        } else if (phase == Phase.OUTBOUND && !allPortsAtMode(groupId, PortMode.OUTBOUND)) {
            logger.warn("[ Sim ][ Host ] boot align - phase=OUTBOUND but ports not OUTBOUND, switching. eqGroupId={}",
                    groupId);
            requestModeChange(groupId, PortMode.OUTBOUND);
            stateService.savePhase(groupId, Phase.SWITCHING_TO_OUTBOUND, new Date());
        } else {
            logger.info("[ Sim ][ Host ] start - keep phase. eqGroupId={}, phase={}", groupId, phase);
        }
    }

    public void stop(String groupId) {
        stateService.saveHostRunning(groupId, false);
        logger.info("[ Sim ][ Host ] stopped - eqGroupId={}", groupId);
    }

    public boolean isSeedDone() { return seeder.isDone(); }

    /**
     * 문자열 → Phase 안전 변환. 실패 시 INBOUND fallback.
     */
    private static Phase parsePhase(String s) {
        if (ValueUtil.isEmpty(s)) return Phase.INBOUND;
        try { return Phase.valueOf(s); }
        catch (IllegalArgumentException ex) { return Phase.INBOUND; }
    }

    private static int nz(Integer v, int fallback) {
        return ValueUtil.isEmpty(v) ? fallback : v;
    }

    private static int toInt(Object o) {
        return (o instanceof Number n) ? n.intValue() : 0;
    }

    /**
     * 페이즈 진행상황 record.
     */
    private record PhaseProgress(int submitted, int completed) {}

}
