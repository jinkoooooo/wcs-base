package operato.logis.connector.gtr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class InspectionRequestDto {
    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("serialNumbers")
    private List<String> serialNumbers;

    @JsonProperty("zoneId")
    private String zoneId;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("frontFileName")
    private String frontFileName;

    @JsonProperty("backFileName")
    private String backFileName;

    @JsonProperty("leftFileName")
    private String leftFileName;

    @JsonProperty("rightFileName")
    private String rightFileName;

    @JsonProperty("topFileName")
    private String topFileName;

    @JsonProperty("bottomLeftFileName")
    private String bottomLeftFileName;

    @JsonProperty("bottomRightFileName")
    private String bottomRightFileName;

}