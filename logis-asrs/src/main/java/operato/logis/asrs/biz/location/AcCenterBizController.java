package operato.logis.asrs.biz.location;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.location.CenterCommandCore;
import operato.logis.asrs.dto.request.CenterUpsertRequest;
import operato.logis.asrs.dto.response.CenterSaveResult;
import operato.logis.asrs.query.location.CenterQueryService;
import operato.logis.asrs.query.location.model.CenterListView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 센터 업무용 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/aislecore/centers")
@ServiceDesc(description = "AisleCore Center Business API")
public class AcCenterBizController {

    private final CenterQueryService centerQueryService;
    private final CenterCommandCore centerCommandCore;

    /**
     * 목록 조회.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Search centers")
    public List<CenterListView> search(
            @RequestParam(value = "centerCode", required = false) String centerCode,
            @RequestParam(value = "centerName", required = false) String centerName,
            @RequestParam(value = "activeYn", required = false) String activeYn
    ) {
        return centerQueryService.search(centerCode, centerName, activeYn);
    }

    /**
     * 상세 조회.
     */
    @GetMapping(value = "/{centerCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get center detail by centerCode")
    public CenterListView detail(@PathVariable("centerCode") String centerCode) {
        return centerQueryService.getDetailByCode(centerCode);
    }

    /**
     * 신규 생성.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Create center")
    public CenterSaveResult create(@RequestBody CenterUpsertRequest request) {
        return centerCommandCore.create(request);
    }

    /**
     * 수정.
     */
    @PutMapping(value = "/{centerCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Update center")
    public CenterSaveResult update(
            @PathVariable("centerCode") String centerCode,
            @RequestBody CenterUpsertRequest request
    ) {
        return centerCommandCore.update(centerCode, request);
    }

    /**
     * 삭제.
     */
    @DeleteMapping(value = "/{centerCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Delete center")
    public CenterSaveResult delete(@PathVariable("centerCode") String centerCode) {
        return centerCommandCore.delete(centerCode);
    }
}