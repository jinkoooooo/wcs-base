package operato.logis.asrs.biz.stock;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.query.stock.StockAllocationQueryService;
import operato.logis.asrs.query.stock.StockQueryService;
import operato.logis.asrs.query.stock.model.StockAllocationView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 재고 할당 조회 API.
 *
 * <p>
 * 외부 조회는 business key 기준으로 제공한다.
 * </p>
 */
@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/stocks/allocations")
@ServiceDesc(description = "AisleCore Stock Allocation Query API")
public class AcStockAllocationQueryController {

    private final StockAllocationQueryService stockAllocationQueryService;
    private final StockQueryService stockQueryService;

    /**
     * 재고단위번호 기준 전체 할당 이력 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return 할당 이력 목록
     */
    @RequestMapping(
            value = "",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find allocation history by stockUnitNo")
    public List<StockAllocationView> findAllocationsByStockUnitNo(@RequestParam("stockUnitNo") String stockUnitNo) {
        return stockAllocationQueryService.findAllocationsByStockUnitNo(stockUnitNo, stockQueryService);
    }

    /**
     * 재고단위번호 기준 활성 할당 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return 활성 할당 목록
     */
    @RequestMapping(
            value = "/active",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find active allocations by stockUnitNo")
    public List<StockAllocationView> findActiveAllocationsByStockUnitNo(@RequestParam("stockUnitNo") String stockUnitNo) {
        return stockAllocationQueryService.findActiveAllocationViewsByStockUnitNo(stockUnitNo, stockQueryService);
    }

    /**
     * 참조문서번호 기준 전체 할당 이력 조회.
     *
     * @param refDocNo 참조 문서 번호
     * @param refDocType 참조 문서 유형
     * @param refLineNo 참조 문서 라인 번호
     * @return 할당 이력 목록
     */
    @RequestMapping(
            value = "/by-ref-doc",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find allocation history by refDocNo")
    public List<StockAllocationView> findAllocationsByRefDocNo(@RequestParam("refDocNo") String refDocNo,
                                                               @RequestParam(value = "refDocType", required = false) String refDocType,
                                                               @RequestParam(value = "refLineNo", required = false) String refLineNo) {
        return stockAllocationQueryService.findAllocationsByRefDocNo(refDocNo, refDocType, refLineNo);
    }

    /**
     * 참조문서번호 기준 활성 할당 조회.
     *
     * @param refDocNo 참조 문서 번호
     * @param refDocType 참조 문서 유형
     * @param refLineNo 참조 문서 라인 번호
     * @return 활성 할당 목록
     */
    @RequestMapping(
            value = "/active/by-ref-doc",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find active allocations by refDocNo")
    public List<StockAllocationView> findActiveAllocationsByRefDocNo(@RequestParam("refDocNo") String refDocNo,
                                                                     @RequestParam(value = "refDocType", required = false) String refDocType,
                                                                     @RequestParam(value = "refLineNo", required = false) String refLineNo) {
        return stockAllocationQueryService.findActiveAllocationsByRefDocNo(refDocNo, refDocType, refLineNo);
    }
}