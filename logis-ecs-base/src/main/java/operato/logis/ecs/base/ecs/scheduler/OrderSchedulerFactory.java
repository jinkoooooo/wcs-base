package operato.logis.ecs.base.ecs.scheduler;

import lombok.RequiredArgsConstructor;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.scheduler.conveyor.ConveyorOrderScheduler;
import operato.logis.ecs.base.ecs.scheduler.crane.StackerCraneOrderScheduler;
import operato.logis.ecs.base.ecs.service.conveyor.ConveyorPlcWriteService;
import operato.logis.ecs.base.ecs.service.crane.StackerCraneOrderService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderSchedulerFactory {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final StackerCraneOrderService stackerCraneOrderService;
    private final ConveyorPlcManager conveyorPlcManager;
    private final ConveyorPlcWriteService conveyorPlcWriteService;

    public StackerCraneOrderScheduler createShuttleOrderScheduler(String rackEqId, int floor) {
        return new StackerCraneOrderScheduler(taskScheduler, stackerCraneOrderService, rackEqId);
    }

    public ConveyorOrderScheduler createConveyorOrderScheduler(String cvEqId) {
        return new ConveyorOrderScheduler(
                taskScheduler,
                conveyorPlcManager,
                conveyorPlcWriteService,
                cvEqId
        );
    }
}