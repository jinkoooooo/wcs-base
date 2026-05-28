package operato.logis.lms.rest.dashboard;

import lombok.RequiredArgsConstructor;
import operato.logis.lms.entity.dashboard.StatusBoardDmg;
import operato.logis.lms.service.impl.dashboard.StatusBoardDmgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/status_board_dmg")
@ServiceDesc(description="StatusBoardDmg Service API")
public class StatusBoardDmgController extends AbstractRestService {

    private final StatusBoardDmgService statusBoardDmgService;

    @Override
    protected Class<?> entityClass() {
        return StatusBoardDmg.class;
    }

    private Logger logger = LoggerFactory.getLogger(StatusBoardDmgController.class);

    @RequestMapping(method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name="page", required=false) Integer page,
            @RequestParam(name="limit", required=false) Integer limit,
            @RequestParam(name="select", required=false) String select,
            @RequestParam(name="sort", required=false) String sort,
            @RequestParam(name="query", required=false) String query) {
        return this.search(this.entityClass(), page, limit, select, sort, query);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Find one by ID")
    public StatusBoardDmg findOne(@PathVariable("id") String id) {
        return this.getOne(this.entityClass(), id);
    }

    @RequestMapping(value="/{id}/exist", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check exists By ID")
    public Boolean isExist(@PathVariable("id") String id) {
        return this.isExistOne(this.entityClass(), id);
    }

    @RequestMapping(method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description="Create")
    public StatusBoardDmg create(@RequestBody StatusBoardDmg input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public StatusBoardDmg update(@PathVariable("id") String id, @RequestBody StatusBoardDmg input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<StatusBoardDmg> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }

    @RequestMapping(value="/select/{lcId}/{groupCode}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Find one by LC ID, Group Code")
    public StatusBoardDmg findOne(@PathVariable("lcId") String lcId, @PathVariable("groupCode") String groupCode) {
        return statusBoardDmgService.findOne(lcId, groupCode);
    }

    @RequestMapping(value="/select/{lcId}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Find List by LC ID")
    public List<StatusBoardDmg> findList(@PathVariable("lcId") String lcId) {
        return statusBoardDmgService.findList(lcId);
    }

    @RequestMapping(value="/update/attributes2D", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update StatusBoardDmt Attributes")
    public Boolean update2DAttributes(@RequestBody StatusBoardDmg attributes) {
        return statusBoardDmgService.update2DAttributes(attributes);
    }
}