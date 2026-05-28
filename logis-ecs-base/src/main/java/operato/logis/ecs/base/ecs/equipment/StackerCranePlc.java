package operato.logis.ecs.base.ecs.equipment;

import lombok.Getter;
import operato.logis.connector.equipment.base.BaseMelsecTypeEquipemnt;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.base.ecs.domain.crane.StackerCrane;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneReadMap;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneWriteMap;

import java.util.List;

@Getter
public class StackerCranePlc extends BaseMelsecTypeEquipemnt {

    private StackerCrane crane;
    private int readFirstDeviceCode;
    private MelsecConsts.DeviceCode readDeviceCode;
    private int writeFirstDeviceCode;
    private MelsecConsts.DeviceCode writeDeviceCode;

    private StackerCraneReadMap readMap = new StackerCraneReadMap();
    private StackerCraneWriteMap writeMap = new StackerCraneWriteMap();

    public StackerCranePlc(String id, String ip, int readPort, int writePort, MelsecConsts.InterfaceType plcType) throws Exception {
        super(id, ip, readPort, writePort, plcType);
        crane = new StackerCrane(id);
    }

    public StackerCranePlc(String id, String ip, int readPort, int writePort, MelsecConsts.InterfaceType plcType,
                           int readFirstDeviceCode, MelsecConsts.DeviceCode readDeviceCode,
                           int writeFirstDeviceCode, MelsecConsts.DeviceCode writeDeviceCode) throws Exception {
        super(id, ip, readPort, writePort, plcType);
        this.readFirstDeviceCode = readFirstDeviceCode;
        this.readDeviceCode = readDeviceCode;
        this.writeFirstDeviceCode = writeFirstDeviceCode;
        this.writeDeviceCode = writeDeviceCode;
        this.crane = new StackerCrane(id);
    }

    public void setReadValue(List<Integer> wordValue) {
        readMap.setReadValues(readFirstDeviceCode, wordValue);
    }
}
