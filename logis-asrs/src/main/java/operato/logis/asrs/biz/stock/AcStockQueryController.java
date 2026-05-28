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
import operato.logis.asrs.query.stock.StockQueryService;
import operato.logis.asrs.query.stock.model.CurrentStockView;
import operato.logis.asrs.query.stock.model.StockTxnHistoryView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 재고 조회 API.
 *
 * <p>
 * 외부 조회는 business key 기준으로 제공한다.
 * </p>
 */
@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/stocks")
@ServiceDesc(description = "AisleCore Stock Query API")
public class AcStockQueryController {

    private final StockQueryService stockQueryService;

    /**
     * 재고단위번호 기준 현재고 단건 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return 현재고 단건
     */
    @RequestMapping(
            value = "/current",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find current stock by stockUnitNo")
    public CurrentStockView findCurrentByStockUnitNo(@RequestParam("stockUnitNo") String stockUnitNo) {
        return stockQueryService.findCurrentStockByStockUnitNo(stockUnitNo);
    }

    /**
     * 영역코드 + 품목코드 기준 현재고 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @return 현재고 목록
     */
    @RequestMapping(
            value = "/current/by-item",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find current stocks by areaCode and itemCode")
    public List<CurrentStockView> findCurrentByItemCode(@RequestParam("areaCode") String areaCode,
                                                        @RequestParam("itemCode") String itemCode) {
        return stockQueryService.findCurrentStocksByItemCode(areaCode, itemCode);
    }

    /**
     * 영역코드 + 로케이션코드 기준 현재고 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param locationCode 로케이션 코드
     * @return 현재고 목록
     */
    @RequestMapping(
            value = "/current/by-location",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find current stocks by areaCode and locationCode")
    public List<CurrentStockView> findCurrentByLocationCode(@RequestParam("areaCode") String areaCode,
                                                            @RequestParam("locationCode") String locationCode) {
        return stockQueryService.findCurrentStocksByLocationCode(areaCode, locationCode);
    }

    /**
     * 재고단위번호 기준 이력 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return 이력 목록
     */
    @RequestMapping(
            value = "/history",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find stock transaction history by stockUnitNo")
    public List<StockTxnHistoryView> findHistoryByStockUnitNo(@RequestParam("stockUnitNo") String stockUnitNo) {
        return stockQueryService.findTxnHistoryByStockUnitNo(stockUnitNo);
    }

    /**
     * 참조문서번호 기준 이력 조회.
     *
     * @param refDocNo 참조 문서 번호
     * @return 이력 목록
     */
    @RequestMapping(
            value = "/history/by-ref-doc",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find stock transaction history by refDocNo")
    public List<StockTxnHistoryView> findHistoryByRefDocNo(@RequestParam("refDocNo") String refDocNo) {
        return stockQueryService.findTxnHistoryByRefDocNo(refDocNo);
    }

    /**
     * 출고 대상 현재고 전체 조회.
     *
     * 규칙:
     * - 조건이 없더라도 현재고 전체를 조회할 수 있도록 제공
     * - 기본적으로 activeYn='Y', OUT 제외 조회를 권장
     *
     * @param areaCode 영역 코드(선택)
     * @param activeYn 사용 여부(선택)
     * @param excludeOutYn OUT 상태 제외 여부(Y/N, 선택)
     * @return 현재고 목록
     */
    @RequestMapping(
            value = "/current/all",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find current stocks for outbound work")
    public List<CurrentStockView> findCurrentStocks(
            @RequestParam(name = "areaCode", required = false) String areaCode,
            @RequestParam(name = "activeYn", required = false) String activeYn,
            @RequestParam(name = "excludeOutYn", required = false) String excludeOutYn) {
        return stockQueryService.findCurrentStocks(areaCode, activeYn, excludeOutYn);
    }
}