package operato.logis.connector.equipment.tspg.shuttle4way.domain.models;

import operato.logis.connector.equipment.base.BaseMelsecTypeEquipemnt;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayReadMap;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayWriteMap;
import operato.logis.connector.plc.melsec.MelsecConsts;
import lombok.Getter;

import java.util.List;

@Getter
public class Tspg4WayShuttlePlc extends BaseMelsecTypeEquipemnt {

    private Tspg4WayShuttleCar car;
    private int readFirstDeviceCode;
    private MelsecConsts.DeviceCode readDeviceCode;
    private int writeFirstDeviceCode;
    private MelsecConsts.DeviceCode writeDeviceCode;

    private Shuttle4WayReadMap readMap = new Shuttle4WayReadMap();
    private Shuttle4WayWriteMap writeMap = new Shuttle4WayWriteMap();

    public Tspg4WayShuttlePlc(String id, String ip, int readPort, int writePort, MelsecConsts.InterfaceType plcType) throws Exception {
        super(id, ip, readPort, writePort, plcType);
        car = new Tspg4WayShuttleCar(id);
    }

    public Tspg4WayShuttlePlc(String id, String ip, int readPort, int writePort, MelsecConsts.InterfaceType plcType
    , int readFirstDeviceCode, MelsecConsts.DeviceCode readDeviceCode, int writeFirstDeviceCode, MelsecConsts.DeviceCode writeDeviceCode) throws Exception {
        super(id, ip, readPort, writePort, plcType);
        this.readFirstDeviceCode = readFirstDeviceCode;
        this.readDeviceCode = readDeviceCode;
        this.writeFirstDeviceCode = writeFirstDeviceCode;
        this.writeDeviceCode = writeDeviceCode;
        this.car = new Tspg4WayShuttleCar(id);
    }

    public void setReadValue(List<Integer> wordValue)
    {
        readMap.setReadValues(readFirstDeviceCode, wordValue);
    }

}
