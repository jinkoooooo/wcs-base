package operato.logis.samsung.dto.ai.internal;

import operato.logis.samsung.consts.ai.AiIntentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParsedCommand {
    private String originalQuestion;
    private String normalizedQuestion;
    private String lowerQuestion;

    private String userId;
    private Long domainId;
    private String contextLineId;
    private String contextEquipId;

    private LocalDate targetDate;
    private String skuCode;
    private String serialNo;
    private String errorCode;

    private AiIntentType intentType;
}