package operato.logis.samsung.dto.report.response;

import lombok.Data;

@Data
public class DailyTimelineRowDto {
    private String rowType;      // CONTAINER / PROCESS / BOX
    private String rowGroup;     // cntr_no or process
    private String processType;  // BCR / SORTER / PALLETIZED
    private String blNo;
    private String cntrNo;
    private String boxId;
    private String plcSeqNo;
    private String itemCode;
    private String startAt;
    private String endAt;
    private Long durationSec;
    private String tooltipTitle;
    private String tooltipSub1;
    private String tooltipSub2;
}