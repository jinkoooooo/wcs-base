package operato.logis.ecs.base.ecs.scheduler;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.crane.StackerCranePlcManager;
import operato.logis.ecs.base.ecs.service.StackerCranePlcReadService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Getter
public class PlcCraneReadScheduler {

    private String id;
    private ThreadPoolTaskScheduler taskScheduler;
    private StackerCranePlcManager stackerCranePlcManager;
    private StackerCranePlcReadService stackerCranePlcReadService;
    private StackerCranePlc stackerCranePlc;

    public PlcCraneReadScheduler(ThreadPoolTaskScheduler taskScheduler, StackerCranePlcManager stackerCranePlcManager, StackerCranePlc stackerCranePlc) {
        this.taskScheduler = taskScheduler;
        this.stackerCranePlcManager = stackerCranePlcManager;
        this.stackerCranePlcReadService = new StackerCranePlcReadService();
        this.stackerCranePlc = stackerCranePlc;
        this.id = stackerCranePlc.getId();
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
        // log.info("[PlcCraneReadScheduler :{}] runTask()", stackerCranePlc.getId());
        runTaskForCrane();
    }

    // plc read 서비스
    private void runTaskForCrane() {
        stackerCranePlcReadService.readCraneMemory(stackerCranePlc);
        // stackerCranePlcReadService.logInfo(cranePlc);
        stackerCranePlcReadService.updatePlcStatus(stackerCranePlc);
    }
}