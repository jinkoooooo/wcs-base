package operato.logis.lms.rest.dashboard;

import lombok.RequiredArgsConstructor;
import operato.logis.lms.dto.dashboard.ImageRequest;
import operato.logis.lms.entity.dashboard.StatusBoardDmt;
import operato.logis.lms.service.impl.dashboard.StatusBoardDmtService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/status_board_dmt")
@ServiceDesc(description="StatusBoardDmt Service API")
public class StatusBoardDmtController extends AbstractRestService {

    private final StatusBoardDmtService statusBoardDmtService;

    @Override
    protected Class<?> entityClass() {
        return StatusBoardDmt.class;
    }

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
    public StatusBoardDmt findOne(@PathVariable("id") String id) {
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
    public StatusBoardDmt create(@RequestBody StatusBoardDmt input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public StatusBoardDmt update(@PathVariable("id") String id, @RequestBody StatusBoardDmt input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<StatusBoardDmt> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }

    @RequestMapping(value="/upload/image", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="2D/3D 현황판 컴포넌트 이미지 파일 업로드")
    public ResponseEntity<String> uploadImage(@RequestBody ImageRequest request) {
        return statusBoardDmtService.uploadImage(request);
    }

    @RequestMapping(value="/download/image", method= RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="2D/3D 현황판 컴포넌트 이미지 파일 다운로드")
    public ResponseEntity<Resource> downloadImage(
            @RequestParam("lcId") String lcId,
            @RequestParam("modelType") String modelType,
            @RequestParam("dimension") String dimension) {
        return statusBoardDmtService.downloadImage(lcId, modelType, dimension);
    }
}