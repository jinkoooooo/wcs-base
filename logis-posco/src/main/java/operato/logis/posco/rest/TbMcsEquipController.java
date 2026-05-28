package operato.logis.posco.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.posco.entity.TbMcsEquip;
import operato.logis.posco.service.TbMcsEquipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbMcsEquip")
public class TbMcsEquipController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbMcsEquip.class;
    }

    private final TbMcsEquipService tbMcsEquipService;

    @RequestMapping(value="/getEquipList", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="전체 설비 정보 조회 API")
    public List<TbMcsEquip> getEquipList() {
        return tbMcsEquipService.getEquipList();
    }
}