package operato.logis.samsung.dto.ai.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGuideStepDto {
    private Integer stepNo;
    private String title;
    private String description;
}