package operato.logis.ecs.base.ecs.scheduler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.service.conveyor.ConveyorPlcReadService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Getter
public class PlcConveyorReadScheduler {

    public String id;
    private ThreadPoolTaskScheduler taskScheduler;
    private ConveyorPlcManager cvPlcManager;
    private ConveyorPlcReadService cvPlcReadService;
    private ConveyorPlc cvPlc;

    public PlcConveyorReadScheduler(ThreadPoolTaskScheduler taskScheduler, ConveyorPlcManager cvPlcManager, ConveyorPlc cvPlc) {
        this.taskScheduler = taskScheduler;
        this.cvPlcManager = cvPlcManager;
        this.cvPlcReadService = new ConveyorPlcReadService();
        this.cvPlc = cvPlc;
        this.id = cvPlc.getId();
    }

    private ScheduledFuture<?> scheduledTask;

    public void startScheduler(int millSeconds) {
        if (scheduledTask == null || scheduledTask.isCancelled())
            scheduledTask = taskScheduler.schedule(
                    this::runTask,
                    new PeriodicTrigger(Duration.ofMillis(millSeconds)));
    }

    public void stopScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled())
            scheduledTask.cancel(true);
    }

    private void runTask() {
        // log.info("[PlcConveyorReadScheduler :{}] runTask()", cvPlc.getId());
        runTaskForConveyor();
    }

    // plc read 서비스
    private void runTaskForConveyor() {
        cvPlcReadService.readConveyorMemory(cvPlc);
        // cvPlcReadService.logInfo(cvPlc);
        cvPlcReadService.updatePlcStatus(cvPlc);
    }
}