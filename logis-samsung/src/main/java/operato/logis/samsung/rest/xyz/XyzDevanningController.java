package operato.logis.samsung.rest.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.samsung.dto.xyz.XyzDevanningResult;
import operato.logis.samsung.entity.xyz.TbMwIfXyzCycle;
import operato.logis.samsung.service.xyz.XyzDevanningService;
import operato.logis.samsung.service.xyz.XyzOrderReceiveService;
import operato.logis.samsung.service.xyz.XyzStatusService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/xyz/devanning")
@ServiceDesc(description="XYZ Order 수신 API")
public class XyzDevanningController {

    private final XyzDevanningService xyzDevanningService;
    private final XyzStatusService xyzStatusService;


    @RequestMapping(value="/cycle", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="XYZ Cycle 완료 수신")
    public CommonApiResponse cycle(@RequestBody TbMwIfXyzCycle cycle) {
        return xyzDevanningService.sendPalletExchange("");
    }

    @RequestMapping(value="/preset_order", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="XYZ Cycle 완료 수신")
    public CommonApiResponse preset_order(@RequestBody TbMwIfXyzCycle cycle) {
        return xyzDevanningService.sendPalletExchange("");
    }

    @RequestMapping(value="/devanning_cancel", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check XYZ Devanning cancel")
    public CommonApiResponse devanning_cancel() {
        xyzDevanningService.getXyzDevanningCancel();
        return CommonApiResponse.success();
    }

    @RequestMapping(value="/devanning_delete", method= RequestMethod.DELETE, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check XYZ Devanning delete")
    public CommonApiResponse devanning_delete() {
        xyzDevanningService.getXyzDevanningDelete();
        return CommonApiResponse.success();
    }

    @RequestMapping(value="/devanning_status", method= RequestMethod.GET, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check XYZ Devanning Status")
    public CommonApiResponse devanning_status() {
        xyzStatusService.getXyzDnvStatus();
        return CommonApiResponse.success();
    }

    @RequestMapping(value="/devanning_order", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Request XYZ Devanning")
    public CommonApiResponse devanning_order( @RequestBody Map<String, String> requestData) {
        String cntrNo = requestData.get("cntr_no");
        String blNo = requestData.get("bl_no");
        CommonApiResponse result = xyzDevanningService.sendDevanningRequest(cntrNo, blNo);
        return result;
    }

    @RequestMapping(value="/orders/result", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="XYZ Order 처리 결과 수신")
    public CommonApiResponse receiveOrderResult(@RequestBody XyzDevanningResult request) {

        System.out.println("[XYZ -> MW] Webhook Data Received: " + request.toString());

        return xyzDevanningService.XyzDevanningOrderResult(request);
    }
}