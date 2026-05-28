package operato.logis.connector.hokusho.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HokushoCommandTaskRequest {

    @JsonProperty("requestedAt")
    private String requestedAt;

    @JsonProperty("parcelId")
    private String parcelId;

    @JsonProperty("plcSeqNo")
    private String plcSeqNo;

    @JsonProperty("lineId")
    private String lineId;

    @JsonProperty("equipId")
    private String equipId;

    @JsonProperty("commandType")
    private String commandType;

    @JsonProperty("commandId")
    private String commandId;

    @JsonProperty("params")
    private Map<String, Object> params; // commandType별 동적 파라미터
}