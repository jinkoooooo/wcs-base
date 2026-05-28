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
import operato.logis.asrs.core.location.OperationProfileCommandCore;
import operato.logis.asrs.dto.request.OperationProfileUpsertRequest;
import operato.logis.asrs.dto.response.OperationProfileSaveResult;
import operato.logis.asrs.query.location.OperationProfileQueryService;
import operato.logis.asrs.query.location.model.OperationProfileListView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 오퍼레이션 프로필 업무용 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/aislecore/operation-profiles")
@ServiceDesc(description = "AisleCore Operation Profile Business API")
public class AcOperationProfileBizController {

    private final OperationProfileQueryService operationProfileQueryService;
    private final OperationProfileCommandCore operationProfileCommandCore;

    /**
     * 목록 조회.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Search operation profiles")
    public List<OperationProfileListView> search(
            @RequestParam(value = "profileCode", required = false) String profileCode,
            @RequestParam(value = "profileName", required = false) String profileName,
            @RequestParam(value = "activeYn", required = false) String activeYn
    ) {
        return operationProfileQueryService.search(profileCode, profileName, activeYn);
    }

    /**
     * 상세 조회.
     */
    @GetMapping(value = "/{profileCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get operation profile detail by profileCode")
    public OperationProfileListView detail(@PathVariable("profileCode") String profileCode) {
        return operationProfileQueryService.getDetailByCode(profileCode);
    }

    /**
     * 신규 생성.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Create operation profile")
    public OperationProfileSaveResult create(@RequestBody OperationProfileUpsertRequest request) {
        return operationProfileCommandCore.create(request);
    }

    /**
     * 수정.
     */
    @PutMapping(value = "/{profileCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Update operation profile")
    public OperationProfileSaveResult update(
            @PathVariable("profileCode") String profileCode,
            @RequestBody OperationProfileUpsertRequest request
    ) {
        return operationProfileCommandCore.update(profileCode, request);
    }

    /**
     * 삭제.
     */
    @DeleteMapping(value = "/{profileCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Delete operation profile")
    public OperationProfileSaveResult delete(@PathVariable("profileCode") String profileCode) {
        return operationProfileCommandCore.delete(profileCode);
    }
}