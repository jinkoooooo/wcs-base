package operato.logis.simulator.tspg.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.simulator.tspg.entity.TbEqCarErrorLog;
import operato.logis.simulator.tspg.entity.TbEqCarMst;
import operato.logis.simulator.tspg.service.TbEqCarMstService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbEqCarMst")
public class TbEqCarMstController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbEqCarMst.class;
    }

    private final TbEqCarMstService tbEqCarMstService;

    @RequestMapping(value="/getCarMstList", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="전체 Shuttle 정보 조회 API")
    public List<TbEqCarMst> getCarMstList() {
        return tbEqCarMstService.getCarMstList();
    }

    @RequestMapping(value="/getCarMstByEqId/{eqId}", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="특정 Shuttle 정보 조회 API")
    public TbEqCarMst getCarMstByEqId(@PathVariable("eqId") String eqId) {
        return tbEqCarMstService.getCarMstByEqId(eqId);
    }

    @RequestMapping(value="/updateCarStatus", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Shuttle 상태 변경 API")
    public CommonApiResponse updateCarStatus(@RequestBody TbEqCarMst car) {
        tbEqCarMstService.updateCarStatus(car);
        return CommonApiResponse.success();
    }

    @RequestMapping(value="/createCarErrorLog", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Shuttle 에러 이력 생성 API")
    public CommonApiResponse createCarErrorLog(@RequestBody TbEqCarErrorLog log) {
        tbEqCarMstService.createCarErrorLog(log);
        return CommonApiResponse.success();
    }
}