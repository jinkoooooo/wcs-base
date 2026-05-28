package operato.logis.connector.equipment.base.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseMelsecTypeEquipmentRegistry<K, BaseMelsecTypeEquipemnt> {
    private final ConcurrentHashMap<K, BaseMelsecTypeEquipemnt> equipmentMap = new ConcurrentHashMap<>();

    public void registerEquipment(K id, BaseMelsecTypeEquipemnt BaseMelsecTypeEquipemnt) {
        equipmentMap.put(id, BaseMelsecTypeEquipemnt);
    }

    public BaseMelsecTypeEquipemnt getEquipment(K id) {
        return equipmentMap.get(id);
    }

    public Collection<BaseMelsecTypeEquipemnt> getAllEquipment() {
        return equipmentMap.values();
    }

}


