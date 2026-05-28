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
public class HokushoResponse {
    @JsonProperty("code")
    private String code;      // 결과 코드

    @JsonProperty("message")
    private String message;   // 설명

    @JsonProperty("commandId")
    private String commandId;     // 요청 ID (commandId)
}