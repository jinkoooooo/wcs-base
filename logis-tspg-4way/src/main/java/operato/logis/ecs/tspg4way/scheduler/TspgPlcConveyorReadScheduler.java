package operato.logis.ecs.tspg4way.scheduler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import operato.logis.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import operato.logis.ecs.tspg4way.service.TspgConveyorPlcReadService;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Getter
public class TspgPlcConveyorReadScheduler {
    public String id;
    private ThreadPoolTaskScheduler taskScheduler;
    private TspgConveyorPlcRegistry tspgConveyorPlcRegistry;
    private TspgConveyorPlcReadService tspgConveyorPlcReadService;
    private TspgConveyorPlc tspgConveyorPlc;
    public TspgPlcConveyorReadScheduler(ThreadPoolTaskScheduler taskScheduler, TspgConveyorPlcRegistry tspgConveyorPlcRegistry, TspgConveyorPlc tspgConveyorPlc ) {
        this.taskScheduler = taskScheduler;
        this.tspgConveyorPlcRegistry = tspgConveyorPlcRegistry;
        this.tspgConveyorPlcReadService = new  TspgConveyorPlcReadService();
        this.tspgConveyorPlc = tspgConveyorPlc;
        this.id = tspgConveyorPlc.getId();
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
        // log.info("[TspgConveyorPlcReadScheduler :{}] runTask()", tspgConveyorPlc.getId());
        runTaskForShuttle();
    }

    // plc read 서비스
    private void runTaskForShuttle() {
        tspgConveyorPlcReadService.readConveyorMemory(tspgConveyorPlc);
        // tspgConveyorPlcReadService.logInfo(tspgConveyorPlc);
        tspgConveyorPlcReadService.updatePlcStatus(tspgConveyorPlc);
    }
}