package operato.logis.lms.rest.dashboard;

import lombok.RequiredArgsConstructor;
import operato.logis.lms.dto.dashboard.StatusBoardUpdateRequest;
import operato.logis.lms.entity.dashboard.StatusBoardDmi;
import operato.logis.lms.service.impl.dashboard.StatusBoardDmiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/status_board_dmi")
@ServiceDesc(description="StatusBoardDmi Service API")
public class StatusBoardDmiController {

    private final StatusBoardDmiService statusBoardDmiService;

    @RequestMapping(value="/{lcId}/{pageId}", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="센터 코드, 페이지 ID에 해당하는 DMI 조회")
    public List<StatusBoardDmi> selectListOnPage(@PathVariable("lcId") String lcId, @PathVariable("pageId") String pageId) {
        return statusBoardDmiService.selectListOnPage(lcId, pageId);
    }

    @RequestMapping(value="/update_on_page", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="센터 코드, 페이지 ID에 해당하는 DMI 조회")
    public Boolean updateOnPage(@RequestBody StatusBoardUpdateRequest request) {
        return statusBoardDmiService.updateListOnPage(request);
    }
}