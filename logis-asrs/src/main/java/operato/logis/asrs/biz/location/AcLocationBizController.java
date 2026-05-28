package operato.logis.asrs.biz.location;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.location.LocationGenerateCore;
import operato.logis.asrs.dto.response.LocationGeneratePreviewResult;
import operato.logis.asrs.dto.response.LocationGenerateResult;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 로케이션 API.
 *
 * <p>
 * 신규 표준은 areaCode + profileCode 기반이다.
 * </p>
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/location")
@ServiceDesc(description = "AisleCore Location Business API")
public class AcLocationBizController {

    private final LocationGenerateCore locationGenerateCore;

    /**
     * 로케이션 프로파일 기준 Preview 조회.
     *
     * @param areaCode 영역 코드
     * @param profileCode 로케이션 프로파일 코드
     * @return preview 결과
     */
    @RequestMapping(
            value = "/profiles/preview",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Preview location generation by areaCode and profileCode")
    public LocationGeneratePreviewResult preview(@RequestParam("areaCode") String areaCode,
                                                 @RequestParam("profileCode") String profileCode) {
        return locationGenerateCore.previewByProfileCode(areaCode, profileCode);
    }

    /**
     * 로케이션 프로파일 기준 실제 생성 실행.
     *
     * @param areaCode 영역 코드
     * @param profileCode 로케이션 프로파일 코드
     * @return 생성 결과
     */
    @RequestMapping(
            value = "/profiles/generate",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Generate locations by areaCode and profileCode")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationGenerateResult generate(@RequestParam("areaCode") String areaCode,
                                           @RequestParam("profileCode") String profileCode) {
        return locationGenerateCore.generateByProfileCode(areaCode, profileCode);
    }
}