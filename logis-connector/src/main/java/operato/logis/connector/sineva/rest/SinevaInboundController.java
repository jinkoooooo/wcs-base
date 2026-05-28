package operato.logis.connector.sineva.rest;

import operato.logis.connector.sineva.event.SinevaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.util.FormatUtil;

import java.util.Map;

/**
 * Sineva에서 들어오는 inbound callback 처리 컨트롤러.
 *
 * 역할:
 * - 외부 callback 수신
 * - 내부 이벤트 발행
 * - 즉시 성공 응답 반환
 */
@RestController
@RequestMapping("/rest/tbecsamhstask")
public class SinevaInboundController {

    private static final Logger logger = LoggerFactory.getLogger(SinevaInboundController.class);

    private final ApplicationEventPublisher eventPublisher;

    public SinevaInboundController(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Robot callback 수신
     *
     * URL Example:
     * POST /rest/tbecsamhstask/{domainId}/robot/callback
     */
    @PostMapping(value = "/{domainId}/robot/callback", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Robot Call Back")
    public Map<String, Object> callBackProcess(
            @PathVariable("domainId") Long domainId,
            @RequestBody Map<String, Object> ifData
    ) {
        logger.info("[Robot Callback] domainId={}, payload={}", domainId, FormatUtil.toJsonString(ifData));

        // 내부 이벤트 발행
        final SinevaEvent event = new SinevaEvent("callback", domainId, ifData);
        eventPublisher.publishEvent(event);

        // 호출 측에는 성공 응답 반환
        return AnyValueUtil.newMap("code,msg", "200", "successful");
    }
}