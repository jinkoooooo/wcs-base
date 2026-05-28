package operato.logis.simulator.asrs.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.simulator.asrs.entity.TbSimulatorAsrsEquip;
import operato.logis.simulator.asrs.service.TbSimulatorAsrsEquipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbSimulatorAsrsEquip")
public class TbSimulatorAsrsEquipController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbSimulatorAsrsEquip.class;
    }

    private final TbSimulatorAsrsEquipService tbSimulatorAsrsEquipService;

    @RequestMapping(value="/getEquipList", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="전체 설비 정보 조회 API")
    public List<TbSimulatorAsrsEquip> getEquipList() {
        return tbSimulatorAsrsEquipService.getEquipList();
    }

    @RequestMapping(value="/updateEquipStatus", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="설비 상태 업데이트 API")
    public CommonApiResponse updateEquipStatus(@RequestBody TbSimulatorAsrsEquip equip) {
        tbSimulatorAsrsEquipService.updateEquipStatus(equip);
        return CommonApiResponse.success();
    }
}