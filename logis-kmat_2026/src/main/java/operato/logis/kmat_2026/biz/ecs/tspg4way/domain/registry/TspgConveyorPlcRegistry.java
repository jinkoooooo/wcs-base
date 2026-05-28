package operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.base.registry.BaseMelsecTypeEquipmentRegistry;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TspgConveyorPlcRegistry  extends BaseMelsecTypeEquipmentRegistry<String, TspgConveyorPlc> {
    public void startAll() {
        try {
            this.getAllEquipment().forEach(cv -> {
                try {
                    log.info("TspgConveyorPlcRegistry start() - TspgConveyorPlc - " + cv.getId());
                    cv.start();
                    log.info("TspgConveyorPlcRegistry start complete() - TspgConveyorPlc - " + cv.getId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }catch (Exception e){
            throw e;
        }

    }

    public void stopAll() {
        this.getAllEquipment().forEach(cv -> {
            try {
                cv.stop();
                log.info("TspgConveyorPlcRegistry stop() - TspgConveyorPlc - " + cv.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void startCv(String id) {
        TspgConveyorPlc tspgConveyorPlc = this.getEquipment(id);
        if (tspgConveyorPlc != null) {
            try {
                tspgConveyorPlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 컨베이어가 존재하지 않습니다: " + id);
        }
    }

    public void stopCv(String id) {
        TspgConveyorPlc tspgConveyorPlc = this.getEquipment(id);
        if (tspgConveyorPlc != null) {
            try {
                tspgConveyorPlc.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 컨베이어가 존재하지 않습니다: " + id);
        }
    }
}
