package operato.logis.changwon.dto.lms;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class AlarmRequestDto {
    private String lcId;        // 센터 ID
    private String equipId;     // 설비 ID
    private String lineId;
    private String orderId;

    private String alarmId;     // UUID
    private String alarmType;
    private String alarmMsg;
    private String description;

    private Boolean isCleared;
    private String clearedBy;
    private Date occurredAt;
    private Date clearedAt;

    private String sourceSystem;
    private Integer durationSeconds;
}
