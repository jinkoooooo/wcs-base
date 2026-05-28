package operato.logis.wcs.simulator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.ecs.InternalEcsCallbackHandler;
import xyz.elidom.util.ValueUtil;

/**
 * ECS + PLC 역할 시뮬레이터. 다중 그룹 폴링 + 비동기 콜백 시퀀스 실행.
 */
@Component
public class EcsPlcSimulator {

    private static final Logger logger = LoggerFactory.getLogger(EcsPlcSimulator.class);

    private final InternalEcsCallbackHandler cbService;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final SimulatorMetrics metrics;
    private final SimulatorStateService stateService;
    private final TaskExecutor executor;

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public EcsPlcSimulator(InternalEcsCallbackHandler cbService,
                           ShuttleOrderRepository shuttleOrderRepository,
                           SimulatorMetrics metrics,
                           @Lazy SimulatorStateService stateService,
                           @Qualifier("simulatorTaskExecutor") TaskExecutor executor) {
        this.cbService = cbService;
        this.shuttleOrderRepository = shuttleOrderRepository;
        this.metrics = metrics;
        this.stateService = stateService;
        this.executor = executor;
    }

    /**
     * 1초 주기 폴링 — 활성 그룹별 SENT 오더를 콜백 시퀀스로 진행.
     */
    @Scheduled(fixedDelayString = "#{T(operato.logis.wcs.simulator.SimulatorConfig).PLC_POLL_MS}")
    public void poll() {
        if (!SimulatorConfig.ENABLED) return;

        List<String> activeGroups = stateService.findActivePlcGroups();
        if (ValueUtil.isEmpty(activeGroups)) return;

        for (String groupId : activeGroups) {
            try {
                pollGroup(groupId);
            } catch (Exception e) {
                logger.error("[ Sim ][ Plc ] poll failed - eqGroupId={}", groupId, e);
            }
        }
    }

    /**
     * 그룹별 SENT 오더를 in-flight 가드로 중복 실행 방지 후 executor 에 위임.
     */
    private void pollGroup(String groupId) {
        List<TbWcsShuttleOrder> orders = shuttleOrderRepository.findSentOrders(
                groupId, SimulatorConfig.PLC_BATCH_SIZE);
        if (ValueUtil.isEmpty(orders)) return;

        logger.debug("[ Sim ][ Plc ] polled - eqGroupId={}, count={}", groupId, orders.size());

        for (TbWcsShuttleOrder order : orders) {
            String key = order.getOrderKey();
            if (!inFlight.add(key)) continue;
            executor.execute(() -> {
                try {
                    runCallbackSequence(groupId, order);
                } finally {
                    inFlight.remove(key);
                }
            });
        }
    }

    /**
     * 콜백 시퀀스 — STARTED → FROM_LOADING → IN_PROGRESS → (INBOUND 한정 conveyorArrived) → TO_UNLOADING → COMPLETE.
     * 각 스텝 사이 PLC_STEP_DELAY_MS 만큼 sleep.
     */
    private void runCallbackSequence(String groupId, TbWcsShuttleOrder order) {
        String key  = order.getOrderKey();
        String type = order.getOrderType();
        long delay  = SimulatorConfig.PLC_STEP_DELAY_MS;

        metrics.recordPlcStart(key, type);
        try {
            cbService.started(key);              sleep(delay);
            cbService.fromLoadingComplete(key);  sleep(delay);
            cbService.inProgress(key);           sleep(delay);

            // INBOUND 만 conveyor 도착 콜백 추가
            if ("INBOUND".equalsIgnoreCase(type)) {
                cbService.conveyorArrived(key);  sleep(delay);
            }

            cbService.toUnloadingComplete(key);  sleep(delay);
            cbService.complete(key);

            metrics.recordPlcComplete(key, type);
            logger.info("[ Sim ][ Plc ] completed - eqGroupId={}, type={}, key={}", groupId, type, key);
        } catch (Exception e) {
            metrics.recordPlcFail(key, type, e.getMessage());
            logger.error("[ Sim ][ Plc ] callback failed - eqGroupId={}, orderKey={}, type={}", groupId, key, type, e);
        }
    }

    /**
     * InterruptedException 시 스레드 인터럽트 보존.
     */
    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * 그룹별 PLC 시뮬레이션 시작.
     */
    public void start(String groupId) {
        try { stateService.savePlcRunning(groupId, true); }
        catch (Exception e) { logger.error("[ Sim ][ Plc ] start failed - eqGroupId={}", groupId, e); }
        logger.info("[ Sim ][ Plc ] started - eqGroupId={}", groupId);
    }

    /**
     * 그룹별 PLC 시뮬레이션 정지.
     */
    public void stop(String groupId) {
        try { stateService.savePlcRunning(groupId, false); }
        catch (Exception e) { logger.error("[ Sim ][ Plc ] stop failed - eqGroupId={}", groupId, e); }
        logger.info("[ Sim ][ Plc ] stopped - eqGroupId={}", groupId);
    }

    public int inFlightCount() { return inFlight.size(); }
}
