package operato.logis.samsung.dto.report.response;

import lombok.Data;

@Data
public class DailyNgDetailRowDto {

    private String boxSerialCode;
    private String containerNo;
    private String firstReceivedAt;
    private String manualProcessedAt;
    private String finalCompletedAt;
    private String rejectType;
    private String finalRemark;
    private String resultType;
}