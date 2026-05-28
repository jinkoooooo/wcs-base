package operato.logis.ecs.tspg4way.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import operato.logis.ecs.tspg4way.service.TspgOrderCompleteService;
import operato.logis.ecs.tspg4way.service.TspgOrderCreateService;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class TspgEqOrderScheduler {
    private final ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;
    private final TspgOrderCreateService createOrderService;
    private final TspgOrderCompleteService completeOrderService;

    public TspgEqOrderScheduler(ThreadPoolTaskScheduler taskScheduler, TspgOrderCreateService tspgOrderCreateService, TspgOrderCompleteService tspgOrderCompleteService) {
        this.taskScheduler = taskScheduler;
        this.createOrderService = tspgOrderCreateService;
        this.completeOrderService = tspgOrderCompleteService;
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
        // log.info("[TspgCreateEqOrderScheduler] runTask()");
        runTaskForShuttle();
    }

    private void runTaskForShuttle() {
        createOrderService.createOrder();
        completeOrderService.completeMoveRouteOrder();
        completeOrderService.completeOrder();
    }
}
