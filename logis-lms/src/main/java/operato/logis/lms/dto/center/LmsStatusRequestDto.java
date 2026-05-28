package operato.logis.lms.dto.center;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class LmsStatusRequestDto {
    // 센터 및 설비 식별
    @JsonProperty("lcId")
    private String lcId;
    @JsonProperty("equipId")
    private String equipId;
    @JsonProperty("lineId")
    private String lineId;
    @JsonProperty("orderId")
    private String orderId;

    // 설비 상태
    @JsonProperty("currentStatus")
    private String currentStatus; // RUN, STOP, etc.
    @JsonProperty("preStatus")
    private String preStatus;

    // 에러 정보
    @JsonProperty("errCd")
    private String errCd;
    @JsonProperty("errMsg")// Entity 컬럼명과 매칭 (또는 errorCode)
    private String errMsg;

    // 센서 값
    private int sensorValue;
    private String sensorUnit;

    // 카운트
    private Integer operatingCnt;
    private Integer errCnt;

    // 시간 및 출처
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    @JsonProperty("statusUpdatedAt")
    private Date statusUpdatedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    @JsonProperty("dataUpdatedAt")
    private Date dataUpdatedAt;
    private String sourceSystem;
}