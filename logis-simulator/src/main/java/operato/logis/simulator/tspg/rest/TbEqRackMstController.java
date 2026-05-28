package operato.logis.simulator.tspg.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.simulator.tspg.entity.TbEqRackMst;
import operato.logis.simulator.tspg.service.TbEqRackMstService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbEqRackMst")
public class TbEqRackMstController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbEqRackMst.class;
    }

    private final TbEqRackMstService tbEqRackMstService;

    @RequestMapping(value="/getRackMstList", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="전체 Rack 정보 조회 API")
    public List<TbEqRackMst> getRackMstList() {
        return tbEqRackMstService.getRackMstList();
    }

    @RequestMapping(value="/getRackMstByEqId/{eqId}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="특정 Rack 정보 조회 API")
    public TbEqRackMst getRackMstByRackEqId(@PathVariable("eqId") String eqId) {
        return tbEqRackMstService.getRackMstByEqId(eqId);
    }
}