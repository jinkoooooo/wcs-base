package operato.logis.samsung.dto.report.response;

import lombok.Data;

@Data
public class DailyReportSummaryDto {
    private String todayDate;
    private String blNo;
    private String cntrNo;

    private Integer totalBoxQty;
    private Integer jobSkuQty;
    private Integer actualSkuQty;
    private Integer okBoxQty;
    private Integer ngBoxQty;
    private Integer pendingBoxQty;

    private String firstReceivedAt;
    private String lastPalletizedAt;
    private String totalOperatingTime;
    private String idleTime;
    private String palletOperatingTime;

    private Double totalTimeUph;
    private Double palletTimeUph;
    private Double ngRatePct;

    private String avgAllTime;
    private String medianTime;
    private String p95Time;
    private String avgExclP95Time;
    private String minTime;
    private String maxTime;

    private String summaryText;

    private Integer manualBoxQty;
    private String jobStartDt;
    private String jobEndDt;
}