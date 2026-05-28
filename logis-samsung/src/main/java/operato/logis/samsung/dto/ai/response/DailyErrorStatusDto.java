package operato.logis.samsung.dto.ai.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyErrorStatusDto {
    private LocalDate targetDate;
    private Integer totalErrorCount;
    private Integer abnormalUnitCount;
    private List<DailyErrorTopDto> topErrors;
    private List<UnresolvedErrorDto> unresolvedErrors;
}