package operato.logis.wcs.rest.inventory;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.inventory.StockIndividualService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;

/**
 * 개별 재고 현황 (BUSINESS304) REST Controller.
 *
 *  - GET /rest/wcs/inventory/stock-individual   : 개별 재고 목록 조회 (페이징)
 *
 * query JSON 예시 (프론트에서 JSON.stringify):
 *   [
 *     { "name": "item_code",    "operator": "like", "value": "01-01" },
 *     { "name": "item_name",    "operator": "like", "value": "대륙" },
 *     { "name": "lot_no",       "operator": "like", "value": "D/E5" },
 *     { "name": "produce_date", "operator": "eq",   "value": "2026-04-15" }
 *   ]
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/inventory/stock-individual")
@ServiceDesc(description = "StockIndividual Service API")
public class StockIndividualController {

    private static final Logger logger = LoggerFactory.getLogger(StockIndividualController.class);

    private final StockIndividualService stockIndividualService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "개별 재고 현황 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort",  required = false) String sort,
            @RequestParam(name = "page",  defaultValue = "1")  int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Inventory ][ Stock ] search - page={}, limit={}, query={}, sort={}",
                page, limit, query, sort);
        return stockIndividualService.search(query, sort, page, limit);
    }
}
