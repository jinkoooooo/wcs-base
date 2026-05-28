package operato.logis.asrs.biz.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.strategy.LocationSelectionStrategyCore;
import operato.logis.asrs.core.strategy.RelocationStrategyCore;
import operato.logis.asrs.dto.request.InboundLocationSelectRequest;
import operato.logis.asrs.dto.request.OutboundLocationSelectRequest;
import operato.logis.asrs.dto.request.StrategyRunRequest;
import operato.logis.asrs.dto.response.InboundLocationSelectResult;
import operato.logis.asrs.dto.response.OutboundLocationSelectResult;
import operato.logis.asrs.dto.response.StrategyRunResult;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 전략 및 로케이션 선택 업무 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/aislecore/strategy")
@ResponseStatus(HttpStatus.OK)
@ServiceDesc(description = "AisleCore Strategy API")
public class AcStrategyController {

    private final RelocationStrategyCore relocationStrategyCore;
    private final LocationSelectionStrategyCore locationSelectionStrategyCore;

    /**
     * 상품등급 ↔ 로케이션등급 매칭 기반 재배치 전략 실행.
     */
    @RequestMapping(
            value = "/run-grade-matching",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Run grade matching relocation strategy")
    public StrategyRunResult runGradeMatching(@RequestBody StrategyRunRequest request) {
        return relocationStrategyCore.runGradeMatchingStrategy(request);
    }

    /**
     * 입고 추천 로케이션 조회.
     */
    @RequestMapping(
            value = "/select-inbound-location",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Select inbound location candidates")
    public InboundLocationSelectResult selectInboundLocation(@RequestBody InboundLocationSelectRequest request) {
        return locationSelectionStrategyCore.selectInboundLocation(request);
    }

    /**
     * 출고 추천 재고 조회.
     */
    @RequestMapping(
            value = "/select-outbound-stocks",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Select outbound stock candidates")
    public OutboundLocationSelectResult selectOutboundStocks(@RequestBody OutboundLocationSelectRequest request) {
        return locationSelectionStrategyCore.selectOutboundStocks(request);
    }
}