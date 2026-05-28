package operato.logis.samsung.dto.ai.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AiChatRequest {
    private String question;
    private String userId;
    private Long domainId;
    private String contextLineId;
    private String contextEquipId;
    private LocalDate targetDate;
}