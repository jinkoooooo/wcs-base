package operato.logis.samsung.rest.mw;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import operato.logis.samsung.entity.mw.TbMwItemMaster;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;
import java.util.Map;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tb_mw_item_master")
@ServiceDesc(description="ItemMaster Service API")
public class TbMwItemMasterController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return TbMwItemMaster.class;
    }

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
    public TbMwItemMaster findOne(@PathVariable("id") String id) {
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
    public TbMwItemMaster create(@RequestBody TbMwItemMaster input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public TbMwItemMaster update(@PathVariable("id") String id, @RequestBody TbMwItemMaster input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<TbMwItemMaster> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }

    @RequestMapping(value="/excel_upload", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Excel upload at one time")
    public Boolean excelUpload(@RequestBody List<TbMwItemMaster> list) {
        this.queryManager.insertBatch(list);
        return true;
    }
    @RequestMapping(value="/search", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search (Pagination) By POST")
    public Page<?> searchPost(@RequestBody Map<String, Object> params) {

        Integer page = params.get("page") != null ? Integer.valueOf(params.get("page").toString()) : null;
        Integer limit = params.get("limit") != null ? Integer.valueOf(params.get("limit").toString()) : null;
        String select = (String) params.get("select");
        String sort = (String) params.get("sort");

        String queryStr = null;
        if (params.get("query") != null) {
            Object queryObj = params.get("query");
            if (queryObj instanceof String) {
                queryStr = (String) queryObj;
            } else {
                queryStr = new Gson().toJson(queryObj);
            }
        }
        // 3. Elidom 내부 조회 로직 태우기
        return this.search(this.entityClass(), page, limit, select, sort, queryStr);
    }
}