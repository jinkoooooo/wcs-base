package operato.logis.ecs.base.ecs.service.conveyor;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConveyorPlcWriteService {

    private final ConveyorPlcManager conveyorPlcManager;

    public ConveyorPlcWriteService(ConveyorPlcManager conveyorPlcManager) {
        this.conveyorPlcManager = conveyorPlcManager;
    }

    public void sendCommandConveyor(String cvId, MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int[] command) {
        ConveyorPlc cv = conveyorPlcManager.getEquipment(cvId);
        try {
            cv.writeWord(deviceCode, firstDeviceCode, command);
            //   new int[]{workId, destRow, destBay, destLayer}
            //   new int [] {MelsecParser.buildWordFromBits(ConveyorWriteConsts.ConveyorWorkStatus.DEST_UPDATE_COMPLETE.getBitIndex())}
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}