package operato.logis.ecs.base.ecs.service.crane;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.crane.StackerCranePlcManager;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StackerCranePlcWriteService {

    private final StackerCranePlcManager stackerCranePlcManager;

    public StackerCranePlcWriteService(StackerCranePlcManager stackerCranePlcManager) {
        this.stackerCranePlcManager = stackerCranePlcManager;
    }

    public void sendCommandCrane(String craneId, MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int[] command) {
        StackerCranePlc cranePlc = stackerCranePlcManager.getEquipment(craneId);
        try {
            cranePlc.writeWord(deviceCode, firstDeviceCode, command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
