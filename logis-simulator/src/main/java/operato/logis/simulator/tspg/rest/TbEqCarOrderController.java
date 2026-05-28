package operato.logis.simulator.tspg.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.simulator.tspg.dto.OrderUpdateRequestDto;
import operato.logis.simulator.tspg.entity.TbEqCarOrder;
import operato.logis.simulator.tspg.service.TbEqCarOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbEqCarOrder")
public class TbEqCarOrderController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbEqCarOrder.class;
    }

    private final TbEqCarOrderService tbEqCarOrderService;

    @RequestMapping(value="/getUnreadOrderList", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="미실행 작업 목록 조회 API")
    public List<TbEqCarOrder> getUnreadOrderList() {
        return tbEqCarOrderService.getUnreadOrderList();
    }

    @RequestMapping(value="/updateOrderStatus", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="작업 상태 변경 API")
    public CommonApiResponse updateOrderStatus(@RequestBody OrderUpdateRequestDto requestParam) {
        return tbEqCarOrderService.updateOrderStatus(requestParam);
    }
}