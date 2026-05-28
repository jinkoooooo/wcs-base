package operato.logis.ecs.base.ecs.scheduler;

import lombok.RequiredArgsConstructor;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.plc.crane.StackerCranePlcManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlcReadSchedulerFactory {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final StackerCranePlcManager craneRegistry;
    private final ConveyorPlcManager conveyorRegistry;

    public PlcCraneReadScheduler create(StackerCranePlc cranePlc) {
        return new PlcCraneReadScheduler(
                taskScheduler,
                craneRegistry,
                cranePlc
        );
    }

    public PlcConveyorReadScheduler create(ConveyorPlc cvPlc) {
        return new PlcConveyorReadScheduler(
                taskScheduler,
                conveyorRegistry,
                cvPlc
        );
    }
}
