package operato.logis.connector.hokusho.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HokushoPerformanceReportRequest {


    @JsonProperty("requestedAt")
    private String requestedAt;

    @JsonProperty("parcelId")
    private String parcelId;

    @JsonProperty("plcSeqNo")
    private String plcSeqNo;

    @JsonProperty("resultType")
    private String resultType;

    @JsonProperty("lineId")
    private String lineId;

    @JsonProperty("equipId")
    private String equipId;

    @JsonProperty("commandId")
    private String commandId;

    @JsonProperty("executedAt")
    private String executedAt;

    @JsonProperty("result")
    private String result;

    @JsonProperty("message")
    private String message;
}