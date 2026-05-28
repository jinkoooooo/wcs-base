package operato.logis.lms.rest.hist;

import operato.logis.lms.dto.hist.SysQueueStatusDto;
import operato.logis.lms.service.impl.hist.UserLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/hist")
@ResponseStatus(HttpStatus.OK)
public class LmsAccessSysLogController {

    private UserLogService userLogService;

    public LmsAccessSysLogController(UserLogService userLogService) {
        this.userLogService = userLogService;
    }

    private final Logger logger = LoggerFactory.getLogger(LmsAccessSysLogController.class);

    @PostMapping(value = "/sys/flush-log")
    public String flushSysLog() {
        logger.info("flush access system log");
        userLogService.processUserActBatch();
        return "OK";
    }

    @GetMapping(value="/sys/monitoring")
    public SysQueueStatusDto getSysQueueStatus() {
        logger.info("get access system log");
        return userLogService.getQueueStatus();
    }
}