package operato.logis.lms.rest.dashboard;

import lombok.RequiredArgsConstructor;
import operato.logis.lms.dto.dashboard.StatusBoardWcsInfo;
import operato.logis.lms.service.impl.dashboard.StatusBoardWcsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/status_board_wcs")
@ServiceDesc(description="StatusBoardWcs Service API")
public class StatusBoardWcsController {

    private final StatusBoardWcsService statusBoardWcsService;

    @RequestMapping(value="/getTaskAndStockInfo/{lc_id}/{task_id}/{stock_id}", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="LC_ID에 해당하는 Tap 목록 조회")
    public StatusBoardWcsInfo findListByLcId(@PathVariable("lc_id") String lcId,
                                             @PathVariable("task_id") String taskId,
                                             @PathVariable("stock_id") String stockId) {
        return statusBoardWcsService.getWcsInfo(lcId, taskId, stockId);
    }
}