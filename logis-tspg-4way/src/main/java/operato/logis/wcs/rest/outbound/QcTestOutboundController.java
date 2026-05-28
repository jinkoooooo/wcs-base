package operato.logis.wcs.rest.outbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.qctest.QcTestOutboundService;
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
 * 시험 대기 재고 출고 REST Controller (QcTest 흐름 일부).
 *
 * - GET  /rest/wcs/outbound/qc-test                  : INBOUND_READY(시험 대기) 재고 목록 조회 (페이징)
 * - POST /rest/wcs/outbound/qc-test/issue            : 선택 재고 시험 출고 지시
 * - GET  /rest/wcs/outbound/qc-test/ports            : 출고대(포트) 목록
 * - GET  /rest/wcs/outbound/qc-test/recommend        : 의뢰번호 추천 파렛트 미리보기 (최소 box_seq)
 * - POST /rest/wcs/outbound/qc-test/issue-recommended: 의뢰번호 추천 파렛트 출고 지시
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/outbound/qc-test")
@ServiceDesc(description = "QcTestOutbound Service API")
public class QcTestOutboundController {

    private static final Logger logger = LoggerFactory.getLogger(QcTestOutboundController.class);

    private final QcTestOutboundService qcTestOutboundService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "시험 대기 재고 목록 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Qctest ][ Outbound ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return qcTestOutboundService.search(query, sort, page, limit);
    }

    @RequestMapping(value = "/issue", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "선택 재고 시험 출고 지시")
    public Map<String, Object> issue(@RequestBody Map<String, Object> requestData) {
        logger.info("[ Qctest ][ Outbound ] issue - {}", requestData);
        return qcTestOutboundService.issueOutbound(requestData);
    }

    @RequestMapping(value = "/ports", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "출고대(포트) 목록 조회")
    public List<Map<String, Object>> ports(
            @RequestParam(name = "eqGroupId", required = false) String eqGroupId) {
        return qcTestOutboundService.listOutboundPorts(eqGroupId);
    }

    @RequestMapping(value = "/recommend", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "의뢰번호 추천 파렛트 미리보기 - 최소 box_seq 보유 파렛트 1건")
    public Map<String, Object> recommend(@RequestParam(name = "testRequestNo") String testRequestNo) {
        logger.debug("[ Qctest ][ Recommend ] preview - reqNo={}", testRequestNo);
        return qcTestOutboundService.recommendPallet(testRequestNo);
    }

    @RequestMapping(value = "/issue-recommended", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "의뢰번호 추천 파렛트 출고 지시 - 서버 추천 재계산 후 발행")
    public Map<String, Object> issueRecommended(@RequestBody Map<String, Object> body) {
        String testRequestNo = (String) body.get("testRequestNo");
        String portCode = (String) body.get("portCode");
        logger.info("[ Qctest ][ Recommend ] issue - reqNo={}, port={}", testRequestNo, portCode);
        return qcTestOutboundService.issueRecommended(testRequestNo, portCode);
    }
}
