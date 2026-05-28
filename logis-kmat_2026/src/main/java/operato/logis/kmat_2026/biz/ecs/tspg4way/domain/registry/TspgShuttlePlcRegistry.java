package operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.base.registry.BaseMelsecTypeEquipmentRegistry;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TspgShuttlePlcRegistry extends BaseMelsecTypeEquipmentRegistry<String, Tspg4WayShuttlePlc> {


    public void startAll() {
        try {
            this.getAllEquipment().forEach(shuttle -> {
                try {
                    log.info("TspgShuttlePlcRegistry start() - Tspg4WayShuttlePlc - " + shuttle.getId());
                    shuttle.start();
                    log.info("TspgShuttlePlcRegistry start complete() - Tspg4WayShuttlePlc - " + shuttle.getId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }catch (Exception e){
            throw e;
        }

    }

    public void stopAll() {
        this.getAllEquipment().forEach(shuttle -> {
            try {
                shuttle.stop();
                log.info("TspgShuttlePlcRegistry stop() - Tspg4WayShuttlePlc - " + shuttle.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void startShuttle(String id) {
        Tspg4WayShuttlePlc tspg4WayShuttlePlc = this.getEquipment(id);
        if (tspg4WayShuttlePlc != null) {
            try {
                tspg4WayShuttlePlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 셔틀이 존재하지 않습니다: " + id);
        }
    }

    public void stopShuttle(String id) {
        Tspg4WayShuttlePlc tspg4WayShuttlePlc = this.getEquipment(id);
        if (tspg4WayShuttlePlc != null) {
            try {
                tspg4WayShuttlePlc.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 셔틀이 존재하지 않습니다: " + id);
        }
    }
}
