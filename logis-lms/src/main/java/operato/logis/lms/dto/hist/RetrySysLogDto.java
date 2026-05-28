package operato.logis.lms.dto.hist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import operato.logis.lms.entity.hist.AccessSysLog;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class RetrySysLogDto {

    private AccessSysLog log;
    private int retryCount;
    private LocalDateTime nextRetryTime;
}