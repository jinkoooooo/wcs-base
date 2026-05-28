package operato.logis.ecs.base.ecs.plc.conveyor;

import lombok.Getter;
import operato.logis.ecs.base.ecs.domain.conveyor.ConveyorStatus;
import operato.logis.ecs.base.ecs.domain.enums.ConveyorReadConsts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ConveyorReadMap {

    private int firstDeviceCode;
    private List<Integer> wordValues;
    private Map<Integer, ConveyorStatus> conveyorMaps;

    public ConveyorReadMap() {
        conveyorMaps = new HashMap<>();
    }

    public void setReadValues(int firstDeviceCode, List<Integer> wordValues) {
        this.firstDeviceCode = firstDeviceCode;
        this.wordValues = wordValues;
        convertReadValues();
    }

    private void convertReadValues() {
        int chunkSize = 10;
        int conveyorIndex = 0;
        for (int i = 0; i < wordValues.size(); i += chunkSize) {
            conveyorIndex += 1;
            int end = Math.min(i + chunkSize, wordValues.size());
            List<Integer> cvWordValues = wordValues.subList(i, end);
            ConveyorStatus cv = new ConveyorStatus(
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.CONVEYOR_STATUS.getAddress()),
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.SIZE_STATUS.getAddress()),
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.LIFT_STATUS.getAddress()),
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.ERROR_CODE.getAddress())
            );
            conveyorMaps.put(conveyorIndex, cv);
        }
    }
}
