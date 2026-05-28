package operato.logis.lms.dto.center;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
public class LmsAlarmRequestDto {
    // 센터 및 설비 식별
    @JsonProperty("lcId")
    private String lcId;
    @JsonProperty("equipId")
    private String equipId;
    @JsonProperty("lineId")
    private String lineId;
    @JsonProperty("orderId")
    private String orderId;

    // 알람 정보
    @JsonProperty("alarmId")
    private String alarmId;
    @JsonProperty("alarmType")
    private String alarmType;
    @JsonProperty("alarmMsg")
    private String alarmMsg;
    @JsonProperty("description")
    private String description;

    // 상태 및 시간
    @JsonProperty("isCleared")
    private Boolean isCleared;
    @JsonProperty("clearedBy")
    private String clearedBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    @JsonProperty("occurredAt")
    private Date occurredAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    @JsonProperty("clearedAt")
    private Date clearedAt;

    // 부가 정보
    @JsonProperty("sourceSystem")
    private String sourceSystem;
    @JsonProperty("durationSeconds")
    private Integer durationSeconds;
}