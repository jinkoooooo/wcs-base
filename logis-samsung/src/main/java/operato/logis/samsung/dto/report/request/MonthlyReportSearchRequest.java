package operato.logis.samsung.dto.report.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class MonthlyReportSearchRequest {

    /**
     * 조회월
     * 형식: yyyy-MM
     * 예: 2026-05
     */
    @JsonAlias({"month", "month"})
    private String month;
}