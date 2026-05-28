package operato.logis.ecs.tspg4way.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import operato.logis.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import operato.logis.ecs.tspg4way.service.TspgConveyorPlcWriteService;
import operato.logis.ecs.tspg4way.service.TspgConveyorOrderService;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public class TspgConveyorOrderScheduler {
    private ScheduledFuture<?> scheduledTask;
    private ThreadPoolTaskScheduler taskScheduler;
    private TspgConveyorOrderService tspgConveyorOrderService;

    public TspgConveyorOrderScheduler(ThreadPoolTaskScheduler taskScheduler, TspgConveyorPlcRegistry conveyorPlcRegistry, TspgConveyorPlcWriteService conveyorPlcWriteService, String conveyorEqId) {
        this.taskScheduler = taskScheduler;
        tspgConveyorOrderService = new TspgConveyorOrderService(conveyorPlcWriteService, conveyorPlcRegistry, conveyorEqId);
    }

    public void startScheduler(int millSeconds) {
        if (scheduledTask == null || scheduledTask.isCancelled())
            scheduledTask = taskScheduler.schedule(
                    this::runTask,
                    new PeriodicTrigger(Duration.ofMillis(millSeconds)) );
    }

    public void stopScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled())
            scheduledTask.cancel(true);
    }

    private void runTask() {
        // log.info("TspgConveyorOrderScheduler runTask()");
        runTaskForshuttle();
    }

    // 셔틀 랙별 컨베이어,리프트 지시 관리 서비스
    private void runTaskForshuttle() {
        tspgConveyorOrderService.work();
    }
}