package operato.logis.posco.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.posco.dto.McsMonitoringDetailDto;
import operato.logis.posco.dto.McsMonitoringRequestDto;
import operato.logis.posco.service.McsMonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/mcsMonitoring")
public class McsMonitoringController {

    private final McsMonitoringService mcsMonitoringService;

    @RequestMapping(value="/getEquipDetail", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="설비 상세 정보 조회 API")
    public McsMonitoringDetailDto getMonitoringDetail(@RequestBody McsMonitoringRequestDto request) {
        return mcsMonitoringService.getMonitoringDetail(request.getEquipType(), request.getEquipCode());
    }
}