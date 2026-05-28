package operato.logis.simulator.asrs.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.simulator.asrs.entity.TbSimulatorAsrsEquip;
import operato.logis.simulator.asrs.entity.TbSimulatorAsrsOrder;
import operato.logis.simulator.asrs.service.TbSimulatorAsrsOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbSimulatorAsrsOrder")
public class TbSimulatorAsrsOrderController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbSimulatorAsrsEquip.class;
    }

    private final TbSimulatorAsrsOrderService tbSimulatorAsrsOrderService;

    @RequestMapping(value="/getNewOrderList", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="신규 작업 정보 조회 API")
    public List<TbSimulatorAsrsOrder> getNewOrderList() {
        return tbSimulatorAsrsOrderService.getNewOrderList();
    }

    @RequestMapping(value="/updateOrderStatus", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="작업 상태 업데이트 API")
    public CommonApiResponse updateOrderStatus(@RequestBody TbSimulatorAsrsOrder order) {
        tbSimulatorAsrsOrderService.updateOrderStatus(order);
        return CommonApiResponse.success();
    }
}