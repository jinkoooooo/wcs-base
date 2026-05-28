package operato.logis.wcs.rest.entity;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.RmkDictionary;
import operato.logis.wcs.service.impl.query.common.RmkDictionaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;

/**
 * 비고(Rmk) 공통코드 Dictionary 조회 REST.
 * 화면 콤보/표시용 코드-라벨 사전을 내려준다.
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/rmkdictionary")
@ServiceDesc(description = "RmkDictionary Service API")
public class RmkDictionaryController {

    private final RmkDictionaryService rmkDictionaryService;

    @RequestMapping(value = "/getRmkDictionary", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "공통코드 Dictionary 조회")
    public List<RmkDictionary> getRmkDictionary() {
        return rmkDictionaryService.getRmkDictionary();
    }
}