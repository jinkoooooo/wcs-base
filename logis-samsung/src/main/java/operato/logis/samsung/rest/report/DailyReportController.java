package operato.logis.samsung.rest.report;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.report.request.DailyReportRawRequest;
import operato.logis.samsung.dto.report.request.DailyReportSearchRequest;
import operato.logis.samsung.dto.report.response.DailyRawRowDto;
import operato.logis.samsung.dto.report.response.DailyReportSummaryDto;
import operato.logis.samsung.dto.report.response.DailyTimelineRowDto;
import operato.logis.samsung.service.report.DailyReportExcelService;
import operato.logis.samsung.service.report.DailyReportQueryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;

@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/rest/report/daily")
@ServiceDesc(description = "Samsung Day Report API")
public class DailyReportController {

    private final DailyReportQueryService dailyReportQueryService;
    private final DailyReportExcelService dailyReportExcelService;

    @PostMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "일별 리포트 요약")
    public DailyReportSummaryDto getSummary(@RequestBody DailyReportSearchRequest request) {
        return dailyReportQueryService.getDailySummary(request);
    }

    @PostMapping(value = "/timeline", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "일별 리포트 타임라인")
    public List<DailyTimelineRowDto> getTimeline(@RequestBody DailyReportSearchRequest request) {
        return dailyReportQueryService.getDailyTimeline(request);
    }

    @PostMapping(value = "/bcr", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "BCR 원본")
    public List<DailyRawRowDto> getBcrRows(@RequestBody DailyReportRawRequest request) {
        request.setProcessType("BCR");
        return dailyReportQueryService.getDailyRawRows(request);
    }

    @PostMapping(value = "/sorter", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Sorter 원본")
    public List<DailyRawRowDto> getSorterRows(@RequestBody DailyReportRawRequest request) {
        request.setProcessType("SORTER");
        return dailyReportQueryService.getDailyRawRows(request);
    }

    @PostMapping(value = "/palletized", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Palletized 원본")
    public List<DailyRawRowDto> getPalletizedRows(@RequestBody DailyReportRawRequest request) {
        request.setProcessType("PALLETIZED");
        return dailyReportQueryService.getDailyRawRows(request);
    }

    @PostMapping(value = "/export")
    @ApiDesc(description = "일별 리포트 엑셀 추출")
    public ResponseEntity<byte[]> export(@RequestBody DailyReportSearchRequest request) {
        byte[] bytes = dailyReportExcelService.exportDailyReport(request);
        String fileName = "samsung_day_report_" + request.getTodayDate() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}