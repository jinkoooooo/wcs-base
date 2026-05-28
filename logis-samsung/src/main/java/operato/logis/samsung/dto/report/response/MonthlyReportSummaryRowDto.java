package operato.logis.samsung.dto.report.response;

import lombok.Data;

@Data
public class MonthlyReportSummaryRowDto {

    /**
     * 일자
     * 예: 2026-05-14
     */
    private String reportDate;

    /**
     * 총 박스수
     */
    private Integer totalBoxQty;

    /**
     * 정상완료 수
     */
    private Integer okBoxQty;

    /**
     * 최종 NG 수
     */
    private Integer ngBoxQty;

    /**
     * 미완료 수
     */
    private Integer pendingBoxQty;

    /**
     * 전체 운영 UPH
     */
    private Double totalTimeUph;

    /**
     * 파렛타이저 운영 UPH
     */
    private Double palletTimeUph;

    /**
     * 전체 운영시간
     */
    private String totalOperatingTime;

    /**
     * 파렛타이저 운영시간
     */
    private String palletOperatingTime;

    /**
     * 입고 첫 박스 시작시간
     */
    private String firstReceivedAt;

    /**
     * 마지막 박스 적재시간
     */
    private String lastPalletizedAt;
}