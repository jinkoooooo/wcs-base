package operato.logis.asrs.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.entity.LocationGenerator;
import operato.logis.asrs.service.InventoryLocationService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/rest/location")
@RequiredArgsConstructor
public class LocationController extends AbstractRestService {

    private final InventoryLocationService inventoryLocationService;

    private static final Logger log = LoggerFactory.getLogger(WcsAsrsLayoutController.class);

    @Override
    protected Class<?> entityClass() {
        return LocationGenerator.class;
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
    public LocationGenerator findOne(@PathVariable("id") String id) {
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
    public LocationGenerator create(@RequestBody LocationGenerator input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public LocationGenerator update(@PathVariable("id") String id, @RequestBody LocationGenerator input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<LocationGenerator> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }

    @PostMapping("/generate")
    @ApiDesc(description="로케이션 마스터 일괄 자동 생성 (중복 방지 적용)")
    public ResponseEntity<?> generateLocations(@RequestBody List<LocationGenerator> locationList) {
        try {
            // 서비스 로직 실행
            inventoryLocationService.generateLocations(locationList);

            // 성공 응답
            Map<String, String> response = new HashMap<>();
            response.put("message", "성공적으로 생성되었습니다.");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // 중복 에러 등이 터지면 400 Bad Request와 함께 에러 메시지 프론트로 반환
            log.error("로케이션 생성 에러: {}", e.getMessage());
            Map<String, String> errorResp = new HashMap<>();
            errorResp.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResp);
        }
    }
}

