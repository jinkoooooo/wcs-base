package operato.logis.ecs.tspg4way.scheduler;

import operato.logis.ecs.tspg4way.domain.registry.TspgShuttleMapRegistry;
import operato.logis.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import operato.logis.ecs.tspg4way.service.TspgConveyorPlcWriteService;
import operato.logis.ecs.tspg4way.service.TspgShuttleOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import operato.logis.ecs.tspg4way.service.TspgShuttlePlcWriteService;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public class TspgShuttleOrderScheduler {
    private ScheduledFuture<?> scheduledTask;
    private ThreadPoolTaskScheduler taskScheduler;
    private TspgShuttleOrderService tspgShuttleOrderService;
    private int floor = 0;

    public TspgShuttleOrderScheduler(ThreadPoolTaskScheduler taskScheduler, TspgShuttlePlcRegistry tspgShuttlePlcRegistry,
                                     TspgShuttleMapRegistry tspgShuttleMapRegistry, TspgShuttlePlcWriteService tspgShuttlePlcWriteService,
                                     TspgConveyorPlcWriteService tspgConveyorPlcWriteService, String rackEqId, int floor ) {
        this.taskScheduler = taskScheduler;
        tspgShuttleOrderService = new TspgShuttleOrderService(
                tspgShuttlePlcRegistry, tspgShuttleMapRegistry, tspgShuttlePlcWriteService, tspgConveyorPlcWriteService, rackEqId, floor
                );
        this.floor = floor;
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
        // log.info("[Floor:{}] runTask()", this.floor);
        runTaskForShuttle();
    }

    // 층별 지시 관리 서비스
    private void runTaskForShuttle() {
        tspgShuttleOrderService.work();
    }
}
