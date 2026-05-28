package operato.logis.ecs.base.ecs.plc.crane;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.service.path.StackerCranePathService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

// todo: 로직 검토
@Component
@Slf4j
public class StackerCraneMapManager {

    private ConcurrentHashMap<String, StackerCranePathService> mapInfo = new ConcurrentHashMap<>();

    public StackerCranePathService getMapinfo(String eqId, int asiel1, int asiel2) {
        return mapInfo.get(setKey(eqId, asiel1, asiel2));
    }

    public void setMapInfo(String eqId, int asiel1, int asiel2, StackerCranePathService mapinfo) {
        mapInfo.put(setKey(eqId, asiel1, asiel2), mapinfo);
    }

    public String setKey(String eqId, int asiel1, int asiel2) {
        return eqId + "_" + asiel1 + "_" + asiel2;
    }
}