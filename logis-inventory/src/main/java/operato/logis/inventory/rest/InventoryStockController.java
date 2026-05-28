package operato.logis.inventory.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.dto.OutboundStockRequestDto;
import operato.logis.inventory.entity.TbInventoryStock;
import operato.logis.inventory.service.InventoryOutboundStockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/inventory_stock")
public class InventoryStockController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbInventoryStock.class;
    }

    private final InventoryOutboundStockService inventoryOutboundStockService;

    @RequestMapping(value="/calculate_outbound_stock", method= RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Inventory 모듈 출고 재고 추천 로직 실행 API")
    public List<TbInventoryStock> calculateOutboundStock(@RequestBody OutboundStockRequestDto requestParam) {
        return inventoryOutboundStockService.calculateOutboundStock(requestParam);
    }
}