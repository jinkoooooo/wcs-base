package operato.logis.samsung.dto.ai.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyErrorTopDto {
    private String errorCode;
    private Integer occurrenceCount;
}