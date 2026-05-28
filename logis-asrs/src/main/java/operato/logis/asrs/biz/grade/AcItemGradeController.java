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
import operato.logis.asrs.core.grade.ItemGradeCalculationCore;
import operato.logis.asrs.dto.response.ItemGradeCalculationResult;
import operato.logis.asrs.query.grade.ItemGradeQueryService;
import operato.logis.asrs.query.grade.model.ItemGradeHistoryView;
import operato.logis.asrs.query.grade.model.ItemGradeView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * AisleCore 업무용 상품 등급 API.
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/grade")
@ServiceDesc(description = "AisleCore Item Grade API")
public class AcItemGradeController {

    private final ItemGradeCalculationCore itemGradeCalculationCore;
    private final ItemGradeQueryService itemGradeQueryService;

    /**
     * 영역 기준 등급 계산 실행.
     */
    @RequestMapping(
            value = "/calculate",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Calculate item grades by area and activity date")
    public ItemGradeCalculationResult calculateArea(@RequestParam("areaCode") String areaCode,
                                                    @RequestParam("activityDate") String activityDate,
                                                    @RequestParam(value = "policyCode", required = false) String policyCode) {
        return itemGradeCalculationCore.calculateArea(areaCode, LocalDate.parse(activityDate), policyCode);
    }

    /**
     * 영역 + 품목 기준 등급 계산 실행.
     */
    @RequestMapping(
            value = "/calculate-item",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Calculate item grade by area, item and activity date")
    public ItemGradeCalculationResult calculateItem(@RequestParam("areaCode") String areaCode,
                                                    @RequestParam("itemCode") String itemCode,
                                                    @RequestParam("activityDate") String activityDate,
                                                    @RequestParam(value = "policyCode", required = false) String policyCode) {
        return itemGradeCalculationCore.calculateItem(areaCode, itemCode, LocalDate.parse(activityDate), policyCode);
    }

    /**
     * 영역 기준 현재 등급 목록 조회.
     */
    @RequestMapping(
            value = "",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Find current item grades by area")
    public List<ItemGradeView> findCurrentGrades(@RequestParam("areaCode") String areaCode) {
        return itemGradeQueryService.findCurrentGrades(areaCode);
    }

    /**
     * 영역 + 품목 기준 현재 등급 단건 조회.
     */
    @RequestMapping(
            value = "/item",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Find current item grade by area and item")
    public ItemGradeView findCurrentGrade(@RequestParam("areaCode") String areaCode,
                                          @RequestParam("itemCode") String itemCode) {
        return itemGradeQueryService.findCurrentGrade(areaCode, itemCode);
    }

    /**
     * 영역 + 품목 기준 등급 변경 이력 조회.
     */
    @RequestMapping(
            value = "/history",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(readOnly = true)
    @ApiDesc(description = "Find item grade history by area and item")
    public List<ItemGradeHistoryView> findGradeHistories(@RequestParam("areaCode") String areaCode,
                                                         @RequestParam("itemCode") String itemCode) {
        return itemGradeQueryService.findGradeHistories(areaCode, itemCode);
    }
}