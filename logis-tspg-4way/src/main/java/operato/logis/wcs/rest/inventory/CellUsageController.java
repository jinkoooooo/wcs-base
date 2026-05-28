package operato.logis.wcs.rest.inventory;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.inventory.CellUsageService;
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
 * 셀 사용 현황 (BUSINESS305) API.
 * 보관층(level) 단위로 총/사용/빈/작업/금지 셀 개수와 사용률을 집계한다.
 *
 * ★ 페이징 규약: InboundResult / InboundSummary 와 동일하게 query/sort/page/limit 파라미터를 받고
 *   { total, items } 형식으로 응답한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/wcs/inventory/cell-usage", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "셀 사용 현황 API")
public class CellUsageController {

    private static final Logger logger = LoggerFactory.getLogger(CellUsageController.class);

    private final CellUsageService cellUsageService;

    /**
     * 셀 사용 현황 (페이징).
     * 기본 URL 매핑(/summary) 은 하위 호환을 위해 유지한다.
     */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "셀 사용 현황 조회 (페이징)")
    public Map<String, Object> getSummary(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort",  required = false) String sort,
            @RequestParam(name = "page",  defaultValue = "1")  int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Inventory ][ CellUsage ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return cellUsageService.search(query, sort, page, limit);
    }

    @GetMapping("/zones")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> getZones() {
        return this.cellUsageService.getZones();
    }
}
