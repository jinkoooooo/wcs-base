package operato.logis.inventory.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.InventoryConstants;
import operato.logis.inventory.dto.InboundLocationRequestDto;
import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.inventory.entity.TbInventoryLocation;
import operato.logis.inventory.service.InventoryInboundLocationService;
import operato.logis.inventory.service.InventoryLocationService;
import operato.logis.inventory.service.MultiDeepSortService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/inventory_location")
public class InventoryLocationController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbInventoryLocation.class;
    }

    private final MultiDeepSortService multiDeepSortService;
    private final InventoryInboundLocationService inventoryInboundLocationService;
    private final InventoryLocationService inventoryLocationService;

    @RequestMapping(value="/calculate_inbound_location", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Inventory 모듈 입고 로케이션 추천 로직 실행 API")
    public TbInventoryLocation calculateInboundLocation(@RequestBody InboundLocationRequestDto requestParam) {
        return inventoryInboundLocationService.calculateInboundLocation(requestParam);
    }

    @RequestMapping(value="/get_location_list_by_condition", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Inventory 모듈 로케이션 조회 API")
    public List<TbInventoryLocation> getLocationListByCondition(@ModelAttribute TbInventoryLocation condition) {
        return inventoryLocationService.getLocationListByCondition(condition);
    }

    @RequestMapping(value="/set_location_properties", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="로케이션 정보 수정 요청 API")
    public String setLocationProperties(@RequestBody List<TbInventoryLocation> locationList) {
        inventoryLocationService.setLocationProperties(locationList);
        return InventoryConstants.SUCCESS_MESSAGE;
    }

    @RequestMapping(value="/create_retrieval_plan", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="출고를 위한 경로 재정렬 계획 조회 API")
    public List<RelocationTaskDto> createRetrievalPlan(@RequestBody String targetLocCode) {
        return multiDeepSortService.createRetrievalPlan(targetLocCode);
    }
}