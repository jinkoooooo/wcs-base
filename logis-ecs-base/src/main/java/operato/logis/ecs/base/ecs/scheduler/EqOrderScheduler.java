package operato.logis.ecs.base.ecs.scheduler;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.service.StackerCraneOrderCompleteService;
import operato.logis.ecs.base.ecs.service.StackerCraneOrderCreateService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class EqOrderScheduler {

    private final ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;
    private final StackerCraneOrderCreateService createOrderService;
    private final StackerCraneOrderCompleteService completeOrderService;

    public EqOrderScheduler(ThreadPoolTaskScheduler taskScheduler, StackerCraneOrderCreateService stackerCraneOrderCreateService, StackerCraneOrderCompleteService stackerCraneOrderCompleteService) {
        this.taskScheduler = taskScheduler;
        this.createOrderService = stackerCraneOrderCreateService;
        this.completeOrderService = stackerCraneOrderCompleteService;
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
        // log.info("[TspgCreateEqOrderScheduler] runTask()");
        runTaskForCrane();
    }

    private void runTaskForCrane() {
        createOrderService.createOrder();
        completeOrderService.completeMoveRouteOrder();
        completeOrderService.completeOrder();
    }
}
