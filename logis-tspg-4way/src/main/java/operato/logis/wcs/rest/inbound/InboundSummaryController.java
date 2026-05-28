package operato.logis.wcs.rest.inbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.inbound.InboundSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;

/**
 * 입고집계현황 REST Controller
 * - Elidom 표준 CRUD와 동일한 query/sort/page/limit 파라미터 구조 사용
 * - query: JSON 필터 배열 [{"name":"item_code","operator":"like","value":"01"}, ...]
 * - sort:  JSON 정렬 배열 [{"field":"item_code","ascending":true}, ...]
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/inbound/summary")
@ServiceDesc(description = "InboundSummary Service API")
public class InboundSummaryController {

    private static final Logger logger = LoggerFactory.getLogger(InboundSummaryController.class);

    private final InboundSummaryService inboundSummaryService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고집계현황 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Inbound ][ Summary ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return inboundSummaryService.search(query, sort, page, limit);
    }
}