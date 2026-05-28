package operato.logis.kmat_2026.biz.ecs.tspg4way.scheduler;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;

@Component
@RequiredArgsConstructor
public class TspgPlcReadSchedulerFactory {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TspgShuttlePlcRegistry shuttleRegistry;
    private final TspgConveyorPlcRegistry conveyorRegistry;

    public TspgPlcShuttleReadScheduler create(Tspg4WayShuttlePlc tspg4WayShuttlePlc) {
        return new TspgPlcShuttleReadScheduler(
                taskScheduler,
                shuttleRegistry,
                tspg4WayShuttlePlc
        );
    }
    public TspgPlcConveyorReadScheduler create(TspgConveyorPlc TspgConveyorPlc) {
        return new TspgPlcConveyorReadScheduler(
                taskScheduler,
                conveyorRegistry,
                TspgConveyorPlc
        );
    }
}
