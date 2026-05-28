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
import operato.logis.asrs.core.stock.StockOutboundCore;
import operato.logis.asrs.dto.request.FullOutboundRequest;
import operato.logis.asrs.dto.request.PartialOutboundRequest;
import operato.logis.asrs.dto.request.ReturnInboundRequest;
import operato.logis.asrs.dto.response.StockCommandResult;
import operato.logis.asrs.dto.response.StockOutboundResult;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 재고 출고/재입고 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/stocks")
@ServiceDesc(description = "AisleCore Stock Outbound API")
public class AcStockOutboundController {

    private final StockOutboundCore stockOutboundCore;

    /**
     * 부분출고.
     *
     * @param request 부분출고 요청
     * @return 처리 결과
     */
    @RequestMapping(
            value = "/partial-out",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Partial outbound stock unit")
    public StockOutboundResult partialOutbound(@RequestBody PartialOutboundRequest request) {
        return stockOutboundCore.partialOutbound(request);
    }

    /**
     * 전체출고.
     *
     * @param request 전체출고 요청
     * @return 처리 결과
     */
    @RequestMapping(
            value = "/full-out",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Full outbound stock unit")
    public StockOutboundResult fullOutbound(@RequestBody FullOutboundRequest request) {
        return stockOutboundCore.fullOutbound(request);
    }

    /**
     * 재입고.
     *
     * @param request 재입고 요청
     * @return 처리 결과
     */
    @RequestMapping(
            value = "/return-in",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Return inbound stock unit")
    public StockCommandResult returnInbound(@RequestBody ReturnInboundRequest request) {
        return stockOutboundCore.returnInbound(request);
    }
}