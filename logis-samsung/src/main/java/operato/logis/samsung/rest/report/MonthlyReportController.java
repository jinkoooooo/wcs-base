package operato.logis.samsung.rest.report;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.report.request.MonthlyReportSearchRequest;
import operato.logis.samsung.dto.report.response.MonthlyReportSummaryRowDto;
import operato.logis.samsung.service.report.DailyReportQueryService;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/report/monthly")
@ServiceDesc(description = "Samsung Monthly Report API")
public class MonthlyReportController {

    private final DailyReportQueryService dailyReportQueryService;

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "월별 리포트 일자 요약")
    public List<MonthlyReportSummaryRowDto> getMonthlySummary(
            @ModelAttribute MonthlyReportSearchRequest request
    ) {
        return dailyReportQueryService.getMonthlySummaryRows(request);
    }
}