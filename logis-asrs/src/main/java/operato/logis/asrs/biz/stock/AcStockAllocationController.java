package operato.logis.asrs.biz.stock;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.stock.StockAllocationCore;
import operato.logis.asrs.dto.request.AllocateStockRequest;
import operato.logis.asrs.dto.request.ReleaseAllocationRequest;
import operato.logis.asrs.dto.response.StockAllocationResult;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 재고 할당 API.
 *
 * <p>
 * 외부 요청은 business key 기준(stockUnitNo, refDocNo)으로 받고,
 * 내부 처리 시 row id 로 변환한다.
 * </p>
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/stocks")
@ServiceDesc(description = "AisleCore Stock Allocation API")
public class AcStockAllocationController {

    private final StockAllocationCore stockAllocationCore;

    /**
     * 재고 할당.
     *
     * @param request 할당 요청
     * @return 처리 결과
     */
    @RequestMapping(
            value = "/allocate",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Allocate stock unit")
    public StockAllocationResult allocate(@RequestBody AllocateStockRequest request) {
        return stockAllocationCore.allocate(request);
    }

    /**
     * 재고 할당 해제.
     *
     * @param request 해제 요청
     * @return 처리 결과
     */
    @RequestMapping(
            value = "/release-allocation",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Release allocated stock unit")
    public StockAllocationResult releaseAllocation(@RequestBody ReleaseAllocationRequest request) {
        return stockAllocationCore.releaseAllocation(request);
    }
}