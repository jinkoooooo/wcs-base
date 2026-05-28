package operato.logis.samsung.rest.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.samsung.event.BoxArrivedOnConveyorEvent;
import operato.logis.samsung.event.BoxTrackingEvent;
import operato.logis.samsung.service.xyz.XyzStatusService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.event.EventPublisher;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/xyz/test")
@ServiceDesc(description="XYZ 테스트 API")
public class TestController {

    private final EventPublisher eventPublisher;
    private final XyzStatusService xyzStatusService;

    @RequestMapping(value="/bcr", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="BCR Scanning Test")
    public CommonApiResponse bcr(@RequestBody BoxTrackingEvent event) {
        eventPublisher.publishEvent(event);
        return CommonApiResponse.success();
    }

    @RequestMapping(value="/conveyor", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="BoxConveyor Trigger Event")
    public CommonApiResponse conveyor(@RequestBody BoxArrivedOnConveyorEvent event) {
        eventPublisher.publishEvent(event);
        return CommonApiResponse.success();
    }

    @RequestMapping(value="/status", method= RequestMethod.GET, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check XYZ Status")
    public CommonApiResponse status() {
        xyzStatusService.getXyzStatus();
        return CommonApiResponse.success();
    }
}
