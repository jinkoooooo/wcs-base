package operato.logis.ecs.base.ecs.scheduler.crane;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.service.crane.StackerCraneOrderService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public class StackerCraneOrderScheduler {

    private ScheduledFuture<?> scheduledTask;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final StackerCraneOrderService stackerCraneOrderService;

    private final String rackEqId;

    public StackerCraneOrderScheduler(ThreadPoolTaskScheduler taskScheduler, StackerCraneOrderService stackerCraneOrderService, String rackEqId) {
        this.taskScheduler = taskScheduler;
        this.stackerCraneOrderService = stackerCraneOrderService;
        this.rackEqId = rackEqId;
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
        runTaskForCrane();
    }

    private void runTaskForCrane() {
        StackerCraneContext context = createContext();
        stackerCraneOrderService.work(context);
    }

    private StackerCraneContext createContext() {
        int aisle1;
        int aisle2;

        // todo: rackEqId 확정
        if ("1".equals(rackEqId)) {
            aisle1 = 1;
            aisle2 = 3;
        } else {
            aisle1 = 2;
            aisle2 = 4;
        }

        // todo: 개선
        return new StackerCraneContext(null, null, new ArrayList<>(), null, rackEqId, null, aisle1, aisle2);
    }
}
