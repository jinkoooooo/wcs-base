package operato.logis.kmat_2026.biz.ecs.tspg4way.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttleMapRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.service.TspgConveyorPlcWriteService;
import operato.logis.kmat_2026.biz.ecs.tspg4way.service.TspgShuttlePlcWriteService;

@Component
@RequiredArgsConstructor
public class TspgOrderSchedulerFactory {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TspgShuttlePlcRegistry shuttlePlcRegistry;
    private final TspgConveyorPlcRegistry conveyorPlcRegistry;
    private final TspgShuttleMapRegistry shuttleMapRegistry;
    private final TspgShuttlePlcWriteService shuttlePlcWriteService;
    private final TspgConveyorPlcWriteService conveyorPlcWriteService;

    public TspgShuttleOrderScheduler createShuttleOrderScheduler(String rackEqId, int floor) {
        return new TspgShuttleOrderScheduler(
                taskScheduler,
                shuttlePlcRegistry,
                shuttleMapRegistry,
                shuttlePlcWriteService,
                rackEqId, floor
        );
    }
    public TspgConveyorOrderScheduler createConveyorOrderScheduler(String cvEqId) {
        return new TspgConveyorOrderScheduler(
                taskScheduler,
                conveyorPlcRegistry,
                conveyorPlcWriteService,
                cvEqId
        );
    }
}
