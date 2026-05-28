package operato.logis.inventory.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.entity.TbInventoryEquipment;
import operato.logis.inventory.service.InventoryEquipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/inventory_equipment")
public class InventoryEquipmentController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbInventoryEquipment.class;
    }

    private final InventoryEquipmentService inventoryEquipmentService;

    @RequestMapping(value="/get_equipment_list/{equipType}", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Inventory 모듈 특정 유형 설비 조회 API")
    public List<TbInventoryEquipment> getEquipmentListByType(@PathVariable("equipType") String equipType) {
        return inventoryEquipmentService.getEquipmentListByType(equipType);
    }
}