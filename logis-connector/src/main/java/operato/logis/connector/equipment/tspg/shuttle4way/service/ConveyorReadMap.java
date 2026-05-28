package operato.logis.connector.equipment.tspg.shuttle4way.service;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.ConveyorReadConsts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Slf4j
public class ConveyorReadMap {
    private int firstDeviceCode;
    private List<Integer> wordValues;
    private Map<String, ConveyorStatus> conveyorMaps;

    public ConveyorReadMap() {
        conveyorMaps = new HashMap<>();
    }

    public  void  setReadValues(int firstDeviceCode, List<Integer> wordValues) {
        this.firstDeviceCode = firstDeviceCode;
        this.wordValues = wordValues;
        convertReadValues();
    }
    private void convertReadValues(){
        int chunkSize = 10;
        int conveyorIndex = 1;
        // log.info("ConveyorReadMap - convertReadValues() ");
        for (int i = 0; i < wordValues.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, wordValues.size());
            // log.info("i = " + i + ", end = " + end);
            List<Integer> cvWordValues = wordValues.subList(i, end);
            ConveyorStatus cv = new ConveyorStatus(
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.CONVEYOR_STATUS.getAddress()),
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.SIZE_STATUS.getAddress()),
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.LIFT_STATUS.getAddress()),
                    cvWordValues.get(ConveyorReadConsts.ConveyorReadAddress.ERROR_CODE.getAddress())
            );
            conveyorMaps.put(String.valueOf((100 + conveyorIndex)), cv);
            conveyorIndex += 1;
        }
    }


}
