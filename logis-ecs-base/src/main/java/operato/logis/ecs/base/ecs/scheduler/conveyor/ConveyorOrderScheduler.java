package operato.logis.ecs.base.ecs.scheduler.conveyor;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.service.conveyor.ConveyorOrderService;
import operato.logis.ecs.base.ecs.service.conveyor.ConveyorPlcWriteService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public class ConveyorOrderScheduler {

    private ScheduledFuture<?> scheduledTask;
    private ThreadPoolTaskScheduler taskScheduler;
    private ConveyorOrderService cvOrderService;

    public ConveyorOrderScheduler(ThreadPoolTaskScheduler taskScheduler, ConveyorPlcManager cvPlcManager, ConveyorPlcWriteService cvPlcWriteService, String cvEqId) {
        this.taskScheduler = taskScheduler;
        cvOrderService = new ConveyorOrderService(cvPlcWriteService, cvPlcManager, cvEqId);
    }

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
        // log.info("ConveyorOrderScheduler runTask()");
        runTaskForConveyor();
    }

    // 랙별 컨베이어, 리프트 지시 관리 서비스
    private void runTaskForConveyor() {
        cvOrderService.work();
    }
}