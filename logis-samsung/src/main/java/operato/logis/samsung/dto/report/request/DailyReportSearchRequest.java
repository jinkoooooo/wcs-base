package operato.logis.samsung.dto.report.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class DailyReportSearchRequest {

    @JsonAlias({"todayDate", "today_date", "targetDate", "target_date"})
    private String todayDate;

    @JsonAlias({"blNo", "bl_no"})
    private String blNo;

    @JsonAlias({"cntrNo", "cntr_no"})
    private String cntrNo;

    @JsonAlias({"excludeManualYn", "exclude_manual_yn"})
    private String excludeManualYn = "N";
}