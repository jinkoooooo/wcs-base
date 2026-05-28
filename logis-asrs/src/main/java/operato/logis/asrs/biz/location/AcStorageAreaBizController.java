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
import operato.logis.asrs.core.location.StorageAreaCommandCore;
import operato.logis.asrs.dto.request.StorageAreaUpsertRequest;
import operato.logis.asrs.dto.response.StorageAreaSaveResult;
import operato.logis.asrs.query.location.StorageAreaQueryService;
import operato.logis.asrs.query.location.model.CenterOptionView;
import operato.logis.asrs.query.location.model.OperationProfileOptionView;
import operato.logis.asrs.query.location.model.StorageAreaListView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 아레아 업무용 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/aislecore/storage-areas/biz")
@ServiceDesc(description = "AisleCore Storage Area Business API")
public class AcStorageAreaBizController {

    private final StorageAreaQueryService storageAreaQueryService;
    private final StorageAreaCommandCore storageAreaCommandCore;

    /**
     * 목록 조회.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Search storage areas")
    public List<StorageAreaListView> search(
            @RequestParam(value = "centerCode", required = false) String centerCode,
            @RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "areaName", required = false) String areaName,
            @RequestParam(value = "activeYn", required = false) String activeYn
    ) {
        return storageAreaQueryService.search(centerCode, areaCode, areaName, activeYn);
    }

    /**
     * 상세 조회.
     */
    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get storage area detail by centerCode and areaCode")
    public StorageAreaListView detail(
            @RequestParam("centerCode") String centerCode,
            @RequestParam("areaCode") String areaCode
    ) {
        return storageAreaQueryService.getDetailByCenterAndAreaCode(centerCode, areaCode);
    }

    /**
     * 센터 옵션 조회.
     */
    @GetMapping(value = "/options/centers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get active center options")
    public List<CenterOptionView> centerOptions() {
        return storageAreaQueryService.getCenterOptions();
    }

    /**
     * 오퍼레이션 프로필 옵션 조회.
     */
    @GetMapping(value = "/options/operation-profiles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get active operation profile options")
    public List<OperationProfileOptionView> operationProfileOptions() {
        return storageAreaQueryService.getOperationProfileOptions();
    }

    /**
     * 신규 생성.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Create storage area")
    public StorageAreaSaveResult create(@RequestBody StorageAreaUpsertRequest request) {
        return storageAreaCommandCore.create(request);
    }

    /**
     * 수정.
     *
     * path 는 기존 business key 기준.
     */
    @PutMapping(value = "/{centerCode}/{areaCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Update storage area")
    public StorageAreaSaveResult update(
            @PathVariable("centerCode") String centerCode,
            @PathVariable("areaCode") String areaCode,
            @RequestBody StorageAreaUpsertRequest request
    ) {
        return storageAreaCommandCore.update(centerCode, areaCode, request);
    }

    /**
     * 삭제.
     */
    @DeleteMapping(value = "/{centerCode}/{areaCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Delete storage area")
    public StorageAreaSaveResult delete(
            @PathVariable("centerCode") String centerCode,
            @PathVariable("areaCode") String areaCode
    ) {
        return storageAreaCommandCore.delete(centerCode, areaCode);
    }
}