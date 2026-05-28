package operato.logis.wcs.rest.outbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.outbound.OutboundInstructionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * 출고지시현황 REST Controller
 *
 * - GET  /rest/wcs/outbound/instruction           : 출고지시 내역 조회 (query/sort/page/limit)
 * - POST /rest/wcs/outbound/instruction/register  : 출고 지시 등록 (WCS Facade 호출)
 * - POST /rest/wcs/outbound/instruction/delete    : 출고지시 내역 삭제
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/outbound/instruction")
@ServiceDesc(description = "OutboundInstruction Service API")
public class OutboundInstructionController {

    private static final Logger logger = LoggerFactory.getLogger(OutboundInstructionController.class);

    private final OutboundInstructionService outboundInstructionService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "출고지시현황 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Outbound ][ Instruction ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return outboundInstructionService.search(query, sort, page, limit);
    }

    /**
     * 출고 지시 등록
     * - body: { eqGroupId, ownerCode, items: [{itemCode, lotNo, qty, uom}] }
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "출고 지시 등록")
    public Map<String, Object> register(@RequestBody Map<String, Object> requestData) {
        logger.info("[ Outbound ][ Instruction ] register - {}", requestData);
        return outboundInstructionService.register(requestData);
    }

    /**
     * 출고지시 내역 삭제
     * - body: { hostOrderKeys: ["UI-OUT-xxx", ...] }
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "출고지시 내역 삭제")
    @SuppressWarnings("unchecked")
    public Map<String, Object> delete(@RequestBody Map<String, Object> requestData) {
        List<String> hostOrderKeys = (List<String>) requestData.get("hostOrderKeys");
        logger.info("[ Outbound ][ Instruction ] delete - {}", hostOrderKeys);
        return outboundInstructionService.deleteByHostOrderKeys(hostOrderKeys);
    }
}
