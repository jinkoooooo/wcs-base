package operato.logis.ecs.tspg4way.scheduler;


import lombok.Getter;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import operato.logis.ecs.tspg4way.service.TspgShuttlePlcReadService;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Getter
public class TspgPlcShuttleReadScheduler {
    private String id;
    private ThreadPoolTaskScheduler taskScheduler;
    private TspgShuttlePlcRegistry tspgShuttlePlcRegistry;
    private TspgShuttlePlcReadService tspgShuttlePlcReadService;
    private Tspg4WayShuttlePlc tspg4WayShuttlePlc;

    public TspgPlcShuttleReadScheduler(ThreadPoolTaskScheduler taskScheduler, TspgShuttlePlcRegistry tspgShuttlePlcRegistry, Tspg4WayShuttlePlc tspg4WayShuttlePlc) {
        this.taskScheduler = taskScheduler;
        this.tspgShuttlePlcRegistry = tspgShuttlePlcRegistry;
        this.tspgShuttlePlcReadService = new TspgShuttlePlcReadService();
        this.tspg4WayShuttlePlc = tspg4WayShuttlePlc;
        this.id = tspg4WayShuttlePlc.getId();
    }

    private ScheduledFuture<?> scheduledTask;

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
        // log.info("[TspgShuttlePlcReadScheduler :{}] runTask()", tspg4WayShuttlePlc.getId());
        runTaskForShuttle();
    }

    // plc read 서비스
    private void runTaskForShuttle() {
        tspgShuttlePlcReadService.readShuttleMemory(tspg4WayShuttlePlc);
        // tspgShuttlePlcReadService.logInfo(tspg4WayShuttlePlc);
        tspgShuttlePlcReadService.updatePlcStatus(tspg4WayShuttlePlc);
    }
}
