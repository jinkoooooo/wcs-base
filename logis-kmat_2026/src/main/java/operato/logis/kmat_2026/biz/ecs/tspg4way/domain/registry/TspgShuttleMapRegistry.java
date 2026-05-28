package operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayPathService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TspgShuttleMapRegistry {

    private ConcurrentHashMap<String, Shuttle4WayPathService> mapInfo = new ConcurrentHashMap<>();
    public Shuttle4WayPathService getMapinfo(String eqId, int floor){
        return mapInfo.get(setKey(eqId,floor));
    }
    public void setMapInfo(String eqId, int floor, Shuttle4WayPathService mapinfo){
        mapInfo.put(setKey(eqId,floor), mapinfo);
    }
    public String setKey(String eqId, int floor){
        return eqId+"_"+floor;
    }
}
