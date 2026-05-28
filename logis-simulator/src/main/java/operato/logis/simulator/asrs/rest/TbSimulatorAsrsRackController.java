package operato.logis.simulator.asrs.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.simulator.asrs.entity.TbSimulatorAsrsRack;
import operato.logis.simulator.asrs.service.TbSimulatorAsrsRackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbSimulatorAsrsRack")
public class TbSimulatorAsrsRackController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbSimulatorAsrsRack.class;
    }

    private final TbSimulatorAsrsRackService tbSimulatorAsrsRackService;

    @RequestMapping(value="/getRackList", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="전체 Rack 정보 조회 API")
    public List<TbSimulatorAsrsRack> getRackList() {
        return tbSimulatorAsrsRackService.getRackList();
    }
}