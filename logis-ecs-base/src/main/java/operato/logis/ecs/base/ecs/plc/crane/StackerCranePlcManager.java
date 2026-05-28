package operato.logis.ecs.base.ecs.plc.crane;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.base.registry.BaseMelsecTypeEquipmentRegistry;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import org.springframework.stereotype.Component;

// 검토 중) 사용 확인 / getEquipment
@Component
@Slf4j
public class StackerCranePlcManager extends BaseMelsecTypeEquipmentRegistry<String, StackerCranePlc> {

    public void startAll() {
        try {
            this.getAllEquipment().forEach(crane -> {
                try {
                    log.info("StackerCranePlcRegistry start() - StackerCranePlc - " + crane.getId());
                    crane.start();
                    log.info("StackerCranePlcRegistry start complete() - StackerCranePlc - " + crane.getId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw e;
        }
    }

    public void stopAll() {
        this.getAllEquipment().forEach(crane -> {
            try {
                crane.stop();
                log.info("StackerCranePlcRegistry stop() - StackerCranePlc - " + crane.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void startCrane(String id) {
        StackerCranePlc movexCranePlc = this.getEquipment(id);
        if (movexCranePlc != null) {
            try {
                movexCranePlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 스태커크레인이 존재하지 않습니다: " + id);
        }
    }

    public void stopCrane(String id) {
        StackerCranePlc movexCranePlc = this.getEquipment(id);
        if (movexCranePlc != null) {
            try {
                movexCranePlc.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("해당 ID의 스태커크레인이 존재하지 않습니다: " + id);
        }
    }
}
