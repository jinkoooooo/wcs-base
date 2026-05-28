package operato.logis.asrs.biz.location;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.location.AccessPointCommandCore;
import operato.logis.asrs.core.location.LocationAccessRecalculateCore;
import operato.logis.asrs.dto.request.AccessPointSaveRequest;
import operato.logis.asrs.dto.request.AccessPointSearchRequest;
import operato.logis.asrs.dto.request.LocationAccessPreviewRequest;
import operato.logis.asrs.dto.request.LocationAccessRecalculateRequest;
import operato.logis.asrs.dto.response.AccessPointResponse;
import operato.logis.asrs.dto.response.LocationAccessPreviewResult;
import operato.logis.asrs.dto.response.LocationAccessRecalculateResult;
import operato.logis.asrs.query.location.AccessPointManageQueryService;
import operato.logis.asrs.query.location.model.AccessPointManageRow;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 로케이션 접근성 / Access Point 관리 업무 API.
 *
 * <p>
 * 주요 기능:
 * - 로케이션 접근성 미리보기
 * - 로케이션 접근성 실제 반영
 * - Access Point 목록 / 상세 / 생성 / 수정 / 삭제
 * </p>
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/location-access")
@ServiceDesc(description = "AisleCore Location Access API")
public class AcLocationAccessController {

    private final LocationAccessRecalculateCore locationAccessRecalculateCore;

    private final AccessPointManageQueryService accessPointManageQueryService;

    private final AccessPointCommandCore accessPointCommandCore;

    /* ---------------------------------------------------------------------- */
    /* Location Access Recalculate                                             */
    /* ---------------------------------------------------------------------- */

    /**
     * 로케이션 접근성 재산출 미리보기 API.
     *
     * @param request 미리보기 요청
     * @return 미리보기 결과
     */
    @RequestMapping(
            value = "/preview",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Preview location access recalculation")
    public LocationAccessPreviewResult preview(@RequestBody LocationAccessPreviewRequest request) {
        return locationAccessRecalculateCore.preview(request);
    }

    /**
     * 로케이션 접근성 재산출 실제 반영 API.
     *
     * @param request 실행 요청
     * @return 실행 결과
     */
    @RequestMapping(
            value = "/execute",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Execute location access recalculation")
    public LocationAccessRecalculateResult execute(@RequestBody LocationAccessRecalculateRequest request) {
        return locationAccessRecalculateCore.execute(request);
    }

    /* ---------------------------------------------------------------------- */
    /* Access Point Management                                                 */
    /* ---------------------------------------------------------------------- */

    /**
     * Access Point 목록 조회.
     *
     * <p>
     * URL:
     * GET /rest/aislecore/location-access/access-points
     * </p>
     *
     * @param areaCode 영역 코드
     * @param pointCode 포인트 코드
     * @param pointName 포인트 명
     * @param pointType 포인트 타입
     * @param purposeCode 목적 코드
     * @param useForSortYn 접근성 산정 사용 여부
     * @param activeYn 활성 여부
     * @return Access Point 목록
     */
    @Transactional(readOnly = true)
    @ApiDesc(description = "Search access points")
    @RequestMapping(
            value = "/access-points",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<AccessPointManageRow> searchAccessPoints(
            @RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "pointCode", required = false) String pointCode,
            @RequestParam(value = "pointName", required = false) String pointName,
            @RequestParam(value = "pointType", required = false) String pointType,
            @RequestParam(value = "purposeCode", required = false) String purposeCode,
            @RequestParam(value = "useForSortYn", required = false) String useForSortYn,
            @RequestParam(value = "activeYn", required = false) String activeYn) {

        AccessPointSearchRequest request = new AccessPointSearchRequest();
        request.setAreaCode(areaCode);
        request.setPointCode(pointCode);
        request.setPointName(pointName);
        request.setPointType(pointType);
        request.setPurposeCode(purposeCode);
        request.setUseForSortYn(useForSortYn);
        request.setActiveYn(activeYn);

        return accessPointManageQueryService.search(request);
    }

    /**
     * Access Point 상세 조회.
     *
     * <p>
     * URL:
     * GET /rest/aislecore/location-access/access-points/detail
     * </p>
     *
     * @param areaCode 영역 코드
     * @param pointCode 포인트 코드
     * @return Access Point 상세
     */
    @Transactional(readOnly = true)
    @ApiDesc(description = "Get access point detail")
    @RequestMapping(
            value = "/access-points/detail",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AccessPointResponse getAccessPointDetail(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("pointCode") String pointCode) {

        return accessPointCommandCore.detail(areaCode, pointCode);
    }

    /**
     * Access Point 신규 생성.
     *
     * <p>
     * URL:
     * POST /rest/aislecore/location-access/access-points
     * </p>
     *
     * @param request 생성 요청
     * @return 생성 결과
     */
    @ApiDesc(description = "Create access point")
    @RequestMapping(
            value = "/access-points",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AccessPointResponse createAccessPoint(@RequestBody AccessPointSaveRequest request) {
        return accessPointCommandCore.create(request);
    }

    /**
     * Access Point 수정.
     *
     * <p>
     * URL:
     * PUT /rest/aislecore/location-access/access-points/{areaCode}/{pointCode}
     * </p>
     *
     * @param areaCode 기존 영역 코드
     * @param pointCode 기존 포인트 코드
     * @param request 수정 요청
     * @return 수정 결과
     */
    @ApiDesc(description = "Update access point")
    @RequestMapping(
            value = "/access-points/{areaCode}/{pointCode}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AccessPointResponse updateAccessPoint(
            @PathVariable("areaCode") String areaCode,
            @PathVariable("pointCode") String pointCode,
            @RequestBody AccessPointSaveRequest request) {

        return accessPointCommandCore.update(areaCode, pointCode, request);
    }

    /**
     * Access Point 삭제.
     *
     * <p>
     * 물리 삭제가 아니라 active_yn = 'N' 처리한다.
     * URL:
     * DELETE /rest/aislecore/location-access/access-points/{areaCode}/{pointCode}
     * </p>
     *
     * @param areaCode 영역 코드
     * @param pointCode 포인트 코드
     * @return 삭제 결과
     */
    @ApiDesc(description = "Delete access point")
    @RequestMapping(
            value = "/access-points/{areaCode}/{pointCode}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AccessPointResponse deleteAccessPoint(
            @PathVariable("areaCode") String areaCode,
            @PathVariable("pointCode") String pointCode) {

        return accessPointCommandCore.delete(areaCode, pointCode);
    }
}