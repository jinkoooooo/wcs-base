package operato.logis.wcs.rest.alarm;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.alarm.ReinboundAlarmService;
import operato.logis.wcs.service.impl.alarm.ReinboundAlarmService.FollowUpAlarmResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * WCS 알람 조회 API.
 *
 * follow-up : 파렛트 기반 — shuttle_order.follow_up_since 가 set 된 재입고 미완료 파렛트.
 *             PalletWorkstation 이 초기 1회 조회 (이후 STOMP 브로드캐스트로 갱신).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/wcs/alarms", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "WCS 알람 조회 API")
public class WcsAlarmController {

    private final ReinboundAlarmService reinboundAlarmService;

    /**
     * 재입고 대기 파렛트 알람 — follow_up_since 가 set 된 파렛트 전체 + 설정 알람 간격.
     * due(경과 ≥ intervalMin) 판정·소리 반복·해제는 프론트가 처리.
     */
    @GetMapping("/follow-up")
    @ResponseStatus(HttpStatus.OK)
    public FollowUpAlarmResponse followUpAlarms() {
        return reinboundAlarmService.buildResponse();
    }
}
