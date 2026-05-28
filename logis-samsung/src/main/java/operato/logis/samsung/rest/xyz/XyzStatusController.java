package operato.logis.samsung.rest.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.samsung.service.xyz.XyzStatusService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/xyz/status")
@ServiceDesc(description="XYZ 상태 API 수신")
public class XyzStatusController {

    private final XyzStatusService xyzStatusService;

    @RequestMapping(value="/exception", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="XYZ 시스템 에러 수신")
    public CommonApiResponse exception(@RequestBody Map<String, Object> errorInfo) {
        xyzStatusService.createXyzErrorLog(errorInfo);
        return CommonApiResponse.success();
    }
}