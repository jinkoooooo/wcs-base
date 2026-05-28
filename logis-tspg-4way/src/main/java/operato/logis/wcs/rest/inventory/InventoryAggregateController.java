package operato.logis.wcs.rest.inventory;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.inventory.InventoryAggregateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * 재고 집계 현황 (BUSINESS303) API.
 *
 * ★ 페이징 규약: InboundResult / InboundSummary 와 동일하게 query/sort/page/limit 파라미터를 받고
 *   { total, items } 형식으로 응답한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/wcs/inventory/aggregate", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "재고 집계 현황 API")
public class InventoryAggregateController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryAggregateController.class);

    private final InventoryAggregateService inventoryAggregateService;

    /** 재고 집계 현황 (페이징). 기본 URL(/summary) 은 유지. */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "재고 집계 현황 조회 (페이징)")
    public Map<String, Object> aggregate(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort",  required = false) String sort,
            @RequestParam(name = "page",  defaultValue = "1")  int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Inventory ][ Aggregate ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return inventoryAggregateService.search(query, sort, page, limit);
    }

    @GetMapping("/item-lookup")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> lookupItems(@RequestParam(value = "keyword", required = false) String keyword) {
        return this.inventoryAggregateService.lookupItems(keyword);
    }
}
