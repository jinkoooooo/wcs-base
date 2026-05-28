package operato.logis.lms.rest.dashboard;

import lombok.RequiredArgsConstructor;
import operato.logis.lms.entity.dashboard.StatusBoardPage;
import operato.logis.lms.service.impl.dashboard.StatusBoardDmiService;
import operato.logis.lms.service.impl.dashboard.StatusBoardPageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/status_board_page")
@ServiceDesc(description="StatusBoardPage Service API")
public class StatusBoardPageController {

    private final StatusBoardPageService statusBoardPageService;
    private final StatusBoardDmiService statusBoardDmiService;

    @RequestMapping(value="/{lc_id}", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="LC_ID에 해당하는 Tap 목록 조회")
    public List<StatusBoardPage> findListByLcId(@PathVariable("lc_id") String lcId) {
        return statusBoardPageService.findListByLcId(lcId);
    }

    @RequestMapping(value="/update_name", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Page 이름 변경")
    public Boolean updateName(@RequestBody StatusBoardPage page) {
        return statusBoardPageService.updateName(page);
    }

    @RequestMapping(value="/update_index", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Page 순서 변경")
    public Boolean updateIndex(@RequestBody List<StatusBoardPage> pageList) {
        return statusBoardPageService.updateIndex(pageList);
    }

    @RequestMapping(value="/update_value", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Page 크기, 색상 변경")
    public Boolean updateIndex(@RequestBody StatusBoardPage page) {
        return statusBoardPageService.updateValue(page);
    }

    @RequestMapping(value="/create", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Page 생성")
    public Boolean create(@RequestBody StatusBoardPage page) {
        return statusBoardPageService.createPage(page);
    }

    @RequestMapping(value="/delete", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Page 삭제")
    public Boolean delete(@RequestBody StatusBoardPage page) {
        boolean resultDmi = statusBoardDmiService.deleteByPageId(page.getId());
        boolean resultPage = statusBoardPageService.deletePage(page);
        return resultDmi && resultPage;
    }
}