package operato.logis.ecs.tspg4way.service;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class TspgConveyorPlcWriteService {

    private final TspgConveyorPlcRegistry tspgConveyorPlcRegistry;

    public TspgConveyorPlcWriteService(TspgConveyorPlcRegistry tspgConveyorPlcRegistry) {
        this.tspgConveyorPlcRegistry = tspgConveyorPlcRegistry;
    }


    public void sendCommandConveyor(String cvId, MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int[] command){
        TspgConveyorPlc cv = tspgConveyorPlcRegistry.getEquipment(cvId);
        try {
            cv.writeWord(deviceCode, firstDeviceCode, command);
            //   new int[]{workId, destRow, destBay, destLayer}
            //   new int [] {MelsecParser.buildWordFromBits(ConveyorWriteConsts.ConveyorWorkStatus.DEST_UPDATE_COMPLETE.getBitIndex())}
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}