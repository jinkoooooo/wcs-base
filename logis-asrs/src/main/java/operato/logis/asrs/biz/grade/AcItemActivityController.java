package operato.logis.asrs.biz.grade;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.grade.ItemActivityAggregationCore;
import operato.logis.asrs.dto.response.ItemActivityAggregationResult;
import operato.logis.asrs.query.grade.ItemActivityQueryService;
import operato.logis.asrs.query.grade.model.ItemActivityDailyView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 일별 활동 집계 API.
 *
 * <p>
 * 이번 단계는 집계 실행과 집계 결과 조회를 담당한다.
 * </p>
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/grade/activity")
@ServiceDesc(description = "AisleCore Item Activity API")
public class AcItemActivityController {

    private final ItemActivityAggregationCore itemActivityAggregationCore;
    private final ItemActivityQueryService itemActivityQueryService;

    /**
     * 영역 기준 일 집계 실행.
     *
     * @param areaCode 영역 코드
     * @param activityDate 집계 일자 (yyyy-MM-dd)
     * @return 집계 결과
     */
    @RequestMapping(
            value = "/aggregate",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Aggregate item activity by area and date")
    public ItemActivityAggregationResult aggregateArea(@RequestParam("areaCode") String areaCode,
                                                       @RequestParam("activityDate") String activityDate) {
        return itemActivityAggregationCore.aggregateArea(areaCode, LocalDate.parse(activityDate));
    }

    /**
     * 품목 기준 일 집계 실행.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @param activityDate 집계 일자 (yyyy-MM-dd)
     * @return 집계 결과
     */
    @RequestMapping(
            value = "/aggregate-item",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Aggregate item activity by item and date")
    public ItemActivityAggregationResult aggregateItem(@RequestParam("areaCode") String areaCode,
                                                       @RequestParam("itemCode") String itemCode,
                                                       @RequestParam("activityDate") String activityDate) {
        return itemActivityAggregationCore.aggregateItem(areaCode, itemCode, LocalDate.parse(activityDate));
    }

    /**
     * 영역 기준 일 집계 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param activityDate 집계 일자 (yyyy-MM-dd)
     * @return 집계 목록
     */
    @RequestMapping(
            value = "",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Find item daily activities by area and date")
    public List<ItemActivityDailyView> findDailyActivities(@RequestParam("areaCode") String areaCode,
                                                           @RequestParam("activityDate") String activityDate) {
        return itemActivityQueryService.findDailyActivities(areaCode, LocalDate.parse(activityDate));
    }

    /**
     * 품목 기준 일 집계 단건 조회.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @param activityDate 집계 일자 (yyyy-MM-dd)
     * @return 집계 단건
     */
    @RequestMapping(
            value = "/item",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Find item daily activity by area, item and date")
    public ItemActivityDailyView findDailyActivity(@RequestParam("areaCode") String areaCode,
                                                   @RequestParam("itemCode") String itemCode,
                                                   @RequestParam("activityDate") String activityDate) {
        return itemActivityQueryService.findDailyActivity(areaCode, itemCode, LocalDate.parse(activityDate));
    }
}