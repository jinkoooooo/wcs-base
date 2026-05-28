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
import operato.logis.asrs.core.location.LocationProfileCommandCore;
import operato.logis.asrs.dto.request.LocationProfileUpsertRequest;
import operato.logis.asrs.dto.response.LocationProfileSaveResult;
import operato.logis.asrs.dto.response.LocationGeneratePreviewResult;
import operato.logis.asrs.dto.response.LocationGenerateResult;
import operato.logis.asrs.query.location.LocationProfileManageQueryService;
import operato.logis.asrs.query.location.model.LocationProfileListView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 로케이션 프로필 업무용 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/aislecore/location-profiles")
@ServiceDesc(description = "AisleCore Location Profile Business API")
public class AcLocationProfileBizController {

    private final LocationProfileManageQueryService locationProfileManageQueryService;
    private final LocationProfileCommandCore locationProfileCommandCore;
    private final AcLocationBizController acLocationBizController;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Search location profiles")
    public List<LocationProfileListView> search(
            @RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "profileCode", required = false) String profileCode,
            @RequestParam(value = "profileName", required = false) String profileName,
            @RequestParam(value = "activeYn", required = false) String activeYn
    ) {
        return locationProfileManageQueryService.search(areaCode, profileCode, profileName, activeYn);
    }

    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get location profile detail by areaCode and profileCode")
    public LocationProfileListView detail(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("profileCode") String profileCode
    ) {
        return locationProfileManageQueryService.getDetailByAreaAndProfileCode(areaCode, profileCode);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Create location profile")
    public LocationProfileSaveResult create(@RequestBody LocationProfileUpsertRequest request) {
        return locationProfileCommandCore.create(request);
    }

    @PutMapping(value = "/{areaCode}/{profileCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Update location profile")
    public LocationProfileSaveResult update(
            @PathVariable("areaCode") String areaCode,
            @PathVariable("profileCode") String profileCode,
            @RequestBody LocationProfileUpsertRequest request
    ) {
        return locationProfileCommandCore.update(areaCode, profileCode, request);
    }

    @DeleteMapping(value = "/{areaCode}/{profileCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Delete location profile")
    public LocationProfileSaveResult delete(
            @PathVariable("areaCode") String areaCode,
            @PathVariable("profileCode") String profileCode
    ) {
        return locationProfileCommandCore.delete(areaCode, profileCode);
    }

    /**
     * 기존 preview API 재사용.
     */
    @GetMapping(value = "/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Preview location generation by areaCode and profileCode")
    public LocationGeneratePreviewResult preview(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("profileCode") String profileCode
    ) {
        return acLocationBizController.preview(areaCode, profileCode);
    }

    /**
     * 기존 generate API 재사용.
     */
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Generate locations by areaCode and profileCode")
    public LocationGenerateResult generate(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("profileCode") String profileCode
    ) {
        return acLocationBizController.generate(areaCode, profileCode);
    }
}