package operato.logis.ecs.base.ecs.plc.conveyor;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.base.registry.BaseMelsecTypeEquipmentRegistry;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConveyorPlcManager extends BaseMelsecTypeEquipmentRegistry<String, ConveyorPlc> {

    public void startAll() {
        try {
            this.getAllEquipment().forEach(cv -> {
                try {
                    log.info("MovexConveyorPlcRegistry start() - MovexConveyorPlc - " + cv.getId());
                    cv.start();
                    log.info("MovexConveyorPlcRegistry start complete() - MovexConveyorPlc - " + cv.getId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw e;
        }
    }

    public void stopAll() {
        this.getAllEquipment().forEach(cv -> {
            try {
                cv.stop();
                log.info("MovexConveyorPlcRegistry stop() - MovexConveyorPlc - " + cv.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void startCv(String id) {
        ConveyorPlc movexConveyorPlc = this.getEquipment(id);
        if (movexConveyorPlc != null) {
            try {
                movexConveyorPlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 컨베이어가 존재하지 않습니다: " + id);
        }
    }

    public void stopCv(String id) {
        ConveyorPlc movexConveyorPlc = this.getEquipment(id);
        if (movexConveyorPlc != null) {
            try {
                movexConveyorPlc.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 컨베이어가 존재하지 않습니다: " + id);
        }
    }
}
