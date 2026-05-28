package operato.logis.samsung.dto.ai.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UnresolvedErrorDto {
    private String unitType;
    private String unitCode;
    private String errorCode;
    private String errorMsg;
    private LocalDateTime occurredAt;
}