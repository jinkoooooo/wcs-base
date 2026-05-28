package operato.logis.asrs.rest;


import lombok.RequiredArgsConstructor;
import operato.logis.asrs.entity.WcsAsrsLayout;
import operato.logis.asrs.service.WcsAsrsLayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;


@RestController
@RequestMapping("/rest/layouts")
@RequiredArgsConstructor
public class WcsAsrsLayoutController extends AbstractRestService {

    private final WcsAsrsLayoutService wcsAsrsLayoutService;
    private static final Logger log = LoggerFactory.getLogger(WcsAsrsLayoutController.class);

    @Override
    protected Class<?> entityClass() {
        return WcsAsrsLayout.class;
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
    public WcsAsrsLayout findOne(@PathVariable("id") String id) {
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
    public WcsAsrsLayout create(@RequestBody WcsAsrsLayout input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public WcsAsrsLayout update(@PathVariable("id") String id, @RequestBody WcsAsrsLayout input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<WcsAsrsLayout> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }

    @PostMapping("/layout")
    public ResponseEntity<String> saveLayout(@RequestBody List<WcsAsrsLayout> layoutList) {
        if (layoutList == null || layoutList.isEmpty()) {
            return ResponseEntity.badRequest().body("저장할 데이터가 없습니다.");
        }

        // 프론트엔드에서 배열로 보낸 데이터 중 첫 번째 객체를 꺼냅니다.
        WcsAsrsLayout layoutEntity = layoutList.get(0);

        log.info("프론트엔드로부터 레이아웃 저장 요청 수신: Center={}, Zone={}",
                layoutEntity.getCenterId(), layoutEntity.getZoneId());

        // 기존에 만들어두신 Service 로직 그대로 사용!
        wcsAsrsLayoutService.saveLayout(layoutEntity);

        return ResponseEntity.ok("레이아웃이 성공적으로 저장되었습니다.");
    }
}

