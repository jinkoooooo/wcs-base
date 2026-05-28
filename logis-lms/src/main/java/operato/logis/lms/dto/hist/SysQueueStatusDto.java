package operato.logis.lms.dto.hist;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysQueueStatusDto {

    // 로그 저장 대기 큐 관련
    private int queueSize;
    private int maxQueueSize;
    // 로그 실패 대기 큐 관련
    private int retryQueueSize;
    private int maxRetryQueueSize;
    private int maxRetryCount;
    // 공통
    private int batchSize;
}