package operato.logis.asrs.rest;

import java.util.List;

import operato.logis.asrs.query.location.LocationQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import operato.logis.asrs.entity.TbAcStorageArea;

import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.dbist.dml.Page;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tb_ac_storage_area")
@ServiceDesc(description="TbAcStorageArea Service API")
public class TbAcStorageAreaController extends AbstractRestService {

	@Override
	protected Class<?> entityClass() {
		return TbAcStorageArea.class;
	}
    private static final Logger log = LoggerFactory.getLogger(TbAcStorageAreaController.class);

    @Autowired
    private LocationQueryService locationQueryService;
  
	@RequestMapping(method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
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
	public TbAcStorageArea findOne(@PathVariable("id") String id) {
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
	public TbAcStorageArea create(@RequestBody TbAcStorageArea input) {
		return this.createOne(input);
	}

	@RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update")
	public TbAcStorageArea update(@PathVariable("id") String id, @RequestBody TbAcStorageArea input) {
		return this.updateOne(input);
	}
  
	@RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}  
  
	@RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple at one time")
	public Boolean multipleUpdate(@RequestBody List<TbAcStorageArea> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}


    @PostMapping("/save-area") // POST로 명시하는 것이 좋습니다.
    public ResponseEntity<String> saveArea(@RequestBody TbAcStorageArea areaEntity) {
        if (areaEntity == null) {
            return ResponseEntity.badRequest().body("저장할 데이터가 없습니다.");
        }
        log.info("컨트롤러 진입 - centerId: {}", areaEntity.getCenterId());
        locationQueryService.saveStorageArea(areaEntity);
        return ResponseEntity.ok("성공");
    }

	@RequestMapping(
			value = "/active-list",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiDesc(description = "Find active storage area list for combo")
	@Transactional(readOnly = true)
	public List<TbAcStorageArea> findActiveAreaList() {
		return locationQueryService.findActiveAreaList();
	}
	  
}