package operato.logis.samsung.rest.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.samsung.entity.xyz.TbMwIfXyzCycle;
import operato.logis.samsung.service.xyz.XyzOrderReceiveService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/xyz/receive")
@ServiceDesc(description="XYZ Order 수신 API")
public class XyzOrderReceiveController {

    private final XyzOrderReceiveService xyzOrderReceiveService;

    @RequestMapping(value="/cycle", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="XYZ Cycle 완료 수신")
    public CommonApiResponse cycle(@RequestBody TbMwIfXyzCycle cycle) {
        return xyzOrderReceiveService.receiveCycleResult(cycle);
    }
}