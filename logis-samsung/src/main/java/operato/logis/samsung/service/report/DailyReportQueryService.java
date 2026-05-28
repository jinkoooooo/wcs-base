package operato.logis.samsung.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.samsung.dto.report.request.DailyReportRawRequest;
import operato.logis.samsung.dto.report.request.DailyReportSearchRequest;
import operato.logis.samsung.dto.report.request.MonthlyReportSearchRequest;
import operato.logis.samsung.dto.report.response.*;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 일별 리포트 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReportQueryService extends AbstractQueryService {

    /** TrackingStatus enum 반영 */
    private static final int STATUS_BCR_MEASURED = 110;
    private static final int STATUS_VISION_MEASURED = 120;
    private static final int STATUS_REPORT_DVRT = 501;
    private static final int STATUS_REPORT_PLTZ = 531;
    private static final int STATUS_STORED = 700;
    private static final int STATUS_REJECTED = 701;

    /** 입고 gap threshold */
    private static final int GAP_THRESHOLD_SEC = 15;

    /** 1 SKU 동시교체 그룹핑 기준(초) */
    private static final int SINGLE_SKU_EXCHANGE_GROUP_SEC = 30;

    /**
     * 상단 KPI 요약
     */
    public DailyReportSummaryDto getDailySummary(DailyReportSearchRequest request) {
        return this.queryManager.selectBySql(
                DailyReportSqls.summarySql(),
                buildParam(request),
                DailyReportSummaryDto.class
        );
    }

    /**
     * 타임라인 조회
     */
    public List<DailyTimelineRowDto> getDailyTimeline(DailyReportSearchRequest request) {
        return this.queryManager.selectListBySql(
                DailyReportSqls.timelineSql(),
                buildParam(request),
                DailyTimelineRowDto.class,
                0,
                0
        );
    }

    /**
     * 하단 원본 데이터 조회
     */
    public List<DailyRawRowDto> getDailyRawRows(DailyReportRawRequest request) {
        String sql;

        if ("BCR".equalsIgnoreCase(request.getProcessType())) {
            sql = DailyReportSqls.bcrRawSql();
        } else if ("SORTER".equalsIgnoreCase(request.getProcessType())) {
            sql = DailyReportSqls.sorterRawSql();
        } else {
            sql = DailyReportSqls.palletizedRawSql();
        }

        return this.queryManager.selectListBySql(sql, buildParam(request), DailyRawRowDto.class, 0, 0);
    }

    /**
     * 공통 파라미터 맵
     */
    private Map<String, Object> buildParam(DailyReportSearchRequest request) {
        return ValueUtil.newMap(
                "targetDate,blNo,cntrNo,excludeManualYn,bcrMeasuredStatus,visionMeasuredStatus,reportDvrtStatus,reportPltzStatus,storedStatus,rejectedStatus,gapThresholdSec,singleSkuExchangeGroupSec",
                Date.valueOf(request.getTodayDate()),
                request.getBlNo(),
                request.getCntrNo(),
                request.getExcludeManualYn(),
                STATUS_BCR_MEASURED,
                STATUS_VISION_MEASURED,
                STATUS_REPORT_DVRT,
                STATUS_REPORT_PLTZ,
                STATUS_STORED,
                STATUS_REJECTED,
                GAP_THRESHOLD_SEC,
                SINGLE_SKU_EXCHANGE_GROUP_SEC
        );
    }

    public List<DailyHourlyUphRowDto> getDailyHourlyUphRows(DailyReportSearchRequest request) {
        return this.queryManager.selectListBySql(
                DailyReportSqls.hourlyUphSql(),
                buildParam(request),
                DailyHourlyUphRowDto.class,
                0,
                0
        );
    }

    public List<DailyPalletExchangeRowDto> getDailyPalletExchangeRows(DailyReportSearchRequest request) {
        return this.queryManager.selectListBySql(
                DailyReportSqls.palletExchangeSheetSql(),
                buildParam(request),
                DailyPalletExchangeRowDto.class,
                0,
                0
        );
    }

    public List<DailyNgDetailRowDto> getDailyNgDetailRows(DailyReportSearchRequest request) {
        return this.queryManager.selectListBySql(
                DailyReportSqls.ngDetailSql(),
                buildParam(request),
                DailyNgDetailRowDto.class,
                0,
                0
        );
    }

    public List<MonthlyReportSummaryRowDto> getMonthlySummaryRows(MonthlyReportSearchRequest request) {
        Map<String, Object> params = new HashMap<>();

        params.put("month", request.getMonth());
        params.put("storedStatus", 700);

        return this.queryManager.selectListBySql(
                DailyReportSqls.monthlySummarySql(),
                params,
                MonthlyReportSummaryRowDto.class,
                0,
                0
        );
    }
}