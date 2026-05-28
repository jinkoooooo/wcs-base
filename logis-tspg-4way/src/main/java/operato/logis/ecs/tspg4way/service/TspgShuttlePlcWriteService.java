package operato.logis.ecs.tspg4way.service;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TspgShuttlePlcWriteService {

    private final TspgShuttlePlcRegistry tspgShuttlePlcRegistry;


    public TspgShuttlePlcWriteService(TspgShuttlePlcRegistry tspgShuttlePlcRegistry) {
        this.tspgShuttlePlcRegistry = tspgShuttlePlcRegistry;
    }


    public void sendCommandShuttle(String shuttleId, MelsecConsts.DeviceCode deviceCode, int firstDeviceCode, int[] command){
        Tspg4WayShuttlePlc shuttle = tspgShuttlePlcRegistry.getEquipment(shuttleId);
        try {
            shuttle.writeWord(deviceCode, firstDeviceCode, command);
            //   new int[]{workId, destRow, destBay, destLayer}
            //   new int [] {MelsecParser.buildWordFromBits(ConveyorWriteConsts.ConveyorWorkStatus.DEST_UPDATE_COMPLETE.getBitIndex())}
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
