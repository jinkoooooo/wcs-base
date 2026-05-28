package operato.logis.connector.equipment.tspg.shuttle4way.domain.models;

import operato.logis.connector.equipment.base.BaseMelsecTypeEquipemnt;
import operato.logis.connector.equipment.tspg.shuttle4way.service.ConveyorReadMap;
import operato.logis.connector.equipment.tspg.shuttle4way.service.ConveyorWriteMap;
import operato.logis.connector.plc.melsec.MelsecConsts;
import lombok.Getter;

import java.util.List;

@Getter
public class TspgConveyorPlc extends BaseMelsecTypeEquipemnt {

    private int readFirstDeviceCode;
    private MelsecConsts.DeviceCode readDeviceCode;
    private int writeFirstDeviceCode;
    private MelsecConsts.DeviceCode writeDeviceCode;

    private ConveyorReadMap readMap = new ConveyorReadMap();
    private ConveyorWriteMap wirteMap = new ConveyorWriteMap();

    public int getWriteFirstDeviceCode(int cvId){
        return ((cvId - 100) * 10);
    }



    public TspgConveyorPlc(String id, String ip, int readPort, int sendPort, MelsecConsts.InterfaceType plcType,
                           int readFirstDeviceCode, MelsecConsts.DeviceCode readDeviceCode, int writeFirstDeviceCode, MelsecConsts.DeviceCode writeDeviceCode) throws Exception {
        super(id, ip, readPort, sendPort, plcType);
        this.readFirstDeviceCode = readFirstDeviceCode;
        this.readDeviceCode = readDeviceCode;
        this.writeFirstDeviceCode = writeFirstDeviceCode;
        this.writeDeviceCode = writeDeviceCode;
    }

    public void setReadValue(List<Integer> wordValue)
    {
        readMap.setReadValues(readFirstDeviceCode, wordValue);
    }
}
