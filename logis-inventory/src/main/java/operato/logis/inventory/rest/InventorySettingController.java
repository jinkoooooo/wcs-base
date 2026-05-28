package operato.logis.inventory.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.InventoryConstants;
import operato.logis.inventory.entity.TbInventorySetting;
import operato.logis.inventory.service.InventorySettingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/inventory_setting")
public class InventorySettingController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbInventorySetting.class;
    }

    private final InventorySettingService inventorySettingService;

    @RequestMapping(value="/get_total_setting", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Inventory 모듈 전체 로케이션 설정 조회 API")
    public List<TbInventorySetting> getTotalSetting() {
        return inventorySettingService.getTotalSetting();
    }

    @RequestMapping(value="/set_option_value", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Inventory 모듈 전체 로케이션 설정 조회 API")
    public String setOptionValue(@RequestBody List<TbInventorySetting> optionList) {
        inventorySettingService.setOptionValue(optionList);
        return InventoryConstants.SUCCESS_MESSAGE;
    }
}