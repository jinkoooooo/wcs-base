package operato.logis.samsung.rest.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.samsung.service.mw.GtrInspectionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/dashboard/integrated")
@ServiceDesc(description="VISION 및 GTR 통합 대시보드 API")
public class TbMwGtrController {

    private final GtrInspectionService gtrinspectionservice;

    @PostMapping(value = "/stats", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "기간 및 바코드 기반 GTR/VISION 통계 및 목록 조회")
    public CommonApiResponse getIntegratedStats(@RequestBody Map<String, Object> params) {

        Map<String, Object> resultData = gtrinspectionservice.getDashboardData(params);

        CommonApiResponse response = CommonApiResponse.success();

        response.setData(resultData);

        return response;
    }
}
