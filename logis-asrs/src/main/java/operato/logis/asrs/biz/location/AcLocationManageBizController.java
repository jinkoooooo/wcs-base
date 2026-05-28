package operato.logis.asrs.biz.location;

import java.util.List;

import operato.logis.asrs.dto.request.LocationBulkDeleteRequest;
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
import operato.logis.asrs.core.location.LocationCommandCore;
import operato.logis.asrs.dto.request.LocationUpsertRequest;
import operato.logis.asrs.dto.response.LocationSaveResult;
import operato.logis.asrs.query.location.LocationManageQueryService;
import operato.logis.asrs.query.location.model.AccessPointOptionView;
import operato.logis.asrs.query.location.model.ItemCategoryOptionView;
import operato.logis.asrs.query.location.model.LocationListView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 로케이션 관리 업무용 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/aislecore/locations/biz")
@ServiceDesc(description = "AisleCore Location Manage Business API")
public class AcLocationManageBizController {

    private final LocationManageQueryService locationManageQueryService;
    private final LocationCommandCore locationCommandCore;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Search locations")
    public List<LocationListView> search(
            @RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "locationCode", required = false) String locationCode,
            @RequestParam(value = "locationType", required = false) String locationType,
            @RequestParam(value = "activeYn", required = false) String activeYn
    ) {
        return locationManageQueryService.search(areaCode, locationCode, locationType, activeYn);
    }

    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get location detail by areaCode and locationCode")
    public LocationListView detail(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("locationCode") String locationCode
    ) {
        return locationManageQueryService.getDetailByAreaAndLocationCode(areaCode, locationCode);
    }

    @GetMapping(value = "/options/item-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get item category options")
    public List<ItemCategoryOptionView> itemCategoryOptions() {
        return locationManageQueryService.getItemCategoryOptions();
    }

    @GetMapping(value = "/options/access-points", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Get access point options")
    public List<AccessPointOptionView> accessPointOptions(
            @RequestParam(value = "areaCode", required = false) String areaCode
    ) {
        return locationManageQueryService.getAccessPointOptions(areaCode);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Create location")
    public LocationSaveResult create(@RequestBody LocationUpsertRequest request) {
        return locationCommandCore.create(request);
    }

    @PutMapping(value = "/{areaCode}/{locationCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Update location")
    public LocationSaveResult update(
            @PathVariable("areaCode") String areaCode,
            @PathVariable("locationCode") String locationCode,
            @RequestBody LocationUpsertRequest request
    ) {
        return locationCommandCore.update(areaCode, locationCode, request);
    }

    @DeleteMapping(value = "/{areaCode}/{locationCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Delete location")
    public LocationSaveResult delete(
            @PathVariable("areaCode") String areaCode,
            @PathVariable("locationCode") String locationCode
    ) {
        return locationCommandCore.delete(areaCode, locationCode);
    }

    @PostMapping(value = "/bulk-delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiDesc(description = "Bulk delete locations by current filter")
    public LocationSaveResult bulkDelete(@RequestBody LocationBulkDeleteRequest request) {
        return locationCommandCore.bulkDelete(request);
    }
}