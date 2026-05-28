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
import operato.logis.asrs.core.stock.StockCommandCore;
import operato.logis.asrs.dto.request.InboundStockRequest;
import operato.logis.asrs.dto.request.MoveStockRequest;
import operato.logis.asrs.dto.request.PutawayStockRequest;
import operato.logis.asrs.dto.response.StockCommandResult;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 재고 변경 API.
 *
 * <p>
 * 자동생성 TbAcStockUnitController / TbAcStockTxnController 는
 * 기본 CRUD 점검용으로 유지하고,
 * 본 Controller 는 실제 업무 시나리오용 API 로 사용한다.
 * </p>
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/stocks")
@ServiceDesc(description = "AisleCore Stock Business API")
public class AcStockCommandController {

    private final StockCommandCore stockCommandCore;

    /**
     * 재고 입고.
     *
     * @param request 입고 요청
     * @return 처리 결과
     * <p>
     * stockUnitNo / refDocNo 가 비어 있으면 backend 에서 자동 생성한다.
     * </p>
     */
    @RequestMapping(
            value = "/inbound",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Inbound stock unit")
    public StockCommandResult inbound(@RequestBody InboundStockRequest request) {
        return stockCommandCore.inbound(request);
    }

    /**
     * 재고 적치.
     *
     * @param request 적치 요청
     * @return 처리 결과
     */
    @RequestMapping(
            value = "/putaway",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Putaway stock unit")
    public StockCommandResult putaway(@RequestBody PutawayStockRequest request) {
        return stockCommandCore.putaway(request);
    }

    /**
     * 재고 이동.
     *
     * @param request 이동 요청
     * @return 처리 결과
     */
    @RequestMapping(
            value = "/move",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Move stock unit")
    public StockCommandResult move(@RequestBody MoveStockRequest request) {
        return stockCommandCore.move(request);
    }
}