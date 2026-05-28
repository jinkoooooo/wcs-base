package operato.logis.samsung.dto.xyz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class XyzDevanningResult {

    @JsonProperty("order_key")
    private String orderKey;

    @JsonProperty("result_time")
    private String resultTime;

    @JsonProperty("result_code")
    private Integer resultCode;

    @JsonProperty("result_msg")
    private String resultMsg;
}
