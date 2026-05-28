package operato.logis.samsung.dto.report.response;

import lombok.Data;

@Data
public class DailyRawRowDto {
    private String date;
    private String blNo;
    private String cntrNo;
    private String boxId;
    private String parcelId;
    private String boxWidth;
    private String boxLength;
    private String boxHeight;
    private String plcSeqNo;
    private String itemCode;
    private String processType;
    private String trackingStatus;
    private String trackingDesc;
    private String eventTime;
    private String lineId;
    private String equipId;
    private String finalStatus;
    private String rejectType;
}